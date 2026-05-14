package me.antir.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.systems.NPCSystems;
import me.antir.api.TitleAPI;
import me.antir.components.RequirementProgressComponent;
import me.antir.components.TitleComponent;
import me.antir.data.Title;
import me.antir.data.requirement.ITitleRequirement;
import me.antir.data.requirement.KillCountRequirement;
import me.antir.data.requirement.TierProgressRequirement;

import javax.annotation.Nonnull;

/**
 * Grants titles with {@link KillCountRequirement}s when their kill targets are reached.
 * Extend or replace {@link #getNpcTypeId} once the Hytale NPC type API is confirmed.
 */
public class TitleKillCheck extends NPCSystems.OnDeathSystem
{
    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer)
    {
        String npcType = getNpcTypeId(ref, store);
        if (npcType == null || npcType.isEmpty()) return;

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null) return;
        if (!(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (!killerRef.isValid()) return;
        if (store.getComponent(killerRef, PlayerRef.getComponentType()) == null) return;

        RequirementProgressComponent prog = store.getComponent(killerRef, RequirementProgressComponent.getComponentType());
        if (prog == null) return;

        TitleComponent titles = store.getComponent(killerRef, TitleComponent.getComponentType());
        if (titles == null) return;

        for (Title title : Title.getAssetMap().getAssetMap().values())
        {
            if (titles.hasTitle(title.getId())) continue;

            ITitleRequirement[] requirements = title.getRequirements();
            for (int i = 0; i < requirements.length; i++)
            {
                if (requirements[i] instanceof KillCountRequirement killReq)
                {
                    if (!killReq.getNpcType().equals(npcType)) continue;

                    String key = killReq.progressKey(title.getId(), i);
                    int newCount = prog.incrementProgress(key, 1);

                    if (newCount >= killReq.getTarget() && areAllRequirementsMet(title, prog))
                    {
                        TitleAPI.grantTitle(killerRef, store, title);
                    }
                }
                else if (requirements[i] instanceof TierProgressRequirement tierReq)
                {
                    boolean npcTypeMatches = false;
                    for (String type : tierReq.getKillNpcTypes())
                    {
                        if (type.equals(npcType)) { npcTypeMatches = true; break; }
                    }
                    if (!npcTypeMatches) continue;

                    // Only add XP when the player owns the previous tier (avoids double-accumulation)
                    if (!tierReq.getPreviousTitleId().isEmpty()
                            && !titles.hasTitle(tierReq.getPreviousTitleId())) continue;

                    TitleTierProgressCheck.addXp(killerRef, store, tierReq.getProgressKey(), tierReq.getXpPerKill());
                }
            }
        }
    }

    private boolean areAllRequirementsMet(Title title, RequirementProgressComponent prog)
    {
        ITitleRequirement[] requirements = title.getRequirements();
        for (int i = 0; i < requirements.length; i++)
        {
            if (prog.getProgress(requirements[i].progressKey(title.getId(), i)) < requirements[i].getTarget())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * TODO: return the NPC role/type ID (e.g. {@code "Wolf_Black"}) for the dead entity.
     * Look up the correct component or API on {@link com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter}.
     */
    private String getNpcTypeId(Ref<EntityStore> ref, Store<EntityStore> store)
    {
        return null;
    }
}
