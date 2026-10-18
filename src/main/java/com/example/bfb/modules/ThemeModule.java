package com.example.bfb.modules;

import com.example.bfb.BfbConfig;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ThemeModule extends CheatModule {
    public static ThemeModule instance;

    public final ModeSetting themePreset = add(new ModeSetting("Preset", "Default",
            "Default", "Neon", "Glass", "Dark", "Matrix", "Cyber", "Blood", "Custom"));

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

    // ---- Blur ----
    public final BooleanSetting menuBlur = add(new BooleanSetting("Menu blur", true));
    public final BooleanSetting worldBlur = add(new BooleanSetting("World blur", false));
    public final NumberSetting blurStrength = add(new NumberSetting("Blur strength", 4, 1, 8, 1));

    // ---- Gradient ----
    public final BooleanSetting gradientEnabled = add(new BooleanSetting("Gradient bg", true));
    public final ColorSetting gradientTop = add(new ColorSetting("Gradient top", 0x1A1A2E));
    public final ColorSetting gradientBottom = add(new ColorSetting("Gradient bottom", 0x0A0A12));

    // ---- Corner radius ----
    public final NumberSetting cornerRadius = add(new NumberSetting("Corner radius", 6, 0, 12, 1));

    // ---- Particles ----
    public final ModeSetting particles = add(new ModeSetting("Particles", "Snow",
            "Off", "Snow", "Rain", "Stars", "Bubbles", "Hearts"));
    public final NumberSetting particleCount = add(new NumberSetting("Particle count", 40, 10, 150, 5));
    public final NumberSetting particleSpeed = add(new NumberSetting("Particle speed", 1.0, 0.2, 3.0, 0.1));
    public final BooleanSetting particleGlow = add(new BooleanSetting("Particle glow", true));

    // ---- Animation ----
    public final BooleanSetting hoverAnim = add(new BooleanSetting("Hover animation", true));

    // === триггер смены пресета ===
    private String lastPreset = null;

    public ThemeModule() {
        super("Theme", "GUI theme, colors and particles", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
        instance = this;
    }

    public static ThemeModule get() {
        return instance;
    }

    @Override
    public void applyEnabled(boolean enabled) {
        super.applyEnabled(enabled);
        // НЕ применяем пресет автоматически — иначе сбрасываются
        // пользовательские цвета при каждом запуске
    }

    @Override
    public void onTick() {
        String now = themePreset.get();
        if (lastPreset == null) {
            // первый тик после загрузки — только запоминаем, не применяем
            lastPreset = now;
            return;
        }
        if (!now.equals(lastPreset)) {
            lastPreset = now;
            // пресет сменили вручную через GUI — применяем
            if (!now.equals("Custom")) {
                applyPreset(now);
            }
            BfbConfig.save();
        }
    }

    public void applyPreset(String preset) {
        switch (preset) {
            case "Neon" -> {
                bgColor.setRgb(0x0A0A12);
                headerColor.setRgb(0x1A1A2E);
                accentColor.setRgb(0xFF00FF);
                accentColor2.setRgb(0x00FFFF);
                textColor.setRgb(0xE0E0FF);
                mutedColor.setRgb(0x777799);
                onColor.setRgb(0x2A1A3A);
                gradientTop.setRgb(0x2A0A2E);
                gradientBottom.setRgb(0x0A0A12);
            }
            case "Glass" -> {
                bgColor.setRgb(0x1A1A2E);
                headerColor.setRgb(0x16213E);
                accentColor.setRgb(0x0F3460);
                accentColor2.setRgb(0x533483);
                textColor.setRgb(0xFFFFFF);
                mutedColor.setRgb(0xAAAAAA);
                onColor.setRgb(0x2A2A4A);
                gradientTop.setRgb(0x1A1A3E);
                gradientBottom.setRgb(0x0A0A12);
            }
            case "Dark" -> {
                bgColor.setRgb(0x08080A);
                headerColor.setRgb(0x121216);
                accentColor.setRgb(0x555566);
                accentColor2.setRgb(0x333344);
                textColor.setRgb(0xE0E0E0);
                mutedColor.setRgb(0x777777);
                onColor.setRgb(0x1A1A20);
                gradientTop.setRgb(0x151520);
                gradientBottom.setRgb(0x08080C);
            }
            case "Matrix" -> {
                bgColor.setRgb(0x000500);
                headerColor.setRgb(0x001A00);
                accentColor.setRgb(0x00FF00);
                accentColor2.setRgb(0x00CC00);
                textColor.setRgb(0x00FF00);
                mutedColor.setRgb(0x006600);
                onColor.setRgb(0x002A00);
                gradientTop.setRgb(0x002A00);
                gradientBottom.setRgb(0x000500);
            }
            case "Cyber" -> {
                bgColor.setRgb(0x0A0F14);
                headerColor.setRgb(0x141F2A);
                accentColor.setRgb(0xFF9900);
                accentColor2.setRgb(0x0099FF);
                textColor.setRgb(0xFFFFFF);
                mutedColor.setRgb(0x667788);
                onColor.setRgb(0x2A3A4A);
                gradientTop.setRgb(0x2A1A0F);
                gradientBottom.setRgb(0x0A0F14);
            }
            case "Blood" -> {
                bgColor.setRgb(0x1A0505);
                headerColor.setRgb(0x2A0A0A);
                accentColor.setRgb(0xFF0000);
                accentColor2.setRgb(0x990000);
                textColor.setRgb(0xFF3333);
                mutedColor.setRgb(0x663333);
                onColor.setRgb(0x3A1010);
                gradientTop.setRgb(0x3A0A0A);
                gradientBottom.setRgb(0x1A0505);
            }
            default -> {
                bgColor.setRgb(0x0C0C12);
                headerColor.setRgb(0x121220);
                accentColor.setRgb(0x7C5CFF);
                accentColor2.setRgb(0x4CC3FF);
                textColor.setRgb(0xF4F6FF);
                mutedColor.setRgb(0x9AA0B4);
                onColor.setRgb(0x1C1830);
                gradientTop.setRgb(0x1A1A2E);
                gradientBottom.setRgb(0x0A0A12);
            }
        }
        BfbConfig.save();
    }
}