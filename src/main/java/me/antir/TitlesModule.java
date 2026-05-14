package me.antir;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.event.events.ecs.BreathingCheckEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import me.antir.api.TitleAPI;
import me.antir.commands.TitleCommands;
import me.antir.components.RequirementProgressComponent;
import me.antir.components.TitleComponent;
import me.antir.config.TitleDisplayConfig;
import me.antir.data.RuntimeTitleConfig;
import me.antir.data.RuntimeTitleData;
import me.antir.data.Title;
import me.antir.data.TitleSynergy;
import me.antir.data.effect.EntityEffectEffect;
import me.antir.data.effect.GrantItemEffect;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.effect.StatModifierEffect;
import me.antir.data.requirement.ActivityCountRequirement;
import me.antir.data.requirement.ITitleRequirement;
import me.antir.data.requirement.KillCountRequirement;
import me.antir.data.requirement.ObtainItemRequirement;
import me.antir.data.requirement.TierProgressRequirement;
import me.antir.interactions.GrantTitleInteraction;
import me.antir.systems.TitleEquippedHandler;
import me.antir.systems.TitleGrantedHandler;
import me.antir.systems.TitleKillCheck;
import me.antir.systems.TitleNameplateSystem;
import me.antir.systems.TitleRemovedHandler;
import me.antir.systems.TitleSynergyHandler;
import me.antir.systems.TitleUnequippedHandler;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class TitlesModule extends JavaPlugin
{
    private static TitlesModule instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ComponentType<EntityStore, TitleComponent> titleComponentType;
    private ComponentType<EntityStore, RequirementProgressComponent> requirementProgressComponentType;

    /** Registered in constructor so preLoad() can load it before setup() runs. */
    private final Config<TitleDisplayConfig> displayConfig;

    /**
     * Persisted store of admin-created runtime titles.
     * Saved to {@code admin-titles.json} in the plugin data dir (world save).
     */
    private final Config<RuntimeTitleConfig> runtimeTitleConfig;

    /**
     * Maps player username → equipped title display name.
     * Written from ECS event threads; read from async chat event thread.
     */
    private final ConcurrentHashMap<String, String> playerTitleCache = new ConcurrentHashMap<>();

    public static TitlesModule get()
    {
        return instance;
    }

    public TitlesModule(@NonNullDecl JavaPluginInit init)
    {
        super(init);
        instance = this;
        // withConfig() must be called before setup() — preLoad() loads it automatically
        this.displayConfig       = withConfig("title-display",  TitleDisplayConfig.CODEC);
        this.runtimeTitleConfig  = withConfig("admin-titles",   RuntimeTitleConfig.CODEC);
    }

    @Override
    protected void setup()
    {
        LOGGER.atInfo().log("Antir's Titles loaded");

        // --- Asset stores ---
        this.getAssetRegistry().register(
                HytaleAssetStore.builder(Title.class, new DefaultAssetMap<String, Title>())
                        .setPath("Titles")
                        .setCodec(Title.CODEC)
                        .setKeyFunction(Title::getId)
                        .setExtension(".json")
                        .build()
        );

        this.getAssetRegistry().register(
                HytaleAssetStore.builder(TitleSynergy.class, new DefaultAssetMap<String, TitleSynergy>())
                        .setPath("TitleSynergies")
                        .setCodec(TitleSynergy.CODEC)
                        .setKeyFunction(TitleSynergy::getId)
                        .setExtension(".json")
                        .build()
        );

        // --- Effect types ---
        ITitleEffect.CODEC
                .register("StatModifier",  StatModifierEffect.class,  StatModifierEffect.CODEC)
                .register("EntityEffect",  EntityEffectEffect.class,  EntityEffectEffect.CODEC)
                .register("GrantItem",     GrantItemEffect.class,     GrantItemEffect.CODEC);

        // --- Requirement types ---
        ITitleRequirement.CODEC
                .register("KillCount",     KillCountRequirement.class,    KillCountRequirement.CODEC)
                .register("TierProgress",  TierProgressRequirement.class,  TierProgressRequirement.CODEC)
                .register("ObtainItem",    ObtainItemRequirement.class,    ObtainItemRequirement.CODEC)
                .register("ActivityCount", ActivityCountRequirement.class, ActivityCountRequirement.CODEC);

        // --- Entity components ---
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        this.titleComponentType = entityStoreRegistry.registerComponent(
                TitleComponent.class, "TitleComponent", TitleComponent.CODEC);
        this.requirementProgressComponentType = entityStoreRegistry.registerComponent(
                RequirementProgressComponent.class, "RequirementProgressComponent", RequirementProgressComponent.CODEC);

        // --- Event-driven systems ---
        entityStoreRegistry.registerSystem(new TitleGrantedHandler());
        entityStoreRegistry.registerSystem(new TitleRemovedHandler());
        entityStoreRegistry.registerSystem(new TitleEquippedHandler());
        entityStoreRegistry.registerSystem(new TitleUnequippedHandler());
        entityStoreRegistry.registerSystem(new TitleSynergyHandler());
        entityStoreRegistry.registerSystem(new TitleNameplateSystem());
        entityStoreRegistry.registerSystem(new TitleKillCheck());

        // --- Load persisted runtime titles into the live runtime map ---
        for (RuntimeTitleData savedTitle : runtimeTitleConfig.get().titles)
        {
            TitleAPI.registerRuntimeTitle(savedTitle.toTitle());
        }
        LOGGER.atInfo().log("Loaded " + runtimeTitleConfig.get().titles.size() + " admin-created runtime title(s)");

        // --- Player lifecycle ---
        EventRegistry eventRegistry = this.getEventRegistry();

        // PlayerReadyEvent is IEvent<String> (keyed by username) → registerGlobal for all players
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            Ref<EntityStore> ref = event.getPlayerRef();
            Store<EntityStore> store = ref.getStore();

            TitleComponent titles = store.ensureAndGetComponent(ref, TitleComponent.getComponentType());
            store.ensureAndGetComponent(ref, RequirementProgressComponent.getComponentType());

            // Apply the server-wide default slot count if the component is still at the
            // hardcoded default (1). Explicit per-player overrides set above 1 are preserved.
            int configDefault = displayConfig.get().getDefaultMaxEquippedTitles();
            if (titles.getMaxEquippedTitles() == 1 && configDefault > 1)
            {
                titles.setMaxEquippedTitles(configDefault);
            }

            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            String titleName = TitleNameplateSystem.resolveFirstEquippedTitle(titles);
            String username = playerRef.getUsername();
            updatePlayerTitle(username, titleName);

            TitleDisplayConfig displayCfg = displayConfig.get();
            if (displayCfg.isShowInNameplate())
            {
                TitleNameplateSystem.applyNameplate(store, ref, displayCfg, titleName, username);
            }
        });

        // PlayerDisconnectEvent is IEvent<Void> → simple register
        eventRegistry.register(PlayerDisconnectEvent.class, event -> {
            updatePlayerTitle(event.getPlayerRef().getUsername(), null);
        });

        // --- Chat title prefix (registerGlobal = fire for all senders) ---
        eventRegistry.registerGlobal(PlayerChatEvent.class, event -> {
            if (!displayConfig.get().isShowInChat()) return;
            String titleName = playerTitleCache.get(event.getSender().getUsername());
            if (titleName == null || titleName.isEmpty()) return;
            String prefix = displayConfig.get().formatChatPrefix(titleName);
            event.setFormatter((chatSender, content) ->
                    Message.translation("server.chat.playerMessage")
                            .param("username", prefix + chatSender.getUsername())
                            .param("message", content));
        });

        // --- Commands ---
        CommandRegistry commandRegistry = this.getCommandRegistry();
        commandRegistry.registerCommand(new TitleCommands());

        // --- Interactions ---
        var interactionRegistry = this.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("GrantTitle", GrantTitleInteraction.class, GrantTitleInteraction.CODEC);
    }

    // ── Public accessors ──────────────────────────────────────────────────────────

    public ComponentType<EntityStore, TitleComponent> getTitleComponentType()
    {
        return this.titleComponentType;
    }

    public ComponentType<EntityStore, RequirementProgressComponent> getRequirementProgressComponentType()
    {
        return this.requirementProgressComponentType;
    }

    public TitleDisplayConfig getDisplayConfig()
    {
        return displayConfig.get();
    }

    /**
     * Persists a newly-created runtime title to the world save so it survives restarts.
     * Call after {@link TitleAPI#registerRuntimeTitle} to write through to disk.
     */
    public void persistRuntimeTitle(@Nonnull Title title)
    {
        RuntimeTitleData runtimeTitleData = RuntimeTitleData.from(title);
        RuntimeTitleConfig config = runtimeTitleConfig.get();

        config.titles.removeIf(existing -> existing.id.equals(title.getId()));
        config.titles.add(runtimeTitleData);

        runtimeTitleConfig.save();
    }

    /**
     * Removes a runtime title from disk persistence.
     * Call after {@link TitleAPI#unregisterRuntimeTitle} to write through to disk.
     */
    public void unpersistRuntimeTitle(@Nonnull String titleId)
    {
        RuntimeTitleConfig config = runtimeTitleConfig.get();
        config.titles.removeIf(existing -> existing.id.equals(titleId));
        runtimeTitleConfig.save();
    }

    public void updatePlayerTitle(String username, @Nullable String titleName)
    {
        if (titleName != null && !titleName.isEmpty())
        {
            playerTitleCache.put(username, titleName);
        }
        else
        {
            playerTitleCache.remove(username);
        }
    }
}
