package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Управление GCD для античит байпасса.
 *
 * КЛЮЧЕВОЕ: НЕТ jitter. Intave и Vulcan сравнивают rotation-поток
 * с vanilla GCD. Если GCD плавает на ±3% — флаг. Оставляем чистый vanilla.
 *
 * Вся "человечность" rotation — в tremor/drift/micro-pause модулей,
 * которые добавляются ДО applyGcd(). Так поток остаётся кратным GCD.
 */
public final class GcdManager {

    private static float baseGcd = -1f;

    private GcdManager() {}

    /**
     * Точный vanilla GCD 1.21:
     *   f = sens * 0.6 + 0.2
     *   gcd = f^3 * 1.2
     */
    public static float getGcd() {
        if (baseGcd < 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) return 1.0e-5f;
            double sens = client.options.getMouseSensitivity().getValue();
            float f = (float) (sens * 0.6 + 0.2);
            baseGcd = Math.max(1.0e-5f, f * f * f * 1.2f);
        }
        return baseGcd;
    }

    /** Применить GCD к одному значению. */
    public static float applyGcd(float value) {
        float gcd = getGcd();
        return Math.round(value / gcd) * gcd;
    }

    /**
     * Синхронизированный GCD для yaw и pitch.
     * Intave требует чтобы ОБА были кратны одному GCD.
     */
    public static float[] applyGcd(float yaw, float pitch) {
        float gcd = getGcd();
        float y = Math.round(yaw / gcd) * gcd;
        float p = Math.round(pitch / gcd) * gcd;
        return new float[]{ y, p };
    }

    /**
     * Квантуем ДЕЛЬТУ от предыдущего значения — так сервер видит
     * консистентный поток дельт, а не плавающий абсолют.
     */
    public static float[] applyGcdDelta(float fromYaw, float fromPitch, float toYaw, float toPitch) {
        float gcd = getGcd();
        float dYaw = MathHelper.wrapDegrees(toYaw - fromYaw);
        float dPitch = toPitch - fromPitch;
        float snappedDy = Math.round(dYaw / gcd) * gcd;
        float snappedDp = Math.round(dPitch / gcd) * gcd;
        return new float[]{ fromYaw + snappedDy, fromPitch + snappedDp };
    }

    /** Сброс при смене чувствительности / init. */
    public static void reset() {
        baseGcd = -1f;
    }
}