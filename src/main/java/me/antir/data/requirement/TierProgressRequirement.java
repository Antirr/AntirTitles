package me.antir.data.requirement;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Unlocks a title by accumulating XP on a shared {@link #progressKey} counter,
 * optionally revoking a previous-tier title on advancement.
 *
 * <p>XP is added via {@link me.antir.systems.TitleTierProgressCheck#addXp}.
 * For kill-driven XP, populate {@link #killNpcTypes} and {@link #xpPerKill};
 * {@link me.antir.systems.TitleKillCheck} will call {@code addXp} automatically.
 *
 * <p>JSON:
 * <pre>{@code
 * {
 *   "Type": "TierProgress",
 *   "ProgressKey": "WolfProgress",
 *   "XpRequired": 500,
 *   "PreviousTitleId": "WolfHunter",
 *   "KillNpcTypes": ["Wolf_Black", "Wolf_White"],
 *   "XpPerKill": 10,
 *   "RemovePrevious": true
 * }
 * }</pre>
 */
public class TierProgressRequirement implements ITitleRequirement
{
    @Nonnull
    public static final BuilderCodec<TierProgressRequirement> CODEC;

    private String progressKey = "";
    private int xpRequired = 1;
    private String previousTitleId = "";
    private String[] killNpcTypes = new String[0];
    private int xpPerKill = 1;
    private boolean removePrevious = true;

    public String getProgressKey() { return progressKey; }
    public String getPreviousTitleId() { return previousTitleId; }
    public String[] getKillNpcTypes() { return killNpcTypes; }
    public int getXpPerKill() { return xpPerKill; }
    public boolean isRemovePrevious() { return removePrevious; }

    @Override
    public int getTarget() { return xpRequired; }

    @Override
    public String getDescription()
    {
        String desc = xpRequired + " XP [" + progressKey + "]";
        if (!previousTitleId.isEmpty()) desc += " prev: " + previousTitleId;
        return desc;
    }

    public static TierProgressRequirement create(@Nonnull String progressKey, int xpRequired, @Nonnull String previousTitleId,
                                                 @Nonnull String[] killNpcTypes, int xpPerKill, boolean removePrevious)
    {
        TierProgressRequirement req = new TierProgressRequirement();
        req.progressKey     = progressKey;
        req.xpRequired      = xpRequired;
        req.previousTitleId = previousTitleId;
        req.killNpcTypes    = killNpcTypes;
        req.xpPerKill       = xpPerKill;
        req.removePrevious  = removePrevious;
        return req;
    }

    /** Returns the shared progress key — the same counter is used across all tiers in a chain. */
    @Override
    public String progressKey(String titleId, int requirementIndex)
    {
        return progressKey;
    }

    static
    {
        CODEC = BuilderCodec.builder(TierProgressRequirement.class, TierProgressRequirement::new)
                .append(new KeyedCodec<>("ProgressKey", Codec.STRING),
                        (requirement, value) -> requirement.progressKey = value != null ? value : "", requirement -> requirement.progressKey).add()
                .append(new KeyedCodec<>("XpRequired", Codec.INTEGER),
                        (requirement, value) -> requirement.xpRequired = value != null ? value : 1, requirement -> requirement.xpRequired).add()
                .append(new KeyedCodec<>("PreviousTitleId", Codec.STRING),
                        (requirement, value) -> requirement.previousTitleId = value != null ? value : "", requirement -> requirement.previousTitleId).add()
                .append(new KeyedCodec<>("KillNpcTypes", Codec.STRING_ARRAY),
                        (requirement, value) -> requirement.killNpcTypes = value != null ? value : new String[0], requirement -> requirement.killNpcTypes).add()
                .append(new KeyedCodec<>("XpPerKill", Codec.INTEGER),
                        (requirement, value) -> requirement.xpPerKill = value != null ? value : 1, requirement -> requirement.xpPerKill).add()
                .append(new KeyedCodec<>("RemovePrevious", Codec.BOOLEAN),
                        (requirement, value) -> requirement.removePrevious = value != null ? value : true, requirement -> requirement.removePrevious).add()
                .build();
    }
}
