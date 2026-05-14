package me.antir.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.api.TitleAPI;
import me.antir.components.RequirementProgressComponent;
import me.antir.components.TitleComponent;
import me.antir.data.Title;
import me.antir.data.requirement.ITitleRequirement;
import me.antir.data.requirement.ObtainItemRequirement;

/**
 * Grants titles with {@link ObtainItemRequirement}s when their item-obtain targets are reached.
 * Call {@link #onItemObtained} from your item pickup / inventory-add event handler.
 *
 * <p>TODO: Hook {@link #onItemObtained} into the Hytale item pickup event once the API is known.
 */
public final class TitleItemCheck
{
    private TitleItemCheck() {}

    public static void onItemObtained(Ref<EntityStore> ref, Store<EntityStore> store, String itemId)
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
                if (!(requirements[i] instanceof ObtainItemRequirement itemReq)) continue;
                if (!itemReq.getItemId().equals(itemId)) continue;

                String key = itemReq.progressKey(title.getId(), i);
                int newCount = prog.incrementProgress(key, 1);

                if (newCount >= itemReq.getTarget() && areAllRequirementsMet(title, prog))
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
