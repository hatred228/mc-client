package com.example.bfb.hud;

import java.util.List;

/**
 * Централизованная палитра HUD.
 *
 * Использование:
 *   int bg = HudTheme.background();
 *   int accent = HudTheme.accent();
 *   int tinted = HudTheme.withAlpha(accent, 128);
 *
 * Тёмная/светлая тема + accent preset.
 * Значения синхронизируются с ThemeModule игрока.
 */
public final class HudTheme {

    // ============================================================
    // PRESETS
    // ============================================================

    public record Preset(String name, String description, int color) {}

    public static final List<Preset> PRESETS = List.of(
            new Preset("Violet", "Signature violet accent", 0xFF9C5CFF),
            new Preset("Pink", "Warm pink accent", 0xFFFF5CA8),
            new Preset("Azure", "Cool blue accent", 0xFF4CC3FF),
            new Preset("Emerald", "Fresh green accent", 0xFF3DFFB0),
            new Preset("Gold", "Premium gold accent", 0xFFFFC14D),
            new Preset("Crimson", "Bold red accent", 0xFFFF5A6A),
            new Preset("Mint", "Soft mint accent", 0xFF82F5D8),
            new Preset("Amethyst", "Deep purple accent", 0xFFB14DFF),
            new Preset("Coral", "Warm coral accent", 0xFFFF8464),
            new Preset("Ice", "Cold ice accent", 0xFFB8E7FF),
            new Preset("Toxic", "Bright toxic green", 0xFF9FFF3D),
            new Preset("Magma", "Lava red-orange", 0xFFFF6A2C),
            new Preset("Ocean", "Deep ocean blue", 0xFF2C7EFF),
            new Preset("Rose", "Dusty rose", 0xFFE8A0B8),
            new Preset("Sand", "Warm desert sand", 0xFFE8C890),
            new Preset("Mono", "Neutral gray", 0xFFAAAAAA)
    );

    // ============================================================
    // СОСТОЯНИЕ
    // ============================================================

    private static boolean light = false;
    private static int accentColor = 0xFF9C5CFF;

    private HudTheme() {}

    public static void setLight(boolean value) {
        light = value;
    }

    public static void setPresetIndex(int index) {
        if (index < 0 || index >= PRESETS.size()) index = 0;
        accentColor = PRESETS.get(index).color();
    }

    public static void setPreset(String name) {
        for (int i = 0; i < PRESETS.size(); i++) {
            if (PRESETS.get(i).name().equalsIgnoreCase(name)) {
                setPresetIndex(i);
                return;
            }
        }
    }

    public static void setAccent(int argb) {
        accentColor = 0xFF000000 | (argb & 0xFFFFFF);
    }

    // ============================================================
    // БАЗОВЫЕ ЦВЕТА
    // ============================================================

    public static boolean light() {
        return light;
    }

    public static int background() {
        return light ? 0xE8F2F2F7 : 0xE8100E14;
    }

    public static int backgroundDark() {
        return light ? 0xD8E8E8EE : 0xD80A0810;
    }

    public static int text() {
        return light ? 0xFF161619 : 0xFFF4F6FF;
    }

    public static int muted() {
        return light ? 0xFF8D8D96 : 0xFF969299;
    }

    public static int border() {
        return light ? 0xFFD0D0D8 : 0xFF2A2A38;
    }

    public static int track() {
        return light ? 0xFFE0E0E8 : 0xFF23242E;
    }

    public static int accent() {
        return accentColor;
    }

    public static int accent2() {
        int a = (accentColor >>> 24) & 0xFF;
        int r = (accentColor >> 16) & 0xFF;
        int g = (accentColor >> 8) & 0xFF;
        int b = accentColor & 0xFF;

        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        float newHue = (hsb[0] + 40f / 360f) % 1f;
        int rgb = java.awt.Color.HSBtoRGB(newHue, hsb[1], hsb[2]);

        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int accentSoft() {
        int a = (accentColor >>> 24) & 0xFF;
        int r = (accentColor >> 16) & 0xFF;
        int g = (accentColor >> 8) & 0xFF;
        int b = accentColor & 0xFF;
        int bg = background() & 0xFFFFFF;
        int bgr = (bg >> 16) & 0xFF;
        int bgg = (bg >> 8) & 0xFF;
        int bgb = bg & 0xFF;
        int nr = (r * 30 + bgr * 70) / 100;
        int ng = (g * 30 + bgg * 70) / 100;
        int nb = (b * 30 + bgb * 70) / 100;
        return (a << 24) | (nr << 16) | (ng << 8) | nb;
    }

    public static int positive() {
        return 0xFF82FF82;
    }

    public static int middle() {
        return 0xFFFFC85F;
    }

    public static int negative() {
        return 0xFFFF5050;
    }

    // ============================================================
    // УТИЛИТЫ
    // ============================================================

    /** Применить alpha к RGB (создать ARGB). */
    public static int alpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    /** Изменить alpha существующего ARGB цвета. */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    /** Линейная интерполяция двух ARGB цветов. */
    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    /** Затемнить/осветлить цвет. factor < 1.0 = темнее. */
    public static int scale(int rgb, float factor) {
        int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, (int) ((rgb & 0xFF) * factor)));
        return (rgb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}