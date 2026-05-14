package me.antir.data;

import me.antir.data.effect.ITitleEffect;

public interface ITitle
{
    String getId();

    String getDisplayName();

    /** Effects applied when the title is granted; reversed (remove()) when the title is removed. */
    ITitleEffect[] getPassiveEffects();

    /** Effects applied when the title is equipped; reversed (remove()) when unequipped. */
    ITitleEffect[] getActiveEffects();
}
