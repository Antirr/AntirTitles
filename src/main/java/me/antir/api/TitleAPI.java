package me.antir.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.components.RequirementProgressComponent;
import me.antir.components.TitleComponent;
import me.antir.data.ITitle;
import me.antir.data.Title;
import me.antir.data.requirement.ITitleRequirement;
import me.antir.events.TitleEquipEvent;
import me.antir.events.TitleEquipEvent.TitleEquipEventType;
import me.antir.events.TitleGrantEvent;
import me.antir.events.TitleGrantEvent.TitleGrantEventType;
import me.antir.systems.TitleTierProgressCheck;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utilities for common title operations.
 * All mutating methods fire the appropriate cancellable event so any registered
 * listener can observe or veto the action.
 */
public final class TitleAPI
{
    /** Runtime titles created via the admin UI — kept separate from the JSON asset store. */
    private static final ConcurrentHashMap<String, Title> runtimeTitles = new ConcurrentHashMap<>();

    private TitleAPI() {}

    // ── Events ────────────────────────────────────────────────────────────────

    public static void grantTitle(Ref<EntityStore> ref, Store<EntityStore> store, ITitle title)
    {
        store.invoke(ref, new TitleGrantEvent(title, TitleGrantEventType.GRANT));
    }

    public static void revokeTitle(Ref<EntityStore> ref, Store<EntityStore> store, ITitle title)
    {
        store.invoke(ref, new TitleGrantEvent(title, TitleGrantEventType.REMOVE));
    }

    public static void equipTitle(Ref<EntityStore> ref, Store<EntityStore> store, ITitle title)
    {
        store.invoke(ref, new TitleEquipEvent(title, TitleEquipEventType.EQUIP));
    }

    public static void unequipTitle(Ref<EntityStore> ref, Store<EntityStore> store, ITitle title)
    {
        store.invoke(ref, new TitleEquipEvent(title, TitleEquipEventType.UNEQUIP));
    }

    // ── Tier progression ──────────────────────────────────────────────────────

    /**
     * Adds XP toward a {@link me.antir.data.requirement.TierProgressRequirement} chain.
     * If the threshold is reached the current tier is revoked and the next tier is granted
     * (and re-equipped if applicable). Intended to be called from non-kill event handlers
     * such as fishing, mining, or crafting; kill-driven XP is handled by
     * {@link me.antir.systems.TitleKillCheck} automatically.
     */
    public static void addTierXp(Ref<EntityStore> ref, Store<EntityStore> store,
                                  String progressKey, int amount)
    {
        TitleTierProgressCheck.addXp(ref, store, progressKey, amount);
    }

    // ── Asset lookup ──────────────────────────────────────────────────────────

    /**
     * Looks up a title by ID, checking the JSON asset store first,
     * then admin-created runtime titles.
     */
    @Nullable
    public static ITitle getTitle(@Nonnull String titleId)
    {
        Title assetTitle = Title.getAssetMap().getAsset(titleId);
        if (assetTitle != null) return assetTitle;
        return runtimeTitles.get(titleId);
    }

    /**
     * Looks up a concrete {@link Title} by ID across both the asset store and
     * the runtime map. Use {@link #getTitle} when only {@link ITitle} is needed.
     */
    @Nullable
    public static Title getTitleAsset(@Nonnull String titleId)
    {
        Title assetTitle = Title.getAssetMap().getAsset(titleId);
        if (assetTitle != null) return assetTitle;
        return runtimeTitles.get(titleId);
    }

    // ── Runtime title registry ────────────────────────────────────────────────

    /**
     * Registers a title that was created at runtime (e.g. via the admin UI).
     * Runtime titles live alongside JSON-loaded titles and can be granted/equipped normally.
     * Overwrites any existing entry with the same ID.
     */
    public static void registerRuntimeTitle(@Nonnull Title title)
    {
        runtimeTitles.put(title.getId(), title);
    }

    /** Removes a runtime title by ID. No-op if the ID is not a runtime title. */
    public static void unregisterRuntimeTitle(@Nonnull String titleId)
    {
        runtimeTitles.remove(titleId);
    }

    /** Returns true if the given ID belongs to a runtime (admin-created) title. */
    public static boolean isRuntimeTitle(@Nonnull String titleId)
    {
        return runtimeTitles.containsKey(titleId);
    }

    /** Read-only view of all runtime titles, ordered by insertion. */
    @Nonnull
    public static Collection<Title> getRuntimeTitles()
    {
        return Collections.unmodifiableCollection(runtimeTitles.values());
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

    public static boolean hasTitle(Ref<EntityStore> ref, Store<EntityStore> store, String titleId)
    {
        TitleComponent titleComp = store.getComponent(ref, TitleComponent.getComponentType());
        return titleComp != null && titleComp.hasTitle(titleId);
    }

    /**
     * Returns the current progress value for a specific requirement on a title,
     * or 0 if the component or title is missing.
     */
    public static int getRequirementProgress(Ref<EntityStore> ref, Store<EntityStore> store,
                                             String titleId, int requirementIndex)
    {
        RequirementProgressComponent progressComp = store.getComponent(ref, RequirementProgressComponent.getComponentType());
        if (progressComp == null) return 0;
        Title title = Title.getAssetMap().getAsset(titleId);
        if (title == null) return 0;
        ITitleRequirement[] requirements = title.getRequirements();
        if (requirementIndex >= requirements.length) return 0;
        ITitleRequirement requirement = requirements[requirementIndex];
        return progressComp.getProgress(requirement.progressKey(titleId, requirementIndex));
    }
}
