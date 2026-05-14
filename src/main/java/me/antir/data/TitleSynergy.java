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
import me.antir.data.effect.ITitleEffect;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Synergy asset loaded from {@code Server/TitleSynergies/*.json}.
 *
 * <p>Activates when either all {@link #requiredTitles} are equipped, OR at least
 * {@link #minTaggedEquipped} equipped titles each carry all of {@link #requiredTags}.
 *
 * <p>On activation: {@link #bonusEffects} are applied and {@link #effectScales} amplify
 * matching {@link me.antir.data.effect.StatModifierEffect}s on equipped titles.
 * Both are reversed on deactivation.
 */
public class TitleSynergy implements JsonAssetWithMap<String, DefaultAssetMap<String, TitleSynergy>>, ITitle
{
    public static final AssetCodec<String, TitleSynergy> CODEC;

    private static AssetStore<String, TitleSynergy, DefaultAssetMap<String, TitleSynergy>> ASSET_STORE;

    private String id;
    AssetExtraInfo.Data data;

    private String displayName = "";
    private String[] requiredTitles = new String[0];
    private String[] requiredTags = new String[0];
    private int minTaggedEquipped = 2;
    private ITitleEffect[] bonusEffects = new ITitleEffect[0];
    private EffectScale[] effectScales = new EffectScale[0];

    public static AssetStore<String, TitleSynergy, DefaultAssetMap<String, TitleSynergy>> getAssetStore()
    {
        if (ASSET_STORE == null)
        {
            ASSET_STORE = AssetRegistry.getAssetStore(TitleSynergy.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, TitleSynergy> getAssetMap()
    {
        return (DefaultAssetMap<String, TitleSynergy>) getAssetStore().getAssetMap();
    }

    @Override public String getId() { return id; }
    @Override @Nonnull public String getDisplayName() { return displayName; }

    @Nonnull public String[] getRequiredTitles() { return requiredTitles; }
    @Nonnull public String[] getRequiredTags() { return requiredTags; }
    public int getMinTaggedEquipped() { return minTaggedEquipped; }
    @Nonnull public ITitleEffect[] getBonusEffects() { return bonusEffects; }
    @Nonnull public EffectScale[] getEffectScales() { return effectScales; }

    @Override @Nonnull public ITitleEffect[] getPassiveEffects() { return new ITitleEffect[0]; }
    @Override @Nonnull public ITitleEffect[] getActiveEffects() { return bonusEffects; }

    public boolean isConditionMet(List<String> equippedTitleIds)
    {
        if (requiredTitles.length > 0)
        {
            return new HashSet<>(equippedTitleIds).containsAll(Arrays.asList(requiredTitles));
        }
        if (requiredTags.length > 0)
        {
            List<String> tagList = Arrays.asList(requiredTags);
            long matching = equippedTitleIds.stream()
                    .map(equipped -> Title.getAssetMap().getAsset(equipped))
                    .filter(t -> t != null && new HashSet<>(Arrays.asList(t.getTags())).containsAll(tagList))
                    .count();
            return matching >= minTaggedEquipped;
        }
        return false;
    }

    static
    {
        CODEC = AssetBuilderCodec.builder(
                        TitleSynergy.class, TitleSynergy::new, Codec.STRING,
                        (synergy, id) -> synergy.id = id, synergy -> synergy.id,
                        (synergy, data) -> synergy.data = data, synergy -> synergy.data)
                .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                        (synergy, value) -> synergy.displayName = value != null ? value : "", synergy -> synergy.displayName).add()
                .append(new KeyedCodec<>("RequiredTitles", Codec.STRING_ARRAY),
                        (synergy, value) -> synergy.requiredTitles = value != null ? value : new String[0], synergy -> synergy.requiredTitles).add()
                .append(new KeyedCodec<>("RequiredTags", Codec.STRING_ARRAY),
                        (synergy, value) -> synergy.requiredTags = value != null ? value : new String[0], synergy -> synergy.requiredTags).add()
                .append(new KeyedCodec<>("MinTaggedEquipped", Codec.INTEGER),
                        (synergy, value) -> synergy.minTaggedEquipped = value != null ? value : 2, synergy -> synergy.minTaggedEquipped).add()
                .append(new KeyedCodec<>("BonusEffects", new ArrayCodec<>(ITitleEffect.CODEC, ITitleEffect[]::new)),
                        (synergy, value) -> synergy.bonusEffects = value != null ? value : new ITitleEffect[0], synergy -> synergy.bonusEffects).add()
                .append(new KeyedCodec<>("EffectScales", new ArrayCodec<>(EffectScale.CODEC, EffectScale[]::new)),
                        (synergy, value) -> synergy.effectScales = value != null ? value : new EffectScale[0], synergy -> synergy.effectScales).add()
                .build();
    }
}
