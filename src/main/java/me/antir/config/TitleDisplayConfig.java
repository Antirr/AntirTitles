package me.antir.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Controls where equipped titles are displayed.
 * Stored at {@code title-display.json} in the plugin data directory.
 *
 * <p>Format strings support {@code {title}} and {@code {name}} placeholders.
 */
public class TitleDisplayConfig
{
    @Nonnull
    public static final BuilderCodec<TitleDisplayConfig> CODEC = BuilderCodec.builder(TitleDisplayConfig.class, TitleDisplayConfig::new)
            .append(new KeyedCodec<>("ShowInNameplate", Codec.BOOLEAN),
                    (cfg, value) -> cfg.showInNameplate = Boolean.TRUE.equals(value), cfg -> cfg.showInNameplate).add()
            .append(new KeyedCodec<>("ShowInChat", Codec.BOOLEAN),
                    (cfg, value) -> cfg.showInChat = Boolean.TRUE.equals(value), cfg -> cfg.showInChat).add()
            .append(new KeyedCodec<>("NameplateFormat", Codec.STRING),
                    (cfg, value) -> cfg.nameplateFormat = value != null ? value : "{title}\n{name}",
                    cfg -> cfg.nameplateFormat).add()
            .append(new KeyedCodec<>("ChatPrefixFormat", Codec.STRING),
                    (cfg, value) -> cfg.chatPrefixFormat = value != null ? value : "[{title}] ",
                    cfg -> cfg.chatPrefixFormat).add()
            .append(new KeyedCodec<>("DefaultMaxEquippedTitles", Codec.INTEGER),
                    (cfg, value) -> cfg.defaultMaxEquippedTitles = value != null ? Math.max(1, value) : 1,
                    cfg -> cfg.defaultMaxEquippedTitles).add()
            .build();

    private boolean showInNameplate = true;
    private boolean showInChat = true;

    /**
     * Nameplate text format. Defaults to {@code "{title}\n{name}"} which places the
     * title one line above the player name — swap to {@code "{name}\n{title}"} to
     * reverse, or {@code "{name} [{title}]"} for a single-line layout.
     */
    private int defaultMaxEquippedTitles = 1;
    private String nameplateFormat = "{title}\n{name}";

    /**
     * Chat prefix injected before the player's username. Defaults to {@code "[{title}] "}.
     * Set to an empty string to disable without turning off the feature globally.
     */
    private String chatPrefixFormat = "[{title}] ";

    public boolean isShowInNameplate() { return showInNameplate; }
    public boolean isShowInChat() { return showInChat; }
    public int getDefaultMaxEquippedTitles() { return defaultMaxEquippedTitles; }

    @Nonnull
    public String formatNameplate(@Nonnull String titleName, @Nonnull String playerName)
    {
        return nameplateFormat.replace("{title}", titleName).replace("{name}", playerName);
    }

    @Nonnull
    public String formatChatPrefix(@Nonnull String titleName)
    {
        return chatPrefixFormat.replace("{title}", titleName);
    }
}
