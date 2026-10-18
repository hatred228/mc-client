package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import com.example.bfb.setting.SettingGroup;
import org.lwjgl.glfw.GLFW;

public class VisualsModule extends CheatModule {
    public static VisualsModule instance;

    // ─── NAMETAGS ─────────────────────────────────────────────
    public final SettingGroup nametags = (SettingGroup) add(new SettingGroup("Nametags",
            new BooleanSetting("Enabled", true),
            new NumberSetting("Range", 48, 8, 128, 1),
            new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05),
            new ModeSetting("Style", "Lite", "Lite", "Box", "Minimal"),
            new BooleanSetting("Show health", true),
            new ModeSetting("Health style", "Hearts", "Hearts", "Numbers"),
            new BooleanSetting("Show armor", true),
            new BooleanSetting("Show enchant", true),
            new BooleanSetting("Show background", true),
            new BooleanSetting("Gradient text", true),
            new ColorSetting("Gradient top", 0x82D982),
            new ColorSetting("Gradient bottom", 0x4CC3FF),
            new ColorSetting("Background", 0x000000),
            new BooleanSetting("Distance fade", true),
            new BooleanSetting("Players", true),
            new BooleanSetting("Mobs", true),
            new BooleanSetting("Animals", true),
            new BooleanSetting("Invisible", false)
    ));

    public final SettingGroup tracers = (SettingGroup) add(new SettingGroup("Tracers",
            new BooleanSetting("Enabled", false),
            new NumberSetting("Range", 64, 8, 128, 1),
            new NumberSetting("Width", 1.5, 0.5, 4, 0.1),
            new ModeSetting("Origin", "Crosshair", "Crosshair", "Top", "Bottom"),
            new BooleanSetting("Gradient", true),
            new ColorSetting("Color from", 0x9C5CFF),
            new ColorSetting("Color to", 0x4CC3FF),
            new BooleanSetting("Players", true),
            new BooleanSetting("Mobs", true),
            new BooleanSetting("Animals", false)
    ));

    public final SettingGroup watermark = (SettingGroup) add(new SettingGroup("Watermark",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Show user", true),
            new BooleanSetting("Show fps", true),
            new BooleanSetting("Show ping", true),
            new ColorSetting("Accent", 0x7C5CFF),
            new ColorSetting("Background", 0x0E0E16)
    ));

    public final SettingGroup potions = (SettingGroup) add(new SettingGroup("Potions",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Header", true),
            new BooleanSetting("Icon", true),
            new BooleanSetting("Show name", true),
            new BooleanSetting("Show level", true),
            new BooleanSetting("Show duration", true),
            new ColorSetting("Background", 0x0E0E16)
    ));

    public final SettingGroup cooldowns = (SettingGroup) add(new SettingGroup("Cooldowns",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Header", true),
            new BooleanSetting("Icon", true),
            new BooleanSetting("Show name", true),
            new BooleanSetting("Show duration", true)
    ));

    public final SettingGroup keybinds = (SettingGroup) add(new SettingGroup("Keybinds",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Header", true),
            new BooleanSetting("Show name", true),
            new BooleanSetting("Show key", true),
            new BooleanSetting("Only enabled", false)
    ));

    public final SettingGroup targetHud = (SettingGroup) add(new SettingGroup("TargetHud",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Show armor", true),
            new BooleanSetting("Show health", true),
            new BooleanSetting("Show hand", true),
            new BooleanSetting("Show hotbar", true),
            new BooleanSetting("Show distance", true),
            new ColorSetting("Background", 0x0E0E16)
    ));

    public final SettingGroup armorHud = (SettingGroup) add(new SettingGroup("Armor",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Show durability", true),
            new ColorSetting("Background", 0x000000)
    ));

    public final SettingGroup notifications = (SettingGroup) add(new SettingGroup("Notifications",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Show toggle", true),
            new BooleanSetting("Show warnings", true),
            new BooleanSetting("Show server alerts", true),
            new BooleanSetting("Show player events", true),
            new NumberSetting("Lifetime", 3.0, 1.0, 10.0, 0.5),
            new ModeSetting("Animation", "Slide", "Slide", "Fade", "Pop", "None"),
            new ModeSetting("Position", "TopRight", "TopRight", "TopLeft", "BottomRight", "BottomLeft"),
            new NumberSetting("Anim speed", 1.0, 0.3, 4.0, 0.1),
            new NumberSetting("Stack gap", 3, 1, 10, 1),
            new NumberSetting("Width", 170, 100, 300, 5),
            new BooleanSetting("Accent bar", true),
            new BooleanSetting("Slide from side", true),
            new ColorSetting("Toggle on color", 0x55FF55),
            new ColorSetting("Toggle off color", 0x888888),
            new ColorSetting("Totem pop color", 0xFFC85F),
            new ColorSetting("Shield break color", 0xFFFFFF),
            new ColorSetting("Helmet break color", 0xB0B5C0),
            new ColorSetting("Totem swap color", 0x9AA0B4),
            new ColorSetting("Potion drink color", 0xB14DFF),
            new ColorSetting("Critical HP color", 0xFF5050)
    ));

    public final SettingGroup staff = (SettingGroup) add(new SettingGroup("StaffDetector",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Header", true),
            new BooleanSetting("Show mode", true),
            new BooleanSetting("Spectators", true),
            new BooleanSetting("Role prefixes", true),
            new BooleanSetting("Include self", false),
            new ModeSetting("Position", "TopLeft",
                    "TopLeft", "TopRight", "BottomLeft", "BottomRight")
    ));

    public final SettingGroup arraylist = (SettingGroup) add(new SettingGroup("ArrayList",
            new BooleanSetting("Enabled", true),
            new BooleanSetting("Show icon", false),
            new BooleanSetting("Accent bar", true),
            new BooleanSetting("Rainbow", false),
            new NumberSetting("Line height", 10, 8, 20, 1)
    ));

    public VisualsModule() {
        super("Visuals", "HUD widget settings", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
        instance = this;
    }

    public static VisualsModule get() { return instance; }
}