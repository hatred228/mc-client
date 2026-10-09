package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class CustomVisualsModule extends CheatModule {
    public final ModeSetting timeOfDay = add(new ModeSetting("Time", "Default", "Default", "Day", "Noon", "Sunset", "Night", "Midnight"));
    public final BooleanSetting customSky = add(new BooleanSetting("Custom sky", false));
    public final ColorSetting skyColor = add(new ColorSetting("Sky color", 0x87CEEB));
    public final BooleanSetting customCrosshair = add(new BooleanSetting("Custom crosshair", false));
    public final ColorSetting crosshairColor = add(new ColorSetting("Crosshair color", 0x00FF00));
    public final NumberSetting crosshairLength = add(new NumberSetting("Crosshair length", 6, 1, 15, 1));
    public final NumberSetting crosshairGap = add(new NumberSetting("Crosshair gap", 3, 0, 10, 1));
    public final NumberSetting crosshairWidth = add(new NumberSetting("Crosshair width", 2, 1, 4, 1));
    public final BooleanSetting crosshairDot = add(new BooleanSetting("Crosshair dot", true));
    public final NumberSetting crosshairDotSize = add(new NumberSetting("Dot size", 2, 1, 5, 1));

    public CustomVisualsModule() {
        super("CustomVisuals", "Custom sky, time, crosshair", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (client.world == null) return;
        String t = timeOfDay.get();
        long time = switch (t) {
            case "Day" -> 1000L;
            case "Noon" -> 6000L;
            case "Sunset" -> 12800L;
            case "Night" -> 14000L;
            case "Midnight" -> 18000L;
            default -> -1L;
        };
        if (time >= 0) {
            // в 1.21.11 метод живёт на WorldProperties, не на ClientWorld
            client.world.getLevelProperties().setTimeOfDay(time);
        }
    }
}