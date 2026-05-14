package me.antir.data.effect;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;

import javax.annotation.Nonnull;

public class EntityEffectEffect implements ITitleEffect
{
    @Nonnull
    public static final BuilderCodec<EntityEffectEffect> CODEC = BuilderCodec.builder(EntityEffectEffect.class, EntityEffectEffect::new)
            .append(new KeyedCodec<>("EffectId", Codec.STRING),
                    (effect, value) -> effect.effectId = value != null ? value : "", effect -> effect.effectId).add()
            .build();

    private String effectId = "";

    @Override
    public String getUiColor() { return "#6880d0"; }

    @Override
    public String getDescription()
    {
        return "Applies " + effectId;
    }

    public static EntityEffectEffect create(@Nonnull String effectId)
    {
        EntityEffectEffect effect = new EntityEffectEffect();
        effect.effectId = effectId;
        return effect;
    }

    @Override
    public void apply(TitleEffectContext ctx)
    {
        int effectIndex = EntityEffect.getAssetMap().getIndexOrDefault(effectId, Integer.MIN_VALUE);
        if (effectIndex == Integer.MIN_VALUE) return;

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        if (effect == null) return;

        EffectControllerComponent controller = ctx.store.getComponent(ctx.ref, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        controller.addInfiniteEffect(ctx.ref, effectIndex, effect, ctx.commandBuffer);
    }

    @Override
    public void remove(TitleEffectContext ctx)
    {
        EffectControllerComponent controller = ctx.store.getComponent(ctx.ref, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        int effectIndex = EntityEffect.getAssetMap().getIndexOrDefault(effectId, Integer.MIN_VALUE);
        if (effectIndex == Integer.MIN_VALUE) return;

        controller.removeEffect(ctx.ref, effectIndex, ctx.commandBuffer);
    }
}
