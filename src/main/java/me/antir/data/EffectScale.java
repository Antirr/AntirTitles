package me.antir.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Multiplies the {@code Amount} of matching {@link me.antir.data.effect.StatModifierEffect}s
 * on equipped titles when a synergy activates. On deactivation the original value is restored
 * by reapplying at scale 1.0.
 *
 * <p>{@code TargetStat} is optional — omit (or set to {@code null}) to scale ALL
 * {@link me.antir.data.effect.StatModifierEffect}s on the title.
 *
 * <p>Set {@code AffectsPassive} to {@code true} to also scale passive effects; by default
 * only active (equipped) effects are scaled.
 */
public class EffectScale
{
    @Nonnull
    public static final BuilderCodec<EffectScale> CODEC = BuilderCodec.builder(EffectScale.class, EffectScale::new)
            .append(new KeyedCodec<>("TargetStat", Codec.STRING),
                    (scale, value) -> scale.targetStat = value, scale -> scale.targetStat).add()
            .append(new KeyedCodec<>("Multiplier", Codec.FLOAT),
                    (scale, value) -> scale.multiplier = value != null ? value : 1.0f, scale -> scale.multiplier).add()
            .append(new KeyedCodec<>("AffectsPassive", Codec.BOOLEAN),
                    (scale, value) -> scale.affectsPassive = Boolean.TRUE.equals(value), scale -> scale.affectsPassive).add()
            .build();

    @Nullable private String targetStat;
    private float multiplier = 1.0f;
    private boolean affectsPassive = false;

    @Nullable
    public String getTargetStat() { return targetStat; }

    public float getMultiplier() { return multiplier; }

    public boolean isAffectsPassive() { return affectsPassive; }
}
