package me.antir.data;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.requirement.ITitleRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Title implements JsonAssetWithMap<String, DefaultAssetMap<String, Title>>, ITitle
{
    public static final AssetCodec<String, Title> CODEC;

    private static AssetStore<String, Title, DefaultAssetMap<String, Title>> ASSET_STORE;

    private String id;
    AssetExtraInfo.Data data;

    private String displayName   = "";
    private String description   = "";
    private String rarity        = "";
    private String uiAccentColor = "";
    private String[] titleTags   = new String[0];
    private ITitleEffect[] active         = new ITitleEffect[0];
    private ITitleEffect[] passive        = new ITitleEffect[0];
    private ITitleRequirement[] requirements = new ITitleRequirement[0];

    public static AssetStore<String, Title, DefaultAssetMap<String, Title>> getAssetStore()
    {
        if (ASSET_STORE == null)
        {
            ASSET_STORE = AssetRegistry.getAssetStore(Title.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, Title> getAssetMap()
    {
        return (DefaultAssetMap<String, Title>) getAssetStore().getAssetMap();
    }

    @Override
    public String getId() { return id; }

    @Override
    @Nonnull
    public String getDisplayName() { return displayName; }

    @Nonnull
    public String getDescription() { return description; }

    /** Returns the {@link ItemQuality} asset for this title's rarity, or {@code null} if not set or not found. */
    @Nullable
    public ItemQuality getRarity()
    {
        if (rarity == null || rarity.isEmpty()) return null;
        return ItemQuality.getAssetMap().getAsset(rarity);
    }

    /** Returns the raw rarity ID string (e.g. {@code "Legendary"}), or empty if none. */
    @Nonnull
    public String getRarityId() { return rarity; }

    /**
     * Optional per-title UI accent color override as a hex string (e.g. {@code "#bb8a2c"}).
     * Empty string means "derive from rarity".
     */
    @Nonnull
    public String getUiAccentColor() { return uiAccentColor; }

    @Nonnull
    public String[] getTags() { return titleTags; }

    @Override
    @Nonnull
    public ITitleEffect[] getActiveEffects() { return active; }

    @Override
    @Nonnull
    public ITitleEffect[] getPassiveEffects() { return passive; }

    @Nonnull
    public ITitleRequirement[] getRequirements() { return requirements; }

    static
    {
        CODEC = AssetBuilderCodec.builder(
                        Title.class, Title::new, Codec.STRING,
                        (title, id) -> title.id = id, title -> title.id,
                        (title, data) -> title.data = data, title -> title.data)
                .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                        (title, value) -> title.displayName = value != null ? value : "", title -> title.displayName).add()
                .append(new KeyedCodec<>("Description", Codec.STRING),
                        (title, value) -> title.description = value != null ? value : "", title -> title.description).add()
                .append(new KeyedCodec<>("Rarity", Codec.STRING),
                        (title, value) -> title.rarity = value != null ? value : "", title -> title.rarity).add()
                .append(new KeyedCodec<>("UiAccentColor", Codec.STRING),
                        (title, value) -> title.uiAccentColor = value != null ? value : "", title -> title.uiAccentColor).add()
                .append(new KeyedCodec<>("TitleTags", Codec.STRING_ARRAY),
                        (title, value) -> title.titleTags = value != null ? value : new String[0], title -> title.titleTags).add()
                .append(new KeyedCodec<>("ActiveEffects", new ArrayCodec<>(ITitleEffect.CODEC, ITitleEffect[]::new)),
                        (title, value) -> title.active = value != null ? value : new ITitleEffect[0], title -> title.active).add()
                .append(new KeyedCodec<>("PassiveEffects", new ArrayCodec<>(ITitleEffect.CODEC, ITitleEffect[]::new)),
                        (title, value) -> title.passive = value != null ? value : new ITitleEffect[0], title -> title.passive).add()
                .append(new KeyedCodec<>("Requirements", new ArrayCodec<>(ITitleRequirement.CODEC, ITitleRequirement[]::new)),
                        (title, value) -> title.requirements = value != null ? value : new ITitleRequirement[0], title -> title.requirements).add()
                .build();
    }

    /** Creates a Title programmatically for the admin UI. */
    @Nonnull
    public static Title create(@Nonnull String titleId, @Nonnull String titleDisplayName,
                               @Nonnull String titleDescription, @Nonnull String rarityId,
                               @Nonnull String[] tags, @Nonnull String accentColor,
                               @Nonnull ITitleEffect[] passiveEffects,
                               @Nonnull ITitleEffect[] activeEffects,
                               @Nonnull ITitleRequirement[] requirements)
    {
        Title title         = new Title();
        title.id            = titleId;
        title.displayName   = titleDisplayName;
        title.description   = titleDescription;
        title.rarity        = rarityId;
        title.titleTags     = tags;
        title.uiAccentColor = accentColor;
        title.passive       = passiveEffects;
        title.active        = activeEffects;
        title.requirements  = requirements;
        return title;
    }
}
