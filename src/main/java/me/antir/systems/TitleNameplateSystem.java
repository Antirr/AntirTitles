package me.antir.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.TitlesModule;
import me.antir.components.TitleComponent;
import me.antir.config.TitleDisplayConfig;
import me.antir.data.Title;
import me.antir.events.TitleEquipEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Keeps the player nameplate and the chat-prefix cache in sync whenever
 * a title is equipped or unequipped.
 *
 * <p>Runs after all equip/unequip and synergy handlers so the
 * {@link TitleComponent} already reflects the final equipped state.
 *
 * <p><b>NameplateBuilder:</b> This implementation uses Hytale's native
 * {@link Nameplate} component (plain text). If the NameplateBuilder library
 * is available, replace {@link #applyNameplate} with its rich-text API and
 * keep the rest unchanged.
 */
public class TitleNameplateSystem extends EntityEventSystem<EntityStore, TitleEquipEvent>
{
    public TitleNameplateSystem()
    {
        super(TitleEquipEvent.class);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery()
    {
        return TitleComponent.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull TitleEquipEvent event)
    {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        TitleComponent comp = chunk.getComponent(index, TitleComponent.getComponentType());
        if (comp == null) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        String titleName = resolveFirstEquippedTitle(comp);
        String username = playerRef.getUsername();

        TitlesModule.get().updatePlayerTitle(username, titleName);

        TitleDisplayConfig cfg = TitlesModule.get().getDisplayConfig();
        if (cfg.isShowInNameplate())
        {
            applyNameplate(store, ref, cfg, titleName, username);
        }
    }

    /**
     * Writes the formatted nameplate text. Override or replace this method
     * to use a rich-text nameplate library instead of the native API.
     */
    public static void applyNameplate(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                       @Nonnull TitleDisplayConfig cfg,
                                       @Nullable String titleName, @Nonnull String username)
    {
        Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
        if (nameplate == null) return;

        if (titleName != null && !titleName.isEmpty())
        {
            nameplate.setText(cfg.formatNameplate(titleName, username));
        }
        else
        {
            nameplate.setText(username);
        }
    }

    /** Returns the display name of the first equipped title, or {@code null} if none are equipped. */
    @Nullable
    public static String resolveFirstEquippedTitle(@Nonnull TitleComponent comp)
    {
        List<String> equipped = comp.getEquippedTitleIds();
        if (equipped.isEmpty()) return null;
        Title title = Title.getAssetMap().getAsset(equipped.get(0));
        return title != null ? title.getDisplayName() : null;
    }
}
