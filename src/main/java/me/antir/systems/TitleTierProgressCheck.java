package me.antir.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.api.TitleAPI;
import me.antir.components.RequirementProgressComponent;
import me.antir.components.TitleComponent;
import me.antir.data.Title;
import me.antir.data.requirement.ITitleRequirement;
import me.antir.data.requirement.TierProgressRequirement;
import me.antir.ui.TitleUI;

/**
 * Handles XP accumulation and tier advancement for {@link TierProgressRequirement}s.
 *
 * <p>Call {@link #addXp} from any event that should contribute XP toward a progression
 * (fishing, mining, crafting, etc.). Kill-driven XP is handled automatically by
 * {@link TitleKillCheck} via {@link TierProgressRequirement#getKillNpcTypes()}.
 *
 * <p>On advancement:
 * <ol>
 *   <li>The previous-tier title is revoked if {@link TierProgressRequirement#isRemovePrevious()}.
 *   <li>The progress counter resets to zero.
 *   <li>The new-tier title is granted.
 *   <li>If the previous title was equipped, the new title is re-equipped automatically.
 * </ol>
 */
public final class TitleTierProgressCheck
{
    private TitleTierProgressCheck() {}

    public static void addXp(Ref<EntityStore> ref, Store<EntityStore> store,
                              String progressKey, int amount)
    {
        if (amount <= 0) return;

        RequirementProgressComponent prog = store.getComponent(ref, RequirementProgressComponent.getComponentType());
        if (prog == null) return;

        TitleComponent titles = store.getComponent(ref, TitleComponent.getComponentType());
        if (titles == null) return;

        int newXp = prog.incrementProgress(progressKey, amount);

        for (Title title : Title.getAssetMap().getAssetMap().values())
        {
            if (titles.hasTitle(title.getId())) continue;

            ITitleRequirement[] requirements = title.getRequirements();
            for (int i = 0; i < requirements.length; i++)
            {
                if (!(requirements[i] instanceof TierProgressRequirement tierReq)) continue;
                if (!tierReq.getProgressKey().equals(progressKey)) continue;

                // Player must own the previous tier (if specified)
                if (!tierReq.getPreviousTitleId().isEmpty()
                        && !titles.hasTitle(tierReq.getPreviousTitleId())) continue;

                if (newXp < tierReq.getTarget()) continue;
                if (!areAllRequirementsMet(title, prog)) continue;

                advanceTier(ref, store, prog, titles, title, tierReq, progressKey);
                return; // one transition per call
            }
        }
    }

    private static void advanceTier(Ref<EntityStore> ref, Store<EntityStore> store,
                                     RequirementProgressComponent prog, TitleComponent titles,
                                     Title nextTitle, TierProgressRequirement tierReq,
                                     String progressKey)
    {
        Title prevTitle = tierReq.getPreviousTitleId().isEmpty()
                ? null
                : Title.getAssetMap().getAsset(tierReq.getPreviousTitleId());

        boolean wasEquipped = prevTitle != null
                && titles.getEquippedTitleIds().contains(prevTitle.getId());

        if (prevTitle != null && tierReq.isRemovePrevious())
        {
            TitleAPI.revokeTitle(ref, store, prevTitle);
        }

        prog.setProgress(progressKey, 0);
        TitleAPI.grantTitle(ref, store, nextTitle);

        if (wasEquipped)
        {
            TitleAPI.equipTitle(ref, store, nextTitle);
        }
    }

    private static boolean areAllRequirementsMet(Title title, RequirementProgressComponent prog)
    {
        ITitleRequirement[] requirements = title.getRequirements();
        for (int i = 0; i < requirements.length; i++)
        {
            if (prog.getProgress(requirements[i].progressKey(title.getId(), i)) < requirements[i].getTarget())
                return false;
        }
        return true;
    }
}
