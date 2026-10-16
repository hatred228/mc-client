package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * HitImpact — визуальные эффекты в момент удара.
 *
 * Что делает:
 *   1. Slow motion — глобально замедляет tickDelta для рендера на N ms.
 *   2. Freeze — модель цели замирает на N ms.
 *   3. Screen shake — камера трясётся по шуму.
 *   4. Impact flash — белая вспышка по краям экрана.
 *
 * Управление через статические методы triggerHit()/triggerKill() —
 * их вызывает AttackSoundMixin, как и SwingAnimations.
 */
public class HitImpactModule extends CheatModule {

    public final BooleanSetting slowMo = add(new BooleanSetting("Slow motion", false));
    public final NumberSetting slowFactor = add(new NumberSetting("Slow factor", 0.4f, 0.1f, 0.9f, 0.05f));
    public final NumberSetting slowDuration = add(new NumberSetting("Slow duration (ms)", 200, 50, 800, 25));

    public final BooleanSetting freeze = add(new BooleanSetting("Freeze target", false));
    public final NumberSetting freezeDuration = add(new NumberSetting("Freeze duration (ms)", 200, 50, 800, 25));

    public final BooleanSetting shake = add(new BooleanSetting("Screen shake", false));
    public final NumberSetting shakeIntensity = add(new NumberSetting("Shake intensity", 4.0f, 0.5f, 12.0f, 0.5f));
    public final NumberSetting shakeDuration = add(new NumberSetting("Shake duration (ms)", 250, 50, 800, 25));

    public final BooleanSetting flash = add(new BooleanSetting("Impact flash", false));
    public final NumberSetting flashAlpha = add(new NumberSetting("Flash alpha", 90, 20, 200, 5));
    public final ColorSetting flashColor = add(new ColorSetting("Flash color", 0xFFFFFF));

    // === static state для миксинов ===
    private static long hitTime = 0L;
    private static long killTime = 0L;
    private static HitImpactModule instance;

    public HitImpactModule() {
        super("HitImpact", "Slow-mo, freeze, shake and flash on hit", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    public static HitImpactModule get() { return instance; }

    /** Вызывается из AttackSoundMixin на каждый удар. */
    public static void triggerHit() {
        hitTime = System.currentTimeMillis();
    }

    /** Вызывается на kill. */
    public static void triggerKill() {
        killTime = System.currentTimeMillis();
    }

    /** Прогресс удара 1.0 → 0.0. */
    public static float hitProgress(long durationMs) {
        if (hitTime == 0L) return 0f;
        long dt = System.currentTimeMillis() - hitTime;
        if (dt < 0 || dt > durationMs) return 0f;
        return 1f - dt / (float) durationMs;
    }

    public static float killProgress(long durationMs) {
        if (killTime == 0L) return 0f;
        long dt = System.currentTimeMillis() - killTime;
        if (dt < 0 || dt > durationMs) return 0f;
        return 1f - dt / (float) durationMs;
    }

    /** Текущий прогресс с учётом hit или kill — берём максимум. */
    public static float activeProgress(HitImpactModule mod) {
        if (mod == null) return 0f;
        float a = mod.slowMo.get() ? hitProgress(mod.slowDuration.getInt()) : 0f;
        float b = mod.freeze.get() ? hitProgress(mod.freezeDuration.getInt()) : 0f;
        return Math.max(a, b);
    }

    /** Множитель tickDelta для slow-mo. 1.0 = норма, <1 = замедление. */
    public static float slowMoMultiplier() {
        HitImpactModule mod = instance;
        if (mod == null || !mod.isEnabled() || !mod.slowMo.get()) return 1f;
        float p = hitProgress(mod.slowDuration.getInt());
        if (p <= 0f) return 1f;
        float f = mod.slowFactor.getFloat();
        // ease: возврат к 1.0 по мере убывания прогресса
        return f + (1f - f) * (1f - p);
    }

    /** Смещение для shake в пикселях. Возвращает [dx, dy]. */
    public static float[] shakeOffset() {
        HitImpactModule mod = instance;
        if (mod == null || !mod.isEnabled() || !mod.shake.get()) return new float[]{0f, 0f};
        float p = hitProgress(mod.shakeDuration.getInt());
        if (p <= 0f) return new float[]{0f, 0f};
        float intensity = mod.shakeIntensity.getFloat() * p;
        long t = System.nanoTime();
        float nx = (float) Math.sin(t / 12_000_000.0) * intensity;
        float ny = (float) Math.cos(t / 13_500_000.0) * intensity;
        return new float[]{nx, ny};
    }

    /** ARGB для flash-эффекта. */
    public static int flashColorArgb() {
        HitImpactModule mod = instance;
        if (mod == null || !mod.isEnabled() || !mod.flash.get()) return 0;
        float p = hitProgress(mod.shakeDuration.getInt());
        if (p <= 0f) return 0;
        int a = (int) (mod.flashAlpha.getInt() * p);
        int rgb = mod.flashColor.getRgb() & 0xFFFFFF;
        return (a << 24) | rgb;
    }

    /** Frozen ли сейчас рендер для целей (для LivingEntityRenderer). */
    public static boolean freezeActive() {
        HitImpactModule mod = instance;
        if (mod == null || !mod.isEnabled() || !mod.freeze.get()) return false;
        return hitProgress(mod.freezeDuration.getInt()) > 0f;
    }
}