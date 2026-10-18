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
    public final ModeSetting timeOfDay = add(new ModeSetting("Time", "Default",
            "Default", "Day", "Noon", "Sunset", "Night", "Midnight"));

    // === CROSSHAIR ===
    public final BooleanSetting customCrosshair = add(new BooleanSetting("Custom crosshair", false));
    public final ModeSetting crosshairStyle = add(new ModeSetting("Style", "Cross",
            "Cross", "Dot", "Circle", "T-Shape", "X-Shape", "Arrow"));
    public final ColorSetting crosshairColor = add(new ColorSetting("Crosshair color", 0x00FF00));
    public final NumberSetting crosshairLength = add(new NumberSetting("Crosshair length", 6, 1, 15, 1));
    public final NumberSetting crosshairGap = add(new NumberSetting("Crosshair gap", 3, 0, 10, 1));
    public final NumberSetting crosshairWidth = add(new NumberSetting("Crosshair width", 1, 1, 4, 1));
    public final BooleanSetting crosshairDot = add(new BooleanSetting("Crosshair dot", true));
    public final NumberSetting crosshairDotSize = add(new NumberSetting("Dot size", 2, 1, 5, 1));
    public final BooleanSetting crosshairOutline = add(new BooleanSetting("Outline", true));
    public final ColorSetting crosshairOutlineColor = add(new ColorSetting("Outline color", 0x000000));

    // === DYNAMIC ===
    public final BooleanSetting dynamicCrosshair = add(new BooleanSetting("Dynamic", true));
    public final NumberSetting dynamicMove = add(new NumberSetting("Move expand", 4.0f, 0f, 12f, 0.5f));
    public final NumberSetting dynamicJump = add(new NumberSetting("Jump expand", 3.0f, 0f, 12f, 0.5f));
    public final NumberSetting dynamicHit = add(new NumberSetting("Hit expand", 6.0f, 0f, 20f, 0.5f));
    public final NumberSetting dynamicSmooth = add(new NumberSetting("Smooth", 8f, 2f, 20f, 0.5f));

    // === ROTATION (выравнивание) ===
    public final NumberSetting rotationAngle = add(new NumberSetting("Rotation (deg)", 0f, -180f, 180f, 1f));
    public final BooleanSetting rotateDynamic = add(new BooleanSetting("Spin while attacking", false));
    public final NumberSetting rotateDynamicSpeed = add(new NumberSetting("Spin speed", 180f, 20f, 720f, 10f));

    public CustomVisualsModule() {
        super("CustomVisuals", "Custom time and crosshair", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
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
            client.world.getLevelProperties().setTimeOfDay(time);
        }
    }

    /** Статическая утилита для HUD — возвращает [extraGap, rotationDeg]. */
    public static float[] crosshairState() {
        CustomVisualsModule mod = com.example.bfb.ModuleManager.get(CustomVisualsModule.class);
        if (mod == null || !mod.isEnabled() || !mod.customCrosshair.get()) return new float[]{0f, 0f};

        // Считаем дополнительный gap
        float extra = 0f;
        if (mod.dynamicCrosshair.get() && BfbMod.getClient().player != null) {
            var p = BfbMod.getClient().player;

            // движение по горизонтали
            double speed = Math.hypot(p.getVelocity().x, p.getVelocity().z);
            float speedFactor = (float) Math.min(1.0, speed / 0.2);
            extra += mod.dynamicMove.getFloat() * speedFactor;

            // в воздухе
            if (!p.isOnGround()) {
                extra += mod.dynamicJump.getFloat();
            }

            // удар — берём прогресс из SwingAnimations
            float hitP = SwingAnimationsModule.hitProgress(200);
            if (hitP > 0f) {
                extra += mod.dynamicHit.getFloat() * hitP;
            }
        }

        float rot = mod.rotationAngle.getFloat();
        if (mod.rotateDynamic.get()) {
            long t = System.currentTimeMillis();
            rot += (t % 2000L) / 2000f * 360f * (mod.rotateDynamicSpeed.getFloat() / 360f);
        }
        return new float[]{extra, rot};
    }
}