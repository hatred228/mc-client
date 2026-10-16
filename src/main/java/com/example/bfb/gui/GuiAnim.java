package com.example.bfb.gui;

/**
 * Spring-анимация для GUI. Даёт bounce-эффект (как на toggle switch).
 */
public final class GuiAnim {

    private float value;
    private float velocity;
    private float target;

    public GuiAnim(float initial) {
        this.value = initial;
        this.target = initial;
        this.velocity = 0f;
    }

    public void setTarget(float t) {
        this.target = t;
    }

    public float value() {
        return value;
    }

    public void set(float v) {
        this.value = this.target = v;
        this.velocity = 0f;
    }

    /**
     * @param dt      delta time (seconds)
     * @param stiffness жёсткость (например 220)
     * @param damping   демпфирование (например 22)
     */
    public float update(float dt, float stiffness, float damping) {
        if (dt <= 0f) dt = 1f / 60f;
        if (dt > 0.1f) dt = 0.1f;

        float accel = (target - value) * stiffness - velocity * damping;
        velocity += accel * dt;
        value += velocity * dt;

        if (Math.abs(target - value) < 0.0005f && Math.abs(velocity) < 0.0005f) {
            value = target;
            velocity = 0f;
        }
        return value;
    }

    /** Универсальный ease-out подход для плавных значений (без bounce). */
    public float update(float dt, float speed) {
        if (dt <= 0f) dt = 1f / 60f;
        if (dt > 0.1f) dt = 0.1f;
        float amount = 1f - (float) Math.exp(-speed * dt);
        value += (target - value) * amount;
        return value;
    }
}