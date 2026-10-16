package com.example.bfb;

/**
 * Плавная траектория rotation через spring.
 * Плавнее чем ease-out — нет рывков на последних тиках.
 */
public final class RotationTrajectory {

    private float startYaw, startPitch;
    private float targetYaw, targetPitch;
    private float currentYaw, currentPitch;
    private float velYaw = 0f, velPitch = 0f;
    private float stiffness = 30f;
    private float damping = 8f;
    private boolean active;
    private int elapsedTicks = 0;

    public void start(float fromYaw, float fromPitch,
                      float toYaw, float toPitch,
                      int ticks) {
        this.startYaw = fromYaw;
        this.startPitch = fromPitch;
        this.currentYaw = fromYaw;
        this.currentPitch = fromPitch;
        this.velYaw = 0f;
        this.velPitch = 0f;
        this.targetYaw = toYaw;
        this.targetPitch = toPitch;
        this.elapsedTicks = 0;
        this.active = true;

        // stiffness зависит от ticks — чем больше ticks тем мягче
        float t = Math.max(1, ticks);
        this.stiffness = Math.max(8f, 60f / t);
        this.damping = 2f * (float) Math.sqrt(stiffness) * 0.95f;
    }

    /** Смена цели БЕЗ сброса — плавно доворачивает. */
    public void retarget(float toYaw, float toPitch) {
        this.targetYaw = toYaw;
        this.targetPitch = toPitch;
        this.elapsedTicks = 0;
    }

    public void update() {
        if (!active) return;
        elapsedTicks++;

        float dt = 1f / 20f; // один тик = 1/20 секунды
        float dYaw = wrap(targetYaw - currentYaw);
        float dPitch = targetPitch - currentPitch;

        float accY = dYaw * stiffness - velYaw * damping;
        float accP = dPitch * stiffness - velPitch * damping;

        velYaw += accY * dt;
        velPitch += accP * dt;

        currentYaw += velYaw * dt;
        currentPitch += velPitch * dt;

        // близко к цели — держим
        if (Math.abs(dYaw) < 0.3f && Math.abs(dPitch) < 0.3f) {
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            velYaw = 0f;
            velPitch = 0f;
        }
    }

    public boolean isActive() { return active; }
    public float getCurrentYaw() { return currentYaw; }
    public float getCurrentPitch() { return currentPitch; }
    public int getElapsed() { return elapsedTicks; }

    /** Не завершает — просто идёт дальше. release() только если цель потеряна. */
    public void stop() { active = false; }

    public float progress() {
        if (!active) return 1f;
        // считаем сколько осталось до цели
        float d = (float) Math.hypot(
                wrap(targetYaw - currentYaw),
                targetPitch - currentPitch);
        return Math.max(0f, Math.min(1f, 1f - d / 45f));
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}