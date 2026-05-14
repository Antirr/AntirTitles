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
import me.antir.data.requirement.ActivityCountRequirement;
import me.antir.data.requirement.ITitleRequirement;
import me.antir.data.requirement.KillCountRequirement;
import me.antir.data.requirement.ObtainItemRequirement;
import me.antir.data.requirement.TierProgressRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Admin sub-page for creating a single {@link ITitleRequirement}.
 * Supports: KillCount, ObtainItem, ActivityCount, TierProgress.
 * On submit, passes the created requirement to the parent {@link AdminTitleEditorPage} and returns.
 */
public class AdminRequirementEditorPage extends InteractiveCustomUIPage<AdminRequirementEditorPage.ReqEditorEvent>
{
    private static final String REQ_EDITOR_UI = "Titles/Admin/AdminRequirementEditor.ui";

    @Nonnull private final AdminTitleEditorPage editorPage;
    @Nonnull private final PlayerRef            playerRef;
    @Nonnull private       String               selectedType = "KillCount";

    public AdminRequirementEditorPage(@Nonnull AdminTitleEditorPage editorPage,
                                      @Nonnull PlayerRef playerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, ReqEditorEvent.CODEC);
        this.editorPage = editorPage;
        this.playerRef  = playerRef;
    }

    // ── Page build ────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store)
    {
        builder.append(REQ_EDITOR_UI);

        // Type dropdown
        builder.set("#ReqTypeDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Kill Count"),     "KillCount"),
                new DropdownEntryInfo(LocalizableString.fromString("Obtain Item"),    "ObtainItem"),
                new DropdownEntryInfo(LocalizableString.fromString("Activity Count"), "ActivityCount"),
                new DropdownEntryInfo(LocalizableString.fromString("Tier Progress"),  "TierProgress")
        ));
        builder.set("#ReqTypeDropdown.Value", selectedType);

        // Section visibility
        builder.set("#KillCountSection.Visible",     "KillCount".equals(selectedType));
        builder.set("#ObtainItemSection.Visible",    "ObtainItem".equals(selectedType));
        builder.set("#ActivityCountSection.Visible", "ActivityCount".equals(selectedType));
        builder.set("#TierProgressSection.Visible",  "TierProgress".equals(selectedType));

        // TierProgress: populate Yes/No dropdown
        if ("TierProgress".equals(selectedType))
        {
            builder.set("#TierRemovePreviousInput.Entries", List.of(
                    new DropdownEntryInfo(LocalizableString.fromString("Yes"), "true"),
                    new DropdownEntryInfo(LocalizableString.fromString("No"),  "false")
            ));
            builder.set("#TierRemovePreviousInput.Value", "true");
        }

        // ADD button with type-specific field captures
        EventData addData = new EventData().append("Action", "submit");
        switch (selectedType)
        {
            case "KillCount" -> addData
                    .append("@NpcType",   "#NpcTypeInput.Value")
                    .append("@KillCount", "#KillCountInput.Value");
            case "ObtainItem" -> addData
                    .append("@ObtainItemId",    "#ObtainItemIdInput.Value")
                    .append("@ObtainItemCount", "#ObtainItemCountInput.Value");
            case "ActivityCount" -> addData
                    .append("@Activity",      "#ActivityInput.Value")
                    .append("@ActivityCount", "#ActivityCountInput.Value");
            case "TierProgress" -> addData
                    .append("@TierProgressKey",     "#TierProgressKeyInput.Value")
                    .append("@TierXpRequired",      "#TierXpRequiredInput.Value")
                    .append("@TierPrevTitleId",     "#TierPrevTitleInput.Value")
                    .append("@TierKillNpcTypes",    "#TierKillNpcTypesInput.Value")
                    .append("@TierXpPerKill",       "#TierXpPerKillInput.Value")
                    .append("@TierRemovePrevious",  "#TierRemovePreviousInput.Value");
        }
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddReqBtn", addData);

        // ValueChanged on type dropdown
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ReqTypeDropdown",
                new EventData().append("Action", "typeChanged")
                               .append("@ReqType", "#ReqTypeDropdown.Value"),
                false);

        // Cancel
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelReqBtn",
                EventData.of("Action", "cancel"));
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ReqEditorEvent event)
    {
        switch (event.action)
        {
            case "typeChanged" ->
            {
                if (!event.reqType.isEmpty()) this.selectedType = event.reqType;
                rebuild();
            }
            case "submit" -> handleSubmit(ref, store, event);
            case "cancel" -> returnToEditor(ref, store);
        }
    }

    private void handleSubmit(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                              @Nonnull ReqEditorEvent event)
    {
        ITitleRequirement requirement = switch (selectedType)
        {
            case "KillCount"     -> buildKillCount(event);
            case "ObtainItem"    -> buildObtainItem(event);
            case "ActivityCount" -> buildActivityCount(event);
            case "TierProgress"  -> buildTierProgress(event);
            default              -> null;
        };

        if (requirement != null) editorPage.addPendingRequirement(requirement);
        returnToEditor(ref, store);
    }

    @Nullable
    private static ITitleRequirement buildKillCount(@Nonnull ReqEditorEvent event)
    {
        String npcType = event.npcType.trim();
        if (npcType.isEmpty()) return null;
        return KillCountRequirement.create(npcType, parsePositiveInt(event.killCount, 1));
    }

    @Nullable
    private static ITitleRequirement buildObtainItem(@Nonnull ReqEditorEvent event)
    {
        String itemId = event.obtainItemId.trim();
        if (itemId.isEmpty()) return null;
        return ObtainItemRequirement.create(itemId, parsePositiveInt(event.obtainItemCount, 1));
    }

    @Nullable
    private static ITitleRequirement buildActivityCount(@Nonnull ReqEditorEvent event)
    {
        String activity = event.activity.trim();
        if (activity.isEmpty()) return null;
        return ActivityCountRequirement.create(activity, parsePositiveInt(event.activityCount, 1));
    }

    @Nullable
    private static ITitleRequirement buildTierProgress(@Nonnull ReqEditorEvent event)
    {
        String progressKey = event.tierProgressKey.trim();
        if (progressKey.isEmpty()) return null;

        int xpRequired = parsePositiveInt(event.tierXpRequired, 1);
        String prevTitle = event.tierPrevTitleId.trim();

        String[] killNpcTypes = event.tierKillNpcTypes.trim().isEmpty()
                ? new String[0]
                : event.tierKillNpcTypes.trim().split(",");

        int xpPerKill      = parsePositiveInt(event.tierXpPerKill, 1);
        boolean removePrev = !"false".equals(event.tierRemovePrevious);

        return TierProgressRequirement.create(progressKey, xpRequired, prevTitle,
                killNpcTypes, xpPerKill, removePrev);
    }

    private static int parsePositiveInt(@Nonnull String str, int defaultValue)
    {
        try { return Math.max(1, Integer.parseInt(str.trim())); }
        catch (NumberFormatException ignored) { return defaultValue; }
    }

    private void returnToEditor(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store)
    {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    // ── Event data ────────────────────────────────────────────────────────────

    public static class ReqEditorEvent
    {
        public String action           = "";
        public String reqType          = "";
        // KillCount
        public String npcType          = "";
        public String killCount        = "";
        // ObtainItem
        public String obtainItemId     = "";
        public String obtainItemCount  = "";
        // ActivityCount
        public String activity         = "";
        public String activityCount    = "";
        // TierProgress
        public String tierProgressKey    = "";
        public String tierXpRequired     = "";
        public String tierPrevTitleId    = "";
        public String tierKillNpcTypes   = "";
        public String tierXpPerKill      = "";
        public String tierRemovePrevious = "";

        public static final BuilderCodec<ReqEditorEvent> CODEC =
                BuilderCodec.builder(ReqEditorEvent.class, ReqEditorEvent::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (event, value) -> event.action = value != null ? value : "",
                                event -> event.action).add()
                        .append(new KeyedCodec<>("@ReqType", Codec.STRING),
                                (event, value) -> event.reqType = value != null ? value : "",
                                event -> event.reqType).add()
                        .append(new KeyedCodec<>("@NpcType", Codec.STRING),
                                (event, value) -> event.npcType = value != null ? value : "",
                                event -> event.npcType).add()
                        .append(new KeyedCodec<>("@KillCount", Codec.STRING),
                                (event, value) -> event.killCount = value != null ? value : "",
                                event -> event.killCount).add()
                        .append(new KeyedCodec<>("@ObtainItemId", Codec.STRING),
                                (event, value) -> event.obtainItemId = value != null ? value : "",
                                event -> event.obtainItemId).add()
                        .append(new KeyedCodec<>("@ObtainItemCount", Codec.STRING),
                                (event, value) -> event.obtainItemCount = value != null ? value : "",
                                event -> event.obtainItemCount).add()
                        .append(new KeyedCodec<>("@Activity", Codec.STRING),
                                (event, value) -> event.activity = value != null ? value : "",
                                event -> event.activity).add()
                        .append(new KeyedCodec<>("@ActivityCount", Codec.STRING),
                                (event, value) -> event.activityCount = value != null ? value : "",
                                event -> event.activityCount).add()
                        .append(new KeyedCodec<>("@TierProgressKey", Codec.STRING),
                                (event, value) -> event.tierProgressKey = value != null ? value : "",
                                event -> event.tierProgressKey).add()
                        .append(new KeyedCodec<>("@TierXpRequired", Codec.STRING),
                                (event, value) -> event.tierXpRequired = value != null ? value : "",
                                event -> event.tierXpRequired).add()
                        .append(new KeyedCodec<>("@TierPrevTitleId", Codec.STRING),
                                (event, value) -> event.tierPrevTitleId = value != null ? value : "",
                                event -> event.tierPrevTitleId).add()
                        .append(new KeyedCodec<>("@TierKillNpcTypes", Codec.STRING),
                                (event, value) -> event.tierKillNpcTypes = value != null ? value : "",
                                event -> event.tierKillNpcTypes).add()
                        .append(new KeyedCodec<>("@TierXpPerKill", Codec.STRING),
                                (event, value) -> event.tierXpPerKill = value != null ? value : "",
                                event -> event.tierXpPerKill).add()
                        .append(new KeyedCodec<>("@TierRemovePrevious", Codec.STRING),
                                (event, value) -> event.tierRemovePrevious = value != null ? value : "",
                                event -> event.tierRemovePrevious).add()
                        .build();
    }
}
