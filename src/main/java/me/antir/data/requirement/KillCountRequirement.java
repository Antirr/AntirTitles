package me.antir.data.requirement;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Satisfied when a player kills {@code count} NPCs of type {@code npcType}.
 *
 * <p>JSON example:
 * <pre>{@code {"Type": "KillCount", "NpcType": "Wolf_Black", "Count": 100}}</pre>
 */
public class KillCountRequirement implements ITitleRequirement
{
    @Nonnull
    public static final BuilderCodec<KillCountRequirement> CODEC;

    private String npcType = "";
    private int count = 1;

    public String getNpcType() { return npcType; }

    @Override
    public int getTarget() { return count; }

    @Override
    public String getDescription() { return "Kill " + count + "x " + npcType; }

    public static KillCountRequirement create(@Nonnull String npcType, int count)
    {
        KillCountRequirement req = new KillCountRequirement();
        req.npcType = npcType;
        req.count   = count;
        return req;
    }

    @Override
    public String progressKey(String titleId, int requirementIndex)
    {
        return titleId + ":" + requirementIndex;
    }

    static
    {
        CODEC = BuilderCodec.builder(KillCountRequirement.class, KillCountRequirement::new)
                .append(new KeyedCodec<>("NpcType", Codec.STRING),
                        (req, value) -> req.npcType = value != null ? value : "", req -> req.npcType).add()
                .append(new KeyedCodec<>("Count", Codec.INTEGER),
                        (req, value) -> req.count = value != null ? value : 1, req -> req.count).add()
                .build();
    }
}
