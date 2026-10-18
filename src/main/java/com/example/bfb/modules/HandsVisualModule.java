package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class HandsVisualModule extends CheatModule {

    public final ModeSetting mode = add(new ModeSetting("Mode", "Rainbow",
            "Rainbow", "Gradient", "Pulse", "Fire", "Ice", "Health"));

    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0f, 0.2f, 4.0f, 0.1f));
    public final NumberSetting intensity = add(new NumberSetting("Intensity", 1.0f, 0.2f, 1.0f, 0.05f));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 200, 30, 255, 5));

    public final ColorSetting color1 = add(new ColorSetting("Color 1", 0x9C5CFF));
    public final ColorSetting color2 = add(new ColorSetting("Color 2", 0xFF5CA8));

    public final BooleanSetting onlyMainHand = add(new BooleanSetting("Only main hand", false));

    public HandsVisualModule() {
        super("HandsVisual", "Re-colored items in your hands", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
    }

    public int tintColor() {
        long now = System.currentTimeMillis();
        float t = (now % 4000L) / 4000f * speed.getFloat();
        int a = (int) (opacity.getInt() * intensity.getFloat());

        switch (mode.get()) {
            case "Gradient": {
                int c = RenderUtil.lerpRgb(color1.getRgb(), color2.getRgb(),
                        (float)((Math.sin(t * Math.PI * 2) + 1) * 0.5));
                return (a << 24) | (c & 0xFFFFFF);
            }
            case "Pulse": {
                float p = (float)((Math.sin(t * Math.PI * 2) + 1) * 0.5);
                float f = 0.5f + 0.5f * p;
                int c = color1.getRgb();
                int r = (int)(((c >> 16) & 0xFF) * f);
                int g = (int)(((c >> 8) & 0xFF) * f);
                int b = (int)((c & 0xFF) * f);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
            case "Fire": {
                float f = 0.5f + 0.5f * (float)Math.sin(now / 200.0);
                int c = RenderUtil.lerpRgb(0xFF6A00, 0xFFFFEE, f);
                return (a << 24) | (c & 0xFFFFFF);
            }
            case "Ice": {
                float f = 0.5f + 0.5f * (float)Math.sin(now / 300.0);
                int c = RenderUtil.lerpRgb(0x4CC3FF, 0xFFFFFF, f);
                return (a << 24) | (c & 0xFFFFFF);
            }
            case "Health": {
                var player = com.example.bfb.BfbMod.getClient().player;
                if (player == null) return (a << 24) | 0x55FF55;
                float hp = player.getHealth() / Math.max(1f, player.getMaxHealth());
                int red = (int)((1f - hp) * 255);
                int green = (int)(hp * 255);
                return (a << 24) | (red << 16) | (green << 8);
            }
            default: {
                float hue = (now % 4000L) / 4000f * speed.getFloat();
                hue = hue - (float)Math.floor(hue);
                int c = RenderUtil.hsbToRgb(hue, 0.75f, 1f);
                return (a << 24) | (c & 0xFFFFFF);
            }
        }
    }

    public boolean shouldTint(boolean offHand) {
        if (!isEnabled()) return false;
        return !(onlyMainHand.get() && offHand);
    }
}