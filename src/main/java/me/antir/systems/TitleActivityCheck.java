package me.antir.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.api.TitleAPI;
import me.antir.components.RequirementProgressComponent;
import me.antir.components.TitleComponent;
import me.antir.data.Title;
import me.antir.data.requirement.ActivityCountRequirement;
import me.antir.data.requirement.ITitleRequirement;

/**
 * Grants titles with {@link ActivityCountRequirement}s.
 * Call {@link #onActivity} from any activity event handler (fishing, crafting, mining, etc.).
 *
 * <p>Activity keys must match the {@code "Activity"} field in the JSON requirement.
 * Common keys: {@code "FishCaught"}, {@code "OreMined"}, {@code "ItemCrafted"}, {@code "ZoneDiscovered"}.
 *
 * <p>TODO: Hook {@link #onActivity} into the appropriate Hytale event handlers once the API is known.
 */
public final class TitleActivityCheck
{
    private TitleActivityCheck() {}

    public static void onActivity(Ref<EntityStore> ref, Store<EntityStore> store,
                                   String activity, int amount)
    {
        RequirementProgressComponent prog = store.getComponent(ref, RequirementProgressComponent.getComponentType());
        if (prog == null) return;

        TitleComponent titles = store.getComponent(ref, TitleComponent.getComponentType());
        if (titles == null) return;

        for (Title title : Title.getAssetMap().getAssetMap().values())
        {
            if (titles.hasTitle(title.getId())) continue;

            ITitleRequirement[] requirements = title.getRequirements();
            for (int i = 0; i < requirements.length; i++)
            {
                if (!(requirements[i] instanceof ActivityCountRequirement actReq)) continue;
                if (!actReq.getActivity().equals(activity)) continue;

                String key = actReq.progressKey(title.getId(), i);
                int newCount = prog.incrementProgress(key, amount);

                if (newCount >= actReq.getTarget() && areAllRequirementsMet(title, prog))
                {
                    TitleAPI.grantTitle(ref, store, title);
                }
            }
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
