package com.example.bfb.hud;

/**
 * Плавное приближение к target. Экспоненциальный ease-out.
 * Проще чем HudAnimation — нет start/stop, просто постоянное стремление.
 *
 * Паттерн:
 *   AnimatedFloat anim = new AnimatedFloat(0f);
 *   // каждый кадр:
 *   anim.setTarget(1f);
 *   float v = anim.update(dt, 14f);  // dt в секундах, response = скорость
 */
public final class AnimatedFloat {
    private float value;
    private float target;

    public AnimatedFloat(float initial) {
        this.value = initial;
        this.target = initial;
    }

    public void setTarget(float newTarget) {
        target = newTarget;
    }

    /**
     * @param dt       секунды с прошлого кадра (обычно 1/60)
     * @param response скорость реакции (больше — быстрее добегает)
     * @return текущее значение
     */
    public float update(float dt, float response) {
        float safe = Math.max(0f, Math.min(dt, 0.1f));
        float amount = 1f - (float) Math.exp(-Math.max(0f, response) * safe);
        value += (target - value) * amount;
        return value;
    }

    public float value() {
        return value;
    }

    /** Мгновенно установить. */
    public void set(float v) {
        this.value = this.target = v;
    }
}