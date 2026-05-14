package me.antir.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.requirement.ITitleRequirement;

import javax.annotation.Nonnull;

/**
 * Persisted holder for an admin-created runtime title.
 * Effects and requirements are encoded using their polymorphic codecs so that all
 * effect/requirement types survive a server restart.
 */
public class RuntimeTitleData
{
    public String id              = "";
    public String displayName     = "";
    public String description     = "";
    public String rarity          = "";
    public String tags            = "";
    public String uiAccentColor   = "";
    public ITitleEffect[]      passiveEffects = new ITitleEffect[0];
    public ITitleEffect[]      activeEffects  = new ITitleEffect[0];
    public ITitleRequirement[] requirements   = new ITitleRequirement[0];

    public static final BuilderCodec<RuntimeTitleData> CODEC;

    @Nonnull
    public Title toTitle()
    {
        String[] tagArray = tags.isEmpty() ? new String[0] : tags.split(",");
        return Title.create(id, displayName, description, rarity, tagArray, uiAccentColor,
                passiveEffects, activeEffects, requirements);
    }

    @Nonnull
    public static RuntimeTitleData from(@Nonnull Title title)
    {
        RuntimeTitleData data  = new RuntimeTitleData();
        data.id                = title.getId();
        data.displayName       = title.getDisplayName();
        data.description       = title.getDescription();
        data.rarity            = title.getRarityId();
        data.tags              = String.join(",", title.getTags());
        data.uiAccentColor     = title.getUiAccentColor();
        data.passiveEffects    = title.getPassiveEffects();
        data.activeEffects     = title.getActiveEffects();
        data.requirements      = title.getRequirements();
        return data;
    }

    static
    {
        CODEC =
                BuilderCodec.builder(RuntimeTitleData.class, RuntimeTitleData::new)
                        .append(new KeyedCodec<>("Id", Codec.STRING),
                                (titleData, value) -> titleData.id            = value != null ? value : "",
                                titleData -> titleData.id).add()
                        .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                                (titleData, value) -> titleData.displayName   = value != null ? value : "",
                                titleData -> titleData.displayName).add()
                        .append(new KeyedCodec<>("Description", Codec.STRING),
                                (titleData, value) -> titleData.description   = value != null ? value : "",
                                titleData -> titleData.description).add()
                        .append(new KeyedCodec<>("Rarity", Codec.STRING),
                                (titleData, value) -> titleData.rarity        = value != null ? value : "",
                                titleData -> titleData.rarity).add()
                        .append(new KeyedCodec<>("Tags", Codec.STRING),
                                (titleData, value) -> titleData.tags          = value != null ? value : "",
                                titleData -> titleData.tags).add()
                        .append(new KeyedCodec<>("UiAccentColor", Codec.STRING),
                                (titleData, value) -> titleData.uiAccentColor = value != null ? value : "",
                                titleData -> titleData.uiAccentColor).add()
                        .append(new KeyedCodec<>("PassiveEffects",
                                        new ArrayCodec<>(ITitleEffect.CODEC, ITitleEffect[]::new)),
                                (titleData, value) -> titleData.passiveEffects = value != null ? value : new ITitleEffect[0],
                                titleData -> titleData.passiveEffects).add()
                        .append(new KeyedCodec<>("ActiveEffects",
                                        new ArrayCodec<>(ITitleEffect.CODEC, ITitleEffect[]::new)),
                                (titleData, value) -> titleData.activeEffects  = value != null ? value : new ITitleEffect[0],
                                titleData -> titleData.activeEffects).add()
                        .append(new KeyedCodec<>("Requirements",
                                        new ArrayCodec<>(ITitleRequirement.CODEC, ITitleRequirement[]::new)),
                                (titleData, value) -> titleData.requirements   = value != null ? value : new ITitleRequirement[0],
                                titleData -> titleData.requirements).add()
                        .build();
    }
}
