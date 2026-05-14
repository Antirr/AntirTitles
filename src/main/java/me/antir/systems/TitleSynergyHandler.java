package me.antir.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.components.TitleComponent;
import me.antir.data.EffectScale;
import me.antir.data.Title;
import me.antir.data.TitleSynergy;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.effect.StatModifierEffect;
import me.antir.data.effect.TitleEffectContext;
import me.antir.events.TitleEquipEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Evaluates all {@code Server/TitleSynergies/*.json} after every equip or unequip.
 * Runs after the individual equip/unequip handlers so the component already reflects the change.
 */
public class TitleSynergyHandler extends EntityEventSystem<EntityStore, TitleEquipEvent>
{
    public TitleSynergyHandler()
    {
        super(TitleEquipEvent.class);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery()
    {
        return TitleComponent.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull TitleEquipEvent event)
    {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        TitleComponent comp = chunk.getComponent(index, TitleComponent.getComponentType());
        if (comp == null) return;

        List<String> equipped = comp.getEquippedTitleIds();

        for (TitleSynergy synergy : TitleSynergy.getAssetMap().getAssetMap().values())
        {
            boolean conditionMet = synergy.isConditionMet(equipped);
            boolean wasActive = comp.getActiveSynergies().contains(synergy.getId());

            if (conditionMet && !wasActive)
            {
                comp.getActiveSynergies().add(synergy.getId());
                TitleEffectContext ctx = new TitleEffectContext(ref, store, commandBuffer, synergy);
                for (ITitleEffect effect : synergy.getBonusEffects()) effect.apply(ctx);
                applyEffectScales(synergy.getEffectScales(), equipped, ref, store, commandBuffer, true);

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null && !synergy.getDisplayName().isEmpty())
                {
                    me.antir.ui.TitleUI.showSynergyActivated(playerRef, synergy.getDisplayName());
                }
            }
            else if (!conditionMet && wasActive)
            {
                comp.getActiveSynergies().remove(synergy.getId());
                TitleEffectContext ctx = new TitleEffectContext(ref, store, commandBuffer, synergy);
                for (ITitleEffect effect : synergy.getBonusEffects()) effect.remove(ctx);
                applyEffectScales(synergy.getEffectScales(), equipped, ref, store, commandBuffer, false);
            }
        }
    }

    /**
     * For each equipped title, applies (or reverts) the synergy's {@link EffectScale}s to
     * matching {@link StatModifierEffect}s. Reverts by re-applying at scale 1.0, which
     * replaces the amplified modifier in the stat map with the original.
     */
    private void applyEffectScales(EffectScale[] scales, List<String> equippedIds,
                                    Ref<EntityStore> ref, Store<EntityStore> store,
                                    CommandBuffer<EntityStore> commandBuffer, boolean activate)
    {
        if (scales.length == 0) return;

        for (String equippedId : equippedIds)
        {
            Title title = Title.getAssetMap().getAsset(equippedId);
            if (title == null) continue;

            TitleEffectContext ctx = new TitleEffectContext(ref, store, commandBuffer, title);
            scaleEffects(title.getActiveEffects(), scales, ctx, activate, false);
            scaleEffects(title.getPassiveEffects(), scales, ctx, activate, true);
        }
    }

    private void scaleEffects(ITitleEffect[] effects, EffectScale[] scales,
                               TitleEffectContext ctx, boolean activate, boolean passiveEffects)
    {
        for (EffectScale scale : scales)
        {
            if (scale.isAffectsPassive() != passiveEffects) continue;
            float multiplier = activate ? scale.getMultiplier() : 1.0f;

            for (ITitleEffect effect : effects)
            {
                if (!(effect instanceof StatModifierEffect statEffect)) continue;
                if (scale.getTargetStat() != null && !scale.getTargetStat().equals(statEffect.getStat())) continue;
                statEffect.applyScaled(ctx, multiplier);
            }
        }
    }
}
