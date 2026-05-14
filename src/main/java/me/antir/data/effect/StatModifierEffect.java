package me.antir.data.effect;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

import javax.annotation.Nonnull;

/**
 * Applies a {@link StaticModifier} to an entity's stat map.
 *
 * <p>{@link PostApplyBehavior} controls what happens to the current value after the
 * modifier changes the max bound:
 * <ul>
 *   <li>{@code NONE} — current value is preserved (clamped to new max if it exceeded it)</li>
 *   <li>{@code FILL} — current value is always set to the new max</li>
 *   <li>{@code SMART_FILL} — current is set to new max only if it was already at max before</li>
 * </ul>
 *
 * <p>All three paths use {@link EntityStatMap#setStatValue} after {@code update()} to
 * explicitly restore the correct current value, preventing the reset-to-zero that would
 * otherwise occur because {@code update()} recalculates current from modifier sums.
 */
public class StatModifierEffect implements ITitleEffect
{
    public enum PostApplyBehavior { NONE, FILL, SMART_FILL }

    @Nonnull
    public static final BuilderCodec<StatModifierEffect> CODEC;

    private String stat = "";
    private StaticModifier.CalculationType calculationType = StaticModifier.CalculationType.ADDITIVE;
    private float amount = 0f;
    private Modifier.ModifierTarget target = Modifier.ModifierTarget.MAX;
    private PostApplyBehavior postApply = PostApplyBehavior.NONE;

    public String getStat() { return stat; }

    @Override
    public String getUiColor() { return amount >= 0 ? "#5ab08e" : "#c85050"; }

    @Override
    public String getDescription()
    {
        if (calculationType == StaticModifier.CalculationType.MULTIPLICATIVE)
        {
            return "x" + amount + " " + stat + " (" + target + ")";
        }
        String sign = amount >= 0 ? "+" : "";
        return sign + (int) amount + " " + stat + " (" + target + ")";
    }

    @Override
    public void apply(TitleEffectContext ctx)
    {
        applyScaled(ctx, 1.0f);
    }

    public void applyScaled(TitleEffectContext ctx, float scale)
    {
        EntityStatMap statMap = ctx.store.getComponent(ctx.ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        int statIndex = EntityStatType.getAssetMap().getIndexOrDefault(stat, Integer.MIN_VALUE);
        if (statIndex == Integer.MIN_VALUE) return;

        EntityStatValue statBefore = statMap.get(statIndex);
        float savedCurrent = statBefore != null ? statBefore.get() : 0f;
        float savedMax    = statBefore != null ? statBefore.getMax() : 0f;

        statMap.putModifier(statIndex, modifierKey(ctx), new StaticModifier(target, calculationType, amount * scale));
        statMap.update();

        EntityStatValue statAfter = statMap.get(statIndex);
        float newMax = statAfter != null ? statAfter.getMax() : 0f;

        float targetCurrent;
        if (postApply == PostApplyBehavior.FILL)
        {
            targetCurrent = newMax;
        }
        else if (postApply == PostApplyBehavior.SMART_FILL)
        {
            targetCurrent = savedCurrent >= savedMax ? newMax : Math.min(savedCurrent, newMax);
        }
        else
        {
            targetCurrent = Math.min(savedCurrent, newMax);
        }

        statMap.setStatValue(statIndex, targetCurrent);
    }

    @Override
    public void remove(TitleEffectContext ctx)
    {
        EntityStatMap statMap = ctx.store.getComponent(ctx.ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        int statIndex = EntityStatType.getAssetMap().getIndexOrDefault(stat, Integer.MIN_VALUE);
        if (statIndex == Integer.MIN_VALUE) return;

        EntityStatValue statBefore = statMap.get(statIndex);
        float savedCurrent = statBefore != null ? statBefore.get() : 0f;

        statMap.removeModifier(statIndex, modifierKey(ctx));
        statMap.update();

        EntityStatValue statAfter = statMap.get(statIndex);
        float newMax = statAfter != null ? statAfter.getMax() : 0f;
        // Clamp current to the reduced max; never let it sit at 0 after a modifier removal.
        statMap.setStatValue(statIndex, Math.min(savedCurrent, newMax));
    }

    public static StatModifierEffect create(@Nonnull String stat,
                                            @Nonnull StaticModifier.CalculationType calculationType,
                                            float amount,
                                            @Nonnull Modifier.ModifierTarget target,
                                            @Nonnull PostApplyBehavior postApply)
    {
        StatModifierEffect effect = new StatModifierEffect();
        effect.stat            = stat;
        effect.calculationType = calculationType;
        effect.amount          = amount;
        effect.target          = target;
        effect.postApply       = postApply;
        return effect;
    }

    private String modifierKey(TitleEffectContext ctx) {
        return "title:" + ctx.title.getId() + ":stat:" + stat;
    }

    static
    {
        CODEC = BuilderCodec.builder(StatModifierEffect.class, StatModifierEffect::new)
                .append(new KeyedCodec<>("Stat", Codec.STRING),
                        (effect, value) -> effect.stat = value != null ? value : "", effect -> effect.stat).add()
                .append(new KeyedCodec<>("CalculationType", new EnumCodec<>(StaticModifier.CalculationType.class)),
                        (effect, value) -> effect.calculationType = value != null ? value : StaticModifier.CalculationType.ADDITIVE,
                        effect -> effect.calculationType).add()
                .append(new KeyedCodec<>("Amount", Codec.FLOAT),
                        (effect, value) -> effect.amount = value != null ? value : 0f, effect -> effect.amount).add()
                .append(new KeyedCodec<>("Target", new EnumCodec<>(Modifier.ModifierTarget.class, EnumCodec.EnumStyle.CAMEL_CASE)),
                        (effect, value) -> effect.target = value != null ? value : Modifier.ModifierTarget.MAX,
                        effect -> effect.target).add()
                .append(new KeyedCodec<>("PostApply", new EnumCodec<>(PostApplyBehavior.class, EnumCodec.EnumStyle.CAMEL_CASE)),
                        (effect, value) -> effect.postApply = value != null ? value : PostApplyBehavior.NONE,
                        effect -> effect.postApply).add()
                .build();
    }
}
