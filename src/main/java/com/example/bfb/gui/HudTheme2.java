package com.example.bfb.gui;

public final class HudTheme2 {

    private HudTheme2() {}

    // panel
    public static int panelBg() { return 0xF00E0E16; }
    public static int panelBorder() { return 0x22FFFFFF; }
    public static int topBar() { return 0x40000000; }

    // text
    public static int text() { return 0xFFF4F6FF; }
    public static int muted() { return 0xFFB0B5C0; }
    public static int dim() { return 0xFF888D9A; }

    // accent — берём из HudTheme
    public static int accent() {
        return com.example.bfb.hud.HudTheme.accent();
    }

    public static int accentDim() {
        return 0xFF5C3A9C;
    }
}