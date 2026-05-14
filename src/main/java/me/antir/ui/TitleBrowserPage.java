package me.antir.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.api.TitleAPI;
import me.antir.components.TitleComponent;
import me.antir.data.Title;
import me.antir.data.effect.ITitleEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TitleBrowserPage extends InteractiveCustomUIPage<TitleBrowserPage.BrowserEvent>
{
    private static final String BROWSER_UI = "Titles/TitleBrowser.ui";
    private static final String CARD_UI    = "Titles/TitleCard.ui";

    @Nullable
    private String selectedTitleId = null;

    public TitleBrowserPage(@Nonnull PlayerRef playerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, BrowserEvent.CODEC);
    }

    // ── Page build ────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store)
    {
        builder.append(BROWSER_UI);

        TitleComponent titleComp = store.getComponent(ref, TitleComponent.getComponentType());
        List<String> learnedIds  = titleComp != null ? titleComp.getTitleIds() : List.of();
        List<String> equippedIds = titleComp != null ? titleComp.getEquippedTitleIds() : List.of();
        int maxSlots             = titleComp != null ? titleComp.getMaxEquippedTitles() : 1;

        builder.set("#BrowserTitle.Text", "Your Titles");
        builder.set("#BrowserSubtitle.Text",
                learnedIds.size() + " learned  •  " + equippedIds.size() + "/" + maxSlots + " equipped");

        int cardIndex = 0;
        for (String titleId : learnedIds)
        {
            Title title = TitleAPI.getTitleAsset(titleId);
            if (title == null) continue;

            builder.append("#TitleCardList", CARD_UI);
            String cardSelector = "#TitleCardList[" + cardIndex + "]";

            builder.set(cardSelector + " #TitleCardName.Text", title.getDisplayName());

            ItemQuality quality = title.getRarity();
            if (quality != null)
            {
                builder.set(cardSelector + " #TitleCardRarity.TextSpans",
                        Message.raw(quality.getId()).color(ColorUtil.accentAwtColor(quality)));
            }
            else
            {
                builder.set(cardSelector + " #TitleCardRarity.Text", "");
            }

            builder.set(cardSelector + " #TitleCardEquipped.Text", equippedIds.contains(titleId) ? "Equipped" : "");

            // Fill the accent bar with the rarity (or per-title override) color
            String accentHex = title.getUiAccentColor().isEmpty()
                    ? ColorUtil.accentHex(quality)
                    : title.getUiAccentColor();
            builder.appendInline(cardSelector + " #TitleCardAccent", ColorUtil.colorFillDoc(accentHex));

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, cardSelector,
                    EventData.of("Action", "select").append("TitleId", titleId));

            cardIndex++;
        }

        if (selectedTitleId != null)
        {
            populateDetail(builder, eventBuilder, selectedTitleId, equippedIds);
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull BrowserEvent browserEvent)
    {
        switch (browserEvent.action)
        {
            case "select" ->
            {
                this.selectedTitleId = browserEvent.titleId;
                TitleComponent titleComp  = store.getComponent(ref, TitleComponent.getComponentType());
                List<String> equippedIds  = titleComp != null ? titleComp.getEquippedTitleIds() : List.of();

                UICommandBuilder updateBuilder = new UICommandBuilder();
                UIEventBuilder updateEventBuilder = new UIEventBuilder();
                populateDetail(updateBuilder, updateEventBuilder, browserEvent.titleId, equippedIds);
                sendUpdate(updateBuilder, updateEventBuilder, false);
            }
            case "equip" ->
            {
                this.selectedTitleId = browserEvent.titleId;
                Title title = TitleAPI.getTitleAsset(browserEvent.titleId);
                if (title != null) TitleAPI.equipTitle(ref, store, title);
                rebuild();
            }
            case "unequip" ->
            {
                this.selectedTitleId = browserEvent.titleId;
                Title title = TitleAPI.getTitleAsset(browserEvent.titleId);
                if (title != null) TitleAPI.unequipTitle(ref, store, title);
                rebuild();
            }
        }
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void populateDetail(@Nonnull UICommandBuilder builder, @Nonnull UIEventBuilder eventBuilder,
                                @Nonnull String titleId, @Nonnull List<String> equippedIds)
    {
        Title title = TitleAPI.getTitleAsset(titleId);
        if (title == null) return;

        builder.set("#DetailEmpty.Text", "");
        builder.set("#DetailName.Text", title.getDisplayName());

        ItemQuality quality = title.getRarity();
        if (quality != null)
        {
            builder.set("#DetailRarity.TextSpans",
                    Message.raw(quality.getId().toUpperCase()).color(ColorUtil.accentAwtColor(quality)));

            String accentHex = title.getUiAccentColor().isEmpty()
                    ? ColorUtil.accentHex(quality)
                    : title.getUiAccentColor();
            builder.clear("#DetailRarityBadge");
            builder.appendInline("#DetailRarityBadge", ColorUtil.colorFillDoc(accentHex));
        }
        else
        {
            builder.set("#DetailRarity.Text", "");
        }

        builder.set("#DetailDescription.Text", title.getDescription());

        builder.set("#DetailPassiveHeader.Text", "PASSIVE");
        builder.clear("#DetailPassiveEffects");
        appendEffectRows(builder, "#DetailPassiveEffects", title.getPassiveEffects());

        builder.set("#DetailActiveHeader.Text", "ACTIVE");
        builder.clear("#DetailActiveEffects");
        appendEffectRows(builder, "#DetailActiveEffects", title.getActiveEffects());

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EquipButton",
                EventData.of("Action", "equip").append("TitleId", titleId));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#UnequipButton",
                EventData.of("Action", "unequip").append("TitleId", titleId));
    }

    private static void appendEffectRows(@Nonnull UICommandBuilder builder,
                                         @Nonnull String containerSelector,
                                         @Nonnull ITitleEffect[] effects)
    {
        if (effects.length == 0)
        {
            builder.appendInline(containerSelector,
                    "Label { Text: \"None\"; Style: (FontSize: 11, TextColor: #4a6070); Anchor: (Bottom: 2); }");
            return;
        }
        for (ITitleEffect effect : effects)
        {
            String desc  = effect.getDescription().replace("\\", "\\\\").replace("\"", "\\\"");
            String color = effect.getUiColor();
            builder.appendInline(containerSelector,
                    "Label { Text: \"" + desc + "\"; Style: (FontSize: 11, TextColor: " + color + "); Anchor: (Bottom: 3); }");
        }
    }

    // ── Event data ────────────────────────────────────────────────────────────

    public static class BrowserEvent
    {
        public String action  = "";
        public String titleId = "";

        public static final BuilderCodec<BrowserEvent> CODEC =
                BuilderCodec.builder(BrowserEvent.class, BrowserEvent::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (browserEvent, value) -> browserEvent.action  = value != null ? value : "",
                                browserEvent -> browserEvent.action).add()
                        .append(new KeyedCodec<>("TitleId", Codec.STRING),
                                (browserEvent, value) -> browserEvent.titleId = value != null ? value : "",
                                browserEvent -> browserEvent.titleId).add()
                        .build();
    }
}
