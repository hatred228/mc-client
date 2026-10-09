package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ThemeModule extends CheatModule {
    public static ThemeModule instance;

    // ---- Colors ----
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0x0C0C12));
    public final ColorSetting headerColor = add(new ColorSetting("Header", 0x121220));
    public final ColorSetting accentColor = add(new ColorSetting("Accent 1", 0x7C5CFF));
    public final ColorSetting accentColor2 = add(new ColorSetting("Accent 2", 0x4CC3FF));
    public final ColorSetting textColor = add(new ColorSetting("Text", 0xF4F6FF));
    public final ColorSetting mutedColor = add(new ColorSetting("Muted", 0x9AA0B4));
    public final ColorSetting onColor = add(new ColorSetting("Enabled BG", 0x1C1830));

    // ---- Transparency ----
    public final NumberSetting menuAlpha = add(new NumberSetting("Menu alpha", 220, 0, 255, 5));
    public final NumberSetting bgAlpha = add(new NumberSetting("HUD alpha", 220, 0, 255, 5));

    // ---- Particles ----
    public final ModeSetting particles = add(new ModeSetting("Particles", "Snow",
            "Off", "Snow", "Rain", "Stars", "Bubbles", "Hearts"));
    public final NumberSetting particleCount = add(new NumberSetting("Particle count", 40, 10, 150, 5));
    public final NumberSetting particleSpeed = add(new NumberSetting("Particle speed", 1.0, 0.2, 3.0, 0.1));
    public final BooleanSetting particleGlow = add(new BooleanSetting("Particle glow", true));

    // ---- Animation ----
    public final BooleanSetting hoverAnim = add(new BooleanSetting("Hover animation", true));

    public ThemeModule() {
        super("Theme", "GUI theme, colors and particles", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
        instance = this;
    }

    public static ThemeModule get() {
        return instance;
    }
}