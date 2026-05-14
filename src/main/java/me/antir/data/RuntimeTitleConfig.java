package me.antir.data;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted container for all admin-created runtime titles.
 * Stored at {@code admin-titles.json} in the plugin data directory (world save).
 */
public class RuntimeTitleConfig
{
    @Nonnull
    public List<RuntimeTitleData> titles = new ArrayList<>();

    public static final BuilderCodec<RuntimeTitleConfig> CODEC =
            BuilderCodec.builder(RuntimeTitleConfig.class, RuntimeTitleConfig::new)
                    .append(new KeyedCodec<>("Titles", new ArrayCodec<>(RuntimeTitleData.CODEC, RuntimeTitleData[]::new)),
                            (config, titleArray) -> config.titles = titleArray != null
                                    ? new ArrayList<>(List.of(titleArray))
                                    : new ArrayList<>(),
                            config -> config.titles.toArray(RuntimeTitleData[]::new)).add()
                    .build();
}
