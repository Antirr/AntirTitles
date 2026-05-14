package me.antir.ui;

import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;

public final class ColorUtil
{
    private static final String DEFAULT_ACCENT = "#4a6a8a";

    private ColorUtil() {}

    /** Converts a protocol Color (3-byte R G B) to an AWT Color for use in Message.color(). */
    @Nonnull
    public static Color toAwtColor(@Nonnull com.hypixel.hytale.protocol.Color color)
    {
        return new Color(color.red & 0xFF, color.green & 0xFF, color.blue & 0xFF);
    }

    /** Returns #RRGGBB hex string from a protocol Color. */
    @Nonnull
    public static String toHexString(@Nonnull com.hypixel.hytale.protocol.Color color)
    {
        return String.format("#%02x%02x%02x", color.red & 0xFF, color.green & 0xFF, color.blue & 0xFF);
    }

    /**
     * Resolves the UI accent hex color for a quality:
     * quality.getTextColor() → hex, or the default neutral if null.
     */
    @Nonnull
    public static String accentHex(@Nullable ItemQuality quality)
    {
        if (quality == null) return DEFAULT_ACCENT;
        com.hypixel.hytale.protocol.Color qualityColor = quality.getTextColor();
        if (qualityColor == null) return DEFAULT_ACCENT;
        return toHexString(qualityColor);
    }

    /** Returns AWT Color from an ItemQuality, or the neutral accent if null. */
    @Nonnull
    public static Color accentAwtColor(@Nullable ItemQuality quality)
    {
        if (quality == null) return new Color(0x4a, 0x6a, 0x8a);
        com.hypixel.hytale.protocol.Color pc = quality.getTextColor();
        if (pc == null) return new Color(0x4a, 0x6a, 0x8a);
        return toAwtColor(pc);
    }

    /** Builds an inline .ui document for a solid-color fill group. */
    @Nonnull
    public static String colorFillDoc(@Nonnull String hexColor)
    {
        return "Group { Background: (Color: " + hexColor + "); Anchor: (Full: 0); }";
    }
}
