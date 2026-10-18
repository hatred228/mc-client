package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class SwingAnimationsModule extends CheatModule {
    public final ModeSetting animation = add(new ModeSetting("Animation", "Custom",
            "Default", "Custom", "Spin", "Side"));

    public final NumberSetting handX = add(new NumberSetting("Hand X", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting handY = add(new NumberSetting("Hand Y", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting handZ = add(new NumberSetting("Hand Z", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting rotateX = add(new NumberSetting("Rotate X", 0.0, -180.0, 180.0, 5.0));
    public final NumberSetting rotateY = add(new NumberSetting("Rotate Y", 0.0, -180.0, 180.0, 5.0));
    public final NumberSetting rotateZ = add(new NumberSetting("Rotate Z", 0.0, -180.0, 180.0, 5.0));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final BooleanSetting noSwing = add(new BooleanSetting("No Swing", false));

    // === smoothing / rotation ===
    public final ModeSetting rotationOrder = add(new ModeSetting("Rotation order", "XYZ",
            "XYZ", "ZYX", "YXZ", "YZX", "ZXY", "XZY"));
    public final NumberSetting lerpSpeed = add(new NumberSetting("Lerp speed", 1.0f, 0.1f, 1.0f, 0.05f));
    public final NumberSetting swingAmplitude = add(new NumberSetting("Swing amplitude", 1.0f, 0.2f, 2.5f, 0.05f));

    // hit animations
    public final BooleanSetting hitSlowdown = add(new BooleanSetting("Hit slowdown", false));
    public final NumberSetting hitSlowFactor = add(new NumberSetting("Slow factor", 0.5, 0.1, 1.0, 0.05));
    public final NumberSetting hitSlowDuration = add(new NumberSetting("Slow duration (ms)", 150, 50, 500, 25));
    public final BooleanSetting hitSpin = add(new BooleanSetting("Hit spin", false));
    public final NumberSetting hitSpinAmount = add(new NumberSetting("Spin degrees", 90, 15, 360, 15));
    public final BooleanSetting hitScale = add(new BooleanSetting("Hit scale pop", false));
    public final NumberSetting hitScaleAmount = add(new NumberSetting("Scale pop", 0.3, 0.1, 1.0, 0.05));
    public final BooleanSetting killEffect = add(new BooleanSetting("Kill effect", false));
    public final NumberSetting killSpinAmount = add(new NumberSetting("Kill spin degrees", 360, 90, 720, 30));
    public final NumberSetting killDuration = add(new NumberSetting("Kill duration (ms)", 400, 150, 1000, 50));

    private static long lastHitTime = 0L;
    private static long lastKillTime = 0L;

    private static float sYaw = 0f;
    private static float sPitch = 0f;
    private static float sRoll = 0f;

    public SwingAnimationsModule() {
        super("SwingAnimations", "Customize hand position and rotation", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void triggerHit() { lastHitTime = System.currentTimeMillis(); }
    public static void triggerKill() { lastKillTime = System.currentTimeMillis(); }

    public static float hitProgress(long durationMs) {
        if (lastHitTime == 0L) return 0f;
        long dt = System.currentTimeMillis() - lastHitTime;
        if (dt < 0 || dt > durationMs) return 0f;
        return 1f - dt / (float) durationMs;
    }

    public static float killProgress(long durationMs) {
        if (lastKillTime == 0L) return 0f;
        long dt = System.currentTimeMillis() - lastKillTime;
        if (dt < 0 || dt > durationMs) return 0f;
        return 1f - dt / (float) durationMs;
    }

    /**
     * lerpSpeed = 1.0 → мгновенно. 0.1 → очень плавно.
     */
    public static void updateSmooth(float targetYaw, float targetPitch, float targetRoll, float lerpSpeed) {
        float blend = Math.max(0.05f, Math.min(1.0f, lerpSpeed));
        sYaw   += (targetYaw   - sYaw)   * blend;
        sPitch += (targetPitch - sPitch) * blend;
        sRoll  += (targetRoll  - sRoll)  * blend;
    }

    public static float lastSmoothYawValue()   { return sYaw; }
    public static float lastSmoothPitchValue() { return sPitch; }
    public static float lastSmoothRollValue()  { return sRoll; }
}