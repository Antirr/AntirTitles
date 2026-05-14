package me.antir.ui.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.data.effect.EntityEffectEffect;
import me.antir.data.effect.GrantItemEffect;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.effect.StatModifierEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Admin sub-page for creating a single {@link ITitleEffect}.
 * On submit, passes the created effect to the parent {@link AdminTitleEditorPage} and returns to it.
 */
public class AdminEffectEditorPage extends InteractiveCustomUIPage<AdminEffectEditorPage.EffectEditorEvent>
{
    private static final String EFFECT_EDITOR_UI = "Titles/Admin/AdminEffectEditor.ui";

    @Nonnull private final AdminTitleEditorPage editorPage;
    @Nonnull private final PlayerRef            playerRef;
    @Nonnull private final String               effectTarget;   // "passive" or "active"
    @Nonnull private       String               selectedType = "StatModifier";

    public AdminEffectEditorPage(@Nonnull AdminTitleEditorPage editorPage,
                                 @Nonnull PlayerRef playerRef,
                                 @Nonnull String effectTarget)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, EffectEditorEvent.CODEC);
        this.editorPage   = editorPage;
        this.playerRef    = playerRef;
        this.effectTarget = effectTarget;
    }

    // ── Page build ────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store)
    {
        builder.append(EFFECT_EDITOR_UI);

        // Type dropdown
        builder.set("#EffectTypeDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Stat Modifier"), "StatModifier"),
                new DropdownEntryInfo(LocalizableString.fromString("Grant Item"),    "GrantItem"),
                new DropdownEntryInfo(LocalizableString.fromString("Entity Effect"), "EntityEffect")
        ));
        builder.set("#EffectTypeDropdown.Value", selectedType);

        // Section visibility
        builder.set("#StatModSection.Visible",     "StatModifier".equals(selectedType));
        builder.set("#GrantItemSection.Visible",   "GrantItem".equals(selectedType));
        builder.set("#EntityEffectSection.Visible","EntityEffect".equals(selectedType));

        // Populate StatModifier sub-dropdowns when visible
        if ("StatModifier".equals(selectedType))
        {
            builder.set("#CalculationTypeInput.Entries", List.of(
                    new DropdownEntryInfo(LocalizableString.fromString("Additive"),       "ADDITIVE"),
                    new DropdownEntryInfo(LocalizableString.fromString("Multiplicative"), "MULTIPLICATIVE")
            ));
            builder.set("#CalculationTypeInput.Value", "ADDITIVE");

            builder.set("#TargetInput.Entries", List.of(
                    new DropdownEntryInfo(LocalizableString.fromString("Max"), "MAX"),
                    new DropdownEntryInfo(LocalizableString.fromString("Min"), "MIN")
            ));
            builder.set("#TargetInput.Value", "MAX");

            builder.set("#PostApplyInput.Entries", List.of(
                    new DropdownEntryInfo(LocalizableString.fromString("None"),        "NONE"),
                    new DropdownEntryInfo(LocalizableString.fromString("Fill to Max"), "FILL"),
                    new DropdownEntryInfo(LocalizableString.fromString("Smart Fill"),  "SMART_FILL")
            ));
            builder.set("#PostApplyInput.Value", "NONE");
        }

        // ADD button with type-specific field captures
        EventData addData = new EventData().append("Action", "submit");
        if ("StatModifier".equals(selectedType))
        {
            addData.append("@Stat",             "#StatInput.Value")
                   .append("@CalculationType",  "#CalculationTypeInput.Value")
                   .append("@Amount",           "#AmountInput.Value")
                   .append("@Target",           "#TargetInput.Value")
                   .append("@PostApply",        "#PostApplyInput.Value");
        }
        else if ("GrantItem".equals(selectedType))
        {
            addData.append("@ItemId",     "#ItemIdInput.Value")
                   .append("@ItemAmount", "#ItemAmountInput.Value");
        }
        else
        {
            addData.append("@EffectId", "#EffectIdInput.Value");
        }
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddEffectBtn", addData);

        // ValueChanged on type dropdown — sends new type to server for rebuild
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EffectTypeDropdown",
                new EventData().append("Action", "typeChanged")
                               .append("@EffectType", "#EffectTypeDropdown.Value"),
                false);

        // Cancel
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelEffectBtn",
                EventData.of("Action", "cancel"));
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull EffectEditorEvent event)
    {
        switch (event.action)
        {
            case "typeChanged" ->
            {
                if (!event.effectType.isEmpty()) this.selectedType = event.effectType;
                rebuild();
            }
            case "submit" -> handleSubmit(ref, store, event);
            case "cancel" -> returnToEditor(ref, store);
        }
    }

    private void handleSubmit(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                              @Nonnull EffectEditorEvent event)
    {
        ITitleEffect effect = switch (selectedType)
        {
            case "StatModifier"  -> buildStatModifier(event);
            case "GrantItem"     -> buildGrantItem(event);
            case "EntityEffect"  -> buildEntityEffect(event);
            default              -> null;
        };

        if (effect != null) editorPage.addPendingEffect(effect, effectTarget);
        returnToEditor(ref, store);
    }

    @Nullable
    private static ITitleEffect buildStatModifier(@Nonnull EffectEditorEvent event)
    {
        String stat = event.stat.trim();
        if (stat.isEmpty()) return null;

        float amount;
        try { amount = Float.parseFloat(event.amount.trim()); }
        catch (NumberFormatException ignored) { amount = 0f; }

        StaticModifier.CalculationType calcType =
                "MULTIPLICATIVE".equals(event.calculationType)
                        ? StaticModifier.CalculationType.MULTIPLICATIVE
                        : StaticModifier.CalculationType.ADDITIVE;

        Modifier.ModifierTarget target =
                "MIN".equalsIgnoreCase(event.modifierTarget)
                        ? Modifier.ModifierTarget.MIN
                        : Modifier.ModifierTarget.MAX;

        StatModifierEffect.PostApplyBehavior postApply = switch (event.postApply)
        {
            case "FILL"       -> StatModifierEffect.PostApplyBehavior.FILL;
            case "SMART_FILL" -> StatModifierEffect.PostApplyBehavior.SMART_FILL;
            default           -> StatModifierEffect.PostApplyBehavior.NONE;
        };

        return StatModifierEffect.create(stat, calcType, amount, target, postApply);
    }

    @Nullable
    private static ITitleEffect buildGrantItem(@Nonnull EffectEditorEvent event)
    {
        String itemId = event.itemId.trim();
        if (itemId.isEmpty()) return null;

        int amount;
        try { amount = Integer.parseInt(event.itemAmount.trim()); }
        catch (NumberFormatException ignored) { amount = 1; }

        return GrantItemEffect.create(itemId, Math.max(1, amount));
    }

    @Nullable
    private static ITitleEffect buildEntityEffect(@Nonnull EffectEditorEvent event)
    {
        String effectId = event.effectId.trim();
        if (effectId.isEmpty()) return null;
        return EntityEffectEffect.create(effectId);
    }

    private void returnToEditor(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store)
    {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    // ── Event data ────────────────────────────────────────────────────────────

    public static class EffectEditorEvent
    {
        public String action          = "";
        // type-changed event
        public String effectType      = "";
        // StatModifier fields
        public String stat            = "";
        public String calculationType = "";
        public String amount          = "";
        public String modifierTarget  = "";
        public String postApply       = "";
        // GrantItem fields
        public String itemId          = "";
        public String itemAmount      = "";
        // EntityEffect fields
        public String effectId        = "";

        public static final BuilderCodec<EffectEditorEvent> CODEC =
                BuilderCodec.builder(EffectEditorEvent.class, EffectEditorEvent::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (event, value) -> event.action = value != null ? value : "",
                                event -> event.action).add()
                        .append(new KeyedCodec<>("@EffectType", Codec.STRING),
                                (event, value) -> event.effectType = value != null ? value : "",
                                event -> event.effectType).add()
                        .append(new KeyedCodec<>("@Stat", Codec.STRING),
                                (event, value) -> event.stat = value != null ? value : "",
                                event -> event.stat).add()
                        .append(new KeyedCodec<>("@CalculationType", Codec.STRING),
                                (event, value) -> event.calculationType = value != null ? value : "",
                                event -> event.calculationType).add()
                        .append(new KeyedCodec<>("@Amount", Codec.STRING),
                                (event, value) -> event.amount = value != null ? value : "",
                                event -> event.amount).add()
                        .append(new KeyedCodec<>("@Target", Codec.STRING),
                                (event, value) -> event.modifierTarget = value != null ? value : "",
                                event -> event.modifierTarget).add()
                        .append(new KeyedCodec<>("@PostApply", Codec.STRING),
                                (event, value) -> event.postApply = value != null ? value : "",
                                event -> event.postApply).add()
                        .append(new KeyedCodec<>("@ItemId", Codec.STRING),
                                (event, value) -> event.itemId = value != null ? value : "",
                                event -> event.itemId).add()
                        .append(new KeyedCodec<>("@ItemAmount", Codec.STRING),
                                (event, value) -> event.itemAmount = value != null ? value : "",
                                event -> event.itemAmount).add()
                        .append(new KeyedCodec<>("@EffectId", Codec.STRING),
                                (event, value) -> event.effectId = value != null ? value : "",
                                event -> event.effectId).add()
                        .build();
    }
}
