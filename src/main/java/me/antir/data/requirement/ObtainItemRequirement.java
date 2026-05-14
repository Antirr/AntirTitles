package me.antir.data.requirement;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Satisfied when a player obtains {@code count} items with the given {@code itemId}.
 * Trigger via {@link me.antir.systems.TitleItemCheck#onItemObtained}.
 *
 * <p>JSON: {@code {"Type":"ObtainItem","ItemId":"Dragon_Scale","Count":1}}
 */
public class ObtainItemRequirement implements ITitleRequirement
{
    @Nonnull
    public static final BuilderCodec<ObtainItemRequirement> CODEC;

    private String itemId = "";
    private int count = 1;

    public String getItemId() { return itemId; }

    @Override
    public int getTarget() { return count; }

    @Override
    public String getDescription() { return "Obtain " + count + "x " + itemId; }

    public static ObtainItemRequirement create(@Nonnull String itemId, int count)
    {
        ObtainItemRequirement req = new ObtainItemRequirement();
        req.itemId = itemId;
        req.count  = count;
        return req;
    }

    @Override
    public String progressKey(String titleId, int requirementIndex)
    {
        return titleId + ":" + requirementIndex;
    }

    static
    {
        CODEC = BuilderCodec.builder(ObtainItemRequirement.class, ObtainItemRequirement::new)
                .append(new KeyedCodec<>("ItemId", Codec.STRING),
                        (requirement, value) -> requirement.itemId = value != null ? value : "", requirement -> requirement.itemId).add()
                .append(new KeyedCodec<>("Count", Codec.INTEGER),
                        (requirement, value) -> requirement.count = value != null ? value : 1, requirement -> requirement.count).add()
                .build();
    }
}
