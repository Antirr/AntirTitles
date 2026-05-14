package me.antir.data.requirement;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Satisfied when a player accumulates {@code count} events of the given {@code activity} key.
 * Trigger via {@link me.antir.systems.TitleActivityCheck#onActivity}.
 *
 * <p>Activity keys are arbitrary strings agreed upon between the requirement and the caller —
 * e.g. {@code "FishCaught"}, {@code "OreMined"}, {@code "ItemCrafted"}, {@code "ZoneDiscovered"}.
 *
 * <p>JSON: {@code {"Type":"ActivityCount","Activity":"FishCaught","Count":50}}
 */
public class ActivityCountRequirement implements ITitleRequirement
{
    @Nonnull
    public static final BuilderCodec<ActivityCountRequirement> CODEC;

    private String activity = "";
    private int count = 1;

    public String getActivity() { return activity; }

    @Override
    public int getTarget() { return count; }

    @Override
    public String getDescription() { return count + "x " + activity; }

    public static ActivityCountRequirement create(@Nonnull String activity, int count)
    {
        ActivityCountRequirement req = new ActivityCountRequirement();
        req.activity = activity;
        req.count    = count;
        return req;
    }

    @Override
    public String progressKey(String titleId, int requirementIndex)
    {
        return titleId + ":" + requirementIndex;
    }

    static
    {
        CODEC = BuilderCodec.builder(ActivityCountRequirement.class, ActivityCountRequirement::new)
                .append(new KeyedCodec<>("Activity", Codec.STRING),
                        (requirement, value) -> requirement.activity = value != null ? value : "", requirement -> requirement.activity).add()
                .append(new KeyedCodec<>("Count", Codec.INTEGER),
                        (requirement, value) -> requirement.count = value != null ? value : 1, requirement -> requirement.count).add()
                .build();
    }
}
