package com.example.bfb.gui;

import java.awt.Color;

/**
 * Палитра для ClickGUI.
 * Синхронизируется с HudTheme и ThemeModule.
 */
public final class GuiTheme {

    private GuiTheme() {}

    // === базовые цвета (тёмная тема) ===
    public static int background() { return 0xF00E0E16; }
    public static int backgroundDarker() { return 0xF0080810; }
    public static int panel() { return 0xF0141420; }
    public static int panelLight() { return 0xF01C1C2C; }
    public static int border() { return 0x30FFFFFF; }
    public static int text() { return 0xFFF4F6FF; }
    public static int muted() { return 0xFF9AA0B4; }
    public static int disabled() { return 0xFF6A6A78; }

    // === accent (из HudTheme) ===
    public static int accent() {
        return com.example.bfb.hud.HudTheme.accent();
    }

    public static int accent2() {
        return com.example.bfb.hud.HudTheme.accent2();
    }

    public static int accentSoft() {
        return com.example.bfb.hud.HudTheme.accentSoft();
    }

    // === статусы ===
    public static int positive() { return 0xFF55FF85; }
    public static int warning() { return 0xFFFFC85F; }
    public static int negative() { return 0xFFFF5050; }

    // === тени ===
    public static int shadow() { return 0x40000000; }
    public static int shadowDeep() { return 0x70000000; }

    // === утилиты ===
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (((int) (aa + (ba - aa) * t)) << 24)
             | (((int) (ar + (br - ar) * t)) << 16)
             | (((int) (ag + (bg - ag) * t)) << 8)
             | ((int) (ab + (bb - ab) * t));
    }

    public static int darken(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, (int) (((color >> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, (int) (((color >> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, (int) ((color & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int color, float factor) {
        return darken(color, factor);
    }

    /** Преобразовать category color в tint с alpha. */
    public static int categoryColor(int alpha) {
        return withAlpha(0xFFF4F6FF, alpha);
    }

    // === пресеты color picker ===
    public static final int[] COLOR_PRESETS = {
            0xFFFFFF, 0x000000, 0xFF4D6D, 0xFF8A3D, 0xFFE14D,
            0x3DFF8A, 0x3DE1FF, 0x7C5CFF, 0xFF5CA8, 0xB14DFF,
            0x4CC3FF, 0x55FF85, 0xFFC85F, 0xFF5050, 0x9AA0B4
    };
}