package com.example.bfb.hud;

/**
 * Плавная анимация from -> target за duration с ease-out sine.
 *
 * Паттерн использования:
 *   HudAnimation anim = new HudAnimation();
 *   // каждый кадр:
 *   anim.run(1.0, 220);   // анимировать к 1.0 за 220мс
 *   anim.update();
 *   float v = anim.value();
 *
 * Или через run каждое состояние — если target совпадает с текущим, ничего не делает.
 */
public final class HudAnimation {
    private double value;
    private double from;
    private double target;
    private long started;
    private long duration;

    public HudAnimation() {}

    public HudAnimation(double initialValue) {
        this.value = this.from = this.target = initialValue;
    }

    /** Запустить анимацию к next за millis миллисекунд. */
    public void run(double next, long millis) {
        update();
        if (Double.compare(next, target) == 0) return;
        from = value;
        target = next;
        duration = Math.max(1L, millis);
        started = System.currentTimeMillis();
    }

    /** Обновить текущее значение. Вызывать каждый кадр. */
    public void update() {
        if (started == 0L) return;
        double part = Math.min(1d, (System.currentTimeMillis() - started) / (double) duration);
        double eased = Math.sin(part * Math.PI * 0.5d); // ease-out sine
        value = from + (target - from) * eased;
        if (part >= 1d) {
            value = target;
            started = 0L;
        }
    }

    public float value() {
        return (float) value;
    }

    /** Мгновенно установить. */
    public void set(double v) {
        this.value = this.from = this.target = v;
        this.started = 0L;
    }

    /** Идёт ли анимация прямо сейчас. */
    public boolean running() {
        return started != 0L;
    }
}