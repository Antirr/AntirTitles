package me.antir.data.effect;

import com.hypixel.hytale.codec.lookup.CodecMapCodec;

public interface ITitleEffect
{
    CodecMapCodec<ITitleEffect> CODEC = new CodecMapCodec<>("Type", false);

    void apply(TitleEffectContext context);

    /**
     * Reverses the effect applied by {@link #apply}. Called on title unequip,
     * title removal, or synergy deactivation. Non-reversible effects (e.g. item
     * grants) leave this as a no-op.
     */
    void remove(TitleEffectContext context);

    /** One-line human-readable summary shown in the title browser detail panel. */
    String getDescription();

    /** Hex color string (#rrggbb) used when rendering this effect in the UI. */
    default String getUiColor() { return "#c8d4e0"; }
}
