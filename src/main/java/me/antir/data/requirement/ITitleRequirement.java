package me.antir.data.requirement;

import com.hypixel.hytale.codec.lookup.CodecMapCodec;

public interface ITitleRequirement
{
    CodecMapCodec<ITitleRequirement> CODEC = new CodecMapCodec<>("Type", false);

    /** Returns the progress key for this requirement. Keys are per-title and per-index. */
    String progressKey(String titleId, int requirementIndex);

    /** Returns the target count needed to satisfy this requirement. */
    int getTarget();

    /** One-line description shown in the admin title editor. */
    default String getDescription() { return "Requirement (target: " + getTarget() + ")"; }
}
