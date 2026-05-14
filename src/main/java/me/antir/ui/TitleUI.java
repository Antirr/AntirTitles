package me.antir.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import me.antir.data.ITitle;
import me.antir.data.Title;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Central hub for all title-related player UI.
 *
 * <p><b>Grant</b> — zone-discovery style full-screen overlay via {@link EventTitleUtil}.
 * <br><b>Equip / Unequip / Remove</b> — toast notifications via {@link NotificationUtil}.
 * <br><b>Error</b> — short danger toast for validation failures.
 */
public final class TitleUI
{
    private static final float GRANT_DURATION    = 5.5f;
    private static final float GRANT_FADE_IN     = 1.0f;
    private static final float GRANT_FADE_OUT    = 2.0f;

    private TitleUI() {}

    // ── Grant / Remove ────────────────────────────────────────────────────────

    /**
     * Zone-discovery style overlay when a new title is granted.
     * Uses {@link EventTitleUtil} so the primary title is the title name,
     * shown large and centre-screen just like an area discovery.
     */
    public static void showGranted(@Nonnull PlayerRef playerRef, @Nonnull ITitle title)
    {
        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw(title.getDisplayName()),
                Message.translation("server.ui.titles.earned"),
                true,
                getRarityIcon(title),
                GRANT_DURATION,
                GRANT_FADE_IN,
                GRANT_FADE_OUT
        );
    }

    /** Toast when a title is removed from the player's collection. */
    public static void showRemoved(@Nonnull PlayerRef playerRef, @Nonnull ITitle title)
    {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.raw(title.getDisplayName()),
                Message.translation("server.ui.titles.removed"),
                getRarityIcon(title),
                null,
                NotificationStyle.Warning
        );
    }

    // ── Equip / Unequip ───────────────────────────────────────────────────────

    /** Success toast when a title is equipped. */
    public static void showEquipped(@Nonnull PlayerRef playerRef, @Nonnull ITitle title)
    {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.raw(title.getDisplayName()),
                Message.translation("server.ui.titles.equipped"),
                getRarityIcon(title),
                null,
                NotificationStyle.Success
        );
    }

    /** Neutral toast when a title is unequipped. */
    public static void showUnequipped(@Nonnull PlayerRef playerRef, @Nonnull ITitle title)
    {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.raw(title.getDisplayName()),
                Message.translation("server.ui.titles.unequipped"),
                getRarityIcon(title),
                null,
                NotificationStyle.Default
        );
    }

    // ── Synergy ───────────────────────────────────────────────────────────────

    /** Small notification when a synergy activates. */
    public static void showSynergyActivated(@Nonnull PlayerRef playerRef, @Nonnull String synergyName)
    {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.raw(synergyName),
                Message.translation("server.ui.titles.synergyActivated"),
                null,
                null,
                NotificationStyle.Success
        );
    }

    // ── Errors ────────────────────────────────────────────────────────────────

    /** Short danger notification for validation failures (slot full, not learned, etc.). */
    public static void showError(@Nonnull PlayerRef playerRef, @Nonnull Message message)
    {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                message,
                null,
                null,
                null,
                NotificationStyle.Danger
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the {@link ItemQuality#getItemTooltipTexture()} for the title's rarity,
     * used as a themed icon in notifications and event titles.
     */
    @Nullable
    public static String getRarityIcon(@Nonnull ITitle title)
    {
        if (title instanceof Title t)
        {
            ItemQuality quality = t.getRarity();
            return quality != null ? quality.getItemTooltipTexture() : null;
        }
        return null;
    }
}
