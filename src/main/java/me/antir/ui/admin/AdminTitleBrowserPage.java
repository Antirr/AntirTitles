package me.antir.ui.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.TitlesModule;
import me.antir.api.TitleAPI;
import me.antir.data.Title;
import me.antir.ui.ColorUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin-only UI page that lists all titles (asset store + runtime),
 * allows self-granting any title, and opens {@link AdminTitleEditorPage} to create new ones.
 * Opened via {@code /title admin} — requires {@code antirtitles.admin} permission.
 */
public class AdminTitleBrowserPage extends InteractiveCustomUIPage<AdminTitleBrowserPage.AdminBrowserEvent>
{
    private static final String BROWSER_UI = "Titles/Admin/AdminTitleBrowser.ui";
    private static final String ENTRY_UI   = "Titles/Admin/AdminTitleEntry.ui";

    public AdminTitleBrowserPage(@Nonnull PlayerRef playerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminBrowserEvent.CODEC);
    }

    // ── Page build ────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store)
    {
        builder.append(BROWSER_UI);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateTitleButton",
                EventData.of("Action", "openEditor"));

        List<Title> allTitles = collectAllTitles();
        int count = allTitles.size();
        int half  = (count + 1) / 2;

        for (int i = 0; i < count; i++)
        {
            String column   = i < half ? "#CardColumnLeft" : "#CardColumnRight";
            int    colIndex = i < half ? i : i - half;

            builder.append(column, ENTRY_UI);
            populateEntry(builder, eventBuilder, column + "[" + colIndex + "]", allTitles.get(i));
        }
    }

    private void populateEntry(@Nonnull UICommandBuilder builder, @Nonnull UIEventBuilder eventBuilder, @Nonnull String entrySelector, @Nonnull Title title)
    {
        builder.set(entrySelector + " #EntryName.Text", title.getDisplayName());
        builder.set(entrySelector + " #EntryId.Text", title.getId());

        ItemQuality quality = title.getRarity();
        String rarityLabel = quality != null ? quality.getId() : "";
        builder.set(entrySelector + " #EntryRarity.Text", rarityLabel);

        boolean isRuntime = TitleAPI.isRuntimeTitle(title.getId());
        builder.set(entrySelector + " #EntryRuntime.Visible", isRuntime);
        builder.set(entrySelector + " #EntryRemove.Visible", isRuntime);

        String accentHex = title.getUiAccentColor().isEmpty()
                ? ColorUtil.accentHex(quality)
                : title.getUiAccentColor();
        builder.appendInline(entrySelector + " #EntryAccent", ColorUtil.colorFillDoc(accentHex));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                entrySelector + " #EntryLearn",
                EventData.of("Action", "grant").append("TitleId", title.getId()));

        if (isRuntime)
        {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    entrySelector + " #EntryRemove",
                    EventData.of("Action", "delete").append("TitleId", title.getId()));
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull AdminBrowserEvent event)
    {
        switch (event.action)
        {
            case "openEditor" ->
            {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) return;

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                player.getPageManager().openCustomPage(ref, store, new AdminTitleEditorPage(playerRef));
            }
            case "grant" ->
            {
                Title title = TitleAPI.getTitleAsset(event.titleId);
                if (title != null)
                {
                    TitleAPI.grantTitle(ref, store, title);
                    rebuild();
                }
            }
            case "delete" ->
            {
                if (TitleAPI.isRuntimeTitle(event.titleId))
                {
                    TitleAPI.unregisterRuntimeTitle(event.titleId);
                    TitlesModule.get().unpersistRuntimeTitle(event.titleId);
                    rebuild();
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nonnull
    private static List<Title> collectAllTitles()
    {
        List<Title> allTitles = new ArrayList<>(Title.getAssetMap().getAssetMap().values());
        allTitles.addAll(TitleAPI.getRuntimeTitles());
        return allTitles;
    }

    // ── Event data ────────────────────────────────────────────────────────────

    public static class AdminBrowserEvent
    {
        public String action  = "";
        public String titleId = "";

        public static final BuilderCodec<AdminBrowserEvent> CODEC =
                BuilderCodec.builder(AdminBrowserEvent.class, AdminBrowserEvent::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (browserEvent, value) -> browserEvent.action  = value != null ? value : "",
                                browserEvent -> browserEvent.action).add()
                        .append(new KeyedCodec<>("TitleId", Codec.STRING),
                                (browserEvent, value) -> browserEvent.titleId = value != null ? value : "",
                                browserEvent -> browserEvent.titleId).add()
                        .build();
    }
}
