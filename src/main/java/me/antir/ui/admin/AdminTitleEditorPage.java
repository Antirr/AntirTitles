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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.TitlesModule;
import me.antir.api.TitleAPI;
import me.antir.data.Title;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.requirement.ITitleRequirement;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin form page for creating a new runtime title.
 * Holds pending effects and requirements until the user clicks CREATE.
 * Navigates to {@link AdminEffectEditorPage} and {@link AdminRequirementEditorPage} to add items,
 * then returns to this page instance (state is preserved across navigation).
 */
public class AdminTitleEditorPage extends InteractiveCustomUIPage<AdminTitleEditorPage.EditorEvent>
{
    private static final String EDITOR_UI      = "Titles/Admin/AdminTitleEditor.ui";
    private static final String PENDING_ROW_UI = "Titles/Admin/AdminPendingRow.ui";

    @Nonnull private final PlayerRef playerRef;

    // Saved form state — restored when returning from sub-pages
    private String savedId          = "";
    private String savedDisplayName = "";
    private String savedDescription = "";
    private String savedRarity      = "Common";
    private String savedTags        = "";
    private String savedAccentColor = "";

    // Pending effects / requirements collected before submission
    private final List<ITitleEffect>     pendingPassive      = new ArrayList<>();
    private final List<ITitleEffect>     pendingActive       = new ArrayList<>();
    private final List<ITitleRequirement> pendingRequirements = new ArrayList<>();

    public AdminTitleEditorPage(@Nonnull PlayerRef playerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, EditorEvent.CODEC);
        this.playerRef = playerRef;
    }

    // ── Public API for sub-pages ──────────────────────────────────────────────

    public void addPendingEffect(@Nonnull ITitleEffect effect, @Nonnull String target)
    {
        if ("passive".equals(target)) pendingPassive.add(effect);
        else                          pendingActive.add(effect);
    }

    public void addPendingRequirement(@Nonnull ITitleRequirement requirement)
    {
        pendingRequirements.add(requirement);
    }

    // ── Page build ────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store)
    {
        builder.append(EDITOR_UI);

        // Restore saved field values after returning from a sub-page
        if (!savedId.isEmpty())
        {
            builder.set("#IdInput.Value",          savedId);
            builder.set("#DisplayNameInput.Value", savedDisplayName);
            builder.set("#DescriptionInput.Value", savedDescription);
            builder.set("#TagsInput.Value",        savedTags);
            builder.set("#AccentColorInput.Value", savedAccentColor);
        }

        // Rarity dropdown
        builder.set("#RarityInput.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Common"),    "Common"),
                new DropdownEntryInfo(LocalizableString.fromString("Uncommon"),  "Uncommon"),
                new DropdownEntryInfo(LocalizableString.fromString("Rare"),      "Rare"),
                new DropdownEntryInfo(LocalizableString.fromString("Epic"),      "Epic"),
                new DropdownEntryInfo(LocalizableString.fromString("Legendary"), "Legendary")
        ));
        builder.set("#RarityInput.Value", savedRarity.isEmpty() ? "Common" : savedRarity);

        // Pending passive effects
        for (int i = 0; i < pendingPassive.size(); i++)
        {
            builder.append("#PassiveEffectList", PENDING_ROW_UI);
            builder.set("#PassiveEffectList[" + i + "] #RowDesc.Text", pendingPassive.get(i).getDescription());
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#PassiveEffectList[" + i + "] #RemoveBtn",
                    EventData.of("Action", "removeEffect")
                            .append("Target", "passive")
                            .append("Index", String.valueOf(i)));
        }

        // Pending active effects
        for (int i = 0; i < pendingActive.size(); i++)
        {
            builder.append("#ActiveEffectList", PENDING_ROW_UI);
            builder.set("#ActiveEffectList[" + i + "] #RowDesc.Text", pendingActive.get(i).getDescription());
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ActiveEffectList[" + i + "] #RemoveBtn",
                    EventData.of("Action", "removeEffect")
                            .append("Target", "active")
                            .append("Index", String.valueOf(i)));
        }

        // Pending requirements
        for (int i = 0; i < pendingRequirements.size(); i++)
        {
            builder.append("#RequirementList", PENDING_ROW_UI);
            builder.set("#RequirementList[" + i + "] #RowDesc.Text", pendingRequirements.get(i).getDescription());
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#RequirementList[" + i + "] #RemoveBtn",
                    EventData.of("Action", "removeRequirement")
                            .append("Index", String.valueOf(i)));
        }

        // Submit button — captures all form fields
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton",
                formCapture("submit"));

        // Cancel
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton",
                EventData.of("Action", "cancel"));

        // Add effect buttons
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddPassiveEffectBtn",
                formCapture("addEffect").append("EffectTarget", "passive"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddActiveEffectBtn",
                formCapture("addEffect").append("EffectTarget", "active"));

        // Add requirement button
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddRequirementBtn",
                formCapture("addRequirement"));
    }

    /** Builds EventData that captures all current form field values via @ references. */
    @Nonnull
    private static EventData formCapture(@Nonnull String action)
    {
        return new EventData()
                .append("Action",          action)
                .append("@Id",             "#IdInput.Value")
                .append("@DisplayName",    "#DisplayNameInput.Value")
                .append("@Description",    "#DescriptionInput.Value")
                .append("@Rarity",         "#RarityInput.Value")
                .append("@Tags",           "#TagsInput.Value")
                .append("@AccentColor",    "#AccentColorInput.Value");
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull EditorEvent editorEvent)
    {
        switch (editorEvent.action)
        {
            case "submit"             -> handleSubmit(ref, store, editorEvent);
            case "cancel"             -> { clearState(); closeAndReturnToBrowser(ref, store); }
            case "addEffect"          -> handleAddEffect(ref, store, editorEvent);
            case "addRequirement"     -> handleAddRequirement(ref, store, editorEvent);
            case "removeEffect"       -> handleRemoveEffect(editorEvent);
            case "removeRequirement"  -> handleRemoveRequirement(editorEvent);
        }
    }

    private void handleSubmit(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                              @Nonnull EditorEvent editorEvent)
    {
        String titleId     = editorEvent.id.trim();
        String displayName = editorEvent.displayName.trim();

        if (titleId.isEmpty() || displayName.isEmpty())
        {
            UICommandBuilder errorBuilder = new UICommandBuilder();
            errorBuilder.set("#EditorError.Text", "ID and Display Name are required.");
            errorBuilder.set("#EditorError.Visible", true);
            sendUpdate(errorBuilder, new UIEventBuilder(), false);
            return;
        }

        if (TitleAPI.getTitle(titleId) != null)
        {
            UICommandBuilder errorBuilder = new UICommandBuilder();
            errorBuilder.set("#EditorError.Text", "A title with ID \"" + titleId + "\" already exists.");
            errorBuilder.set("#EditorError.Visible", true);
            sendUpdate(errorBuilder, new UIEventBuilder(), false);
            return;
        }

        String[] tagArray = editorEvent.tags.isEmpty()
                ? new String[0]
                : editorEvent.tags.split(",");

        Title newTitle = Title.create(
                titleId, displayName, editorEvent.description, editorEvent.rarity,
                tagArray, editorEvent.accentColor,
                pendingPassive.toArray(new ITitleEffect[0]),
                pendingActive.toArray(new ITitleEffect[0]),
                pendingRequirements.toArray(new ITitleRequirement[0]));

        TitleAPI.registerRuntimeTitle(newTitle);
        TitlesModule.get().persistRuntimeTitle(newTitle);

        clearState();
        closeAndReturnToBrowser(ref, store);
    }

    private void handleAddEffect(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                 @Nonnull EditorEvent editorEvent)
    {
        saveFormState(editorEvent);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        player.getPageManager().openCustomPage(ref, store,
                new AdminEffectEditorPage(this, playerRef, editorEvent.effectTarget));
    }

    private void handleAddRequirement(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                      @Nonnull EditorEvent editorEvent)
    {
        saveFormState(editorEvent);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        player.getPageManager().openCustomPage(ref, store,
                new AdminRequirementEditorPage(this, playerRef));
    }

    private void handleRemoveEffect(@Nonnull EditorEvent editorEvent)
    {
        int index = parseIndex(editorEvent.removeIndex);
        if ("passive".equals(editorEvent.removeTarget) && index >= 0 && index < pendingPassive.size())
        {
            pendingPassive.remove(index);
        }
        else if ("active".equals(editorEvent.removeTarget) && index >= 0 && index < pendingActive.size())
        {
            pendingActive.remove(index);
        }
        rebuild();
    }

    private void handleRemoveRequirement(@Nonnull EditorEvent editorEvent)
    {
        int index = parseIndex(editorEvent.removeIndex);
        if (index >= 0 && index < pendingRequirements.size())
        {
            pendingRequirements.remove(index);
        }
        rebuild();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveFormState(@Nonnull EditorEvent editorEvent)
    {
        savedId          = editorEvent.id;
        savedDisplayName = editorEvent.displayName;
        savedDescription = editorEvent.description;
        savedRarity      = editorEvent.rarity.isEmpty() ? "Common" : editorEvent.rarity;
        savedTags        = editorEvent.tags;
        savedAccentColor = editorEvent.accentColor;
    }

    private void clearState()
    {
        savedId = ""; savedDisplayName = ""; savedDescription = "";
        savedRarity = "Common"; savedTags = ""; savedAccentColor = "";
        pendingPassive.clear();
        pendingActive.clear();
        pendingRequirements.clear();
    }

    private void closeAndReturnToBrowser(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store)
    {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        player.getPageManager().openCustomPage(ref, store, new AdminTitleBrowserPage(playerRef));
    }

    private static int parseIndex(@Nonnull String indexStr)
    {
        try { return Integer.parseInt(indexStr); }
        catch (NumberFormatException ignored) { return -1; }
    }

    // ── Event data ────────────────────────────────────────────────────────────

    public static class EditorEvent
    {
        public String action      = "";
        // Form fields (@ codec keys — client resolves element values)
        public String id          = "";
        public String displayName = "";
        public String description = "";
        public String rarity      = "Common";
        public String tags        = "";
        public String accentColor = "";
        // Navigation fields (plain static values)
        public String effectTarget  = "";   // "passive" or "active"
        public String removeTarget  = "";   // "passive" or "active"
        public String removeIndex   = "";   // string index for removeEffect / removeRequirement

        public static final BuilderCodec<EditorEvent> CODEC =
                BuilderCodec.builder(EditorEvent.class, EditorEvent::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (editorEvent, value) -> editorEvent.action = value != null ? value : "",
                                editorEvent -> editorEvent.action).add()
                        .append(new KeyedCodec<>("@Id", Codec.STRING),
                                (editorEvent, value) -> editorEvent.id = value != null ? value : "",
                                editorEvent -> editorEvent.id).add()
                        .append(new KeyedCodec<>("@DisplayName", Codec.STRING),
                                (editorEvent, value) -> editorEvent.displayName = value != null ? value : "",
                                editorEvent -> editorEvent.displayName).add()
                        .append(new KeyedCodec<>("@Description", Codec.STRING),
                                (editorEvent, value) -> editorEvent.description = value != null ? value : "",
                                editorEvent -> editorEvent.description).add()
                        .append(new KeyedCodec<>("@Rarity", Codec.STRING),
                                (editorEvent, value) -> editorEvent.rarity = value != null && !value.isEmpty() ? value : "Common",
                                editorEvent -> editorEvent.rarity).add()
                        .append(new KeyedCodec<>("@Tags", Codec.STRING),
                                (editorEvent, value) -> editorEvent.tags = value != null ? value : "",
                                editorEvent -> editorEvent.tags).add()
                        .append(new KeyedCodec<>("@AccentColor", Codec.STRING),
                                (editorEvent, value) -> editorEvent.accentColor = value != null ? value : "",
                                editorEvent -> editorEvent.accentColor).add()
                        .append(new KeyedCodec<>("EffectTarget", Codec.STRING),
                                (editorEvent, value) -> editorEvent.effectTarget = value != null ? value : "",
                                editorEvent -> editorEvent.effectTarget).add()
                        .append(new KeyedCodec<>("Target", Codec.STRING),
                                (editorEvent, value) -> editorEvent.removeTarget = value != null ? value : "",
                                editorEvent -> editorEvent.removeTarget).add()
                        .append(new KeyedCodec<>("Index", Codec.STRING),
                                (editorEvent, value) -> editorEvent.removeIndex = value != null ? value : "",
                                editorEvent -> editorEvent.removeIndex).add()
                        .build();
    }
}
