package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.combat.CombatMode;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Velocity — ослабление knockback.
 *
 * РАБОТАЕТ ЧЕРЕЗ МИКСИН:
 *   ClientPlayNetworkHandlerMixin.onEntityVelocityUpdate
 *   перехватывает пакет и вызывает smooth(). Packet не доходит до клиента.
 *
 * Режимы:
 *   Default — берёт коэффициенты из CombatController профиля античита
 *   Legit / SpookyTime — доп. ручное ограничение
 */
public class VelocityModule extends CheatModule {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal", 40, 30, 100, 1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", 40, 30, 100, 1));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Default",
            "Default", "Legit", "SpookyTime"));

    public VelocityModule() {
        super("Velocity", "Knockback strength", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    /**
     * Сглаживание velocity. Возвращает [x, y, z] после модификации.
     * Вызывается ТОЛЬКО из ClientPlayNetworkHandlerMixin.
     */
    public double[] smooth(double x, double y, double z) {
        CombatMode cm = CombatController.mode();
        double h, v;

        // === PER-MODE коэффициенты по античиту ===
        switch (cm) {
            case GRIM -> { h = 0.5; v = 0.5; }
            case POLAR, REALLYWORLD, DONUTSMP -> { h = 0.6; v = 0.5; }
            case VULCAN -> { h = 0.4; v = 0.4; }
            case MATRIX -> { h = 0.45; v = 0.4; }
            case SPOOKYTIME -> { h = 0.5; v = 0.45; }
            case VIMEMC, RAIDMINE -> { h = 0.5; v = 0.5; }
            default -> { h = horizontal.get() / 100.0; v = vertical.get() / 100.0; }
        }

        // === ручные режимы — поверх профиля ===
        if (mode.is("SpookyTime")) {
            h = Math.min(h, 0.5);
            v = Math.min(v, 0.5);
        }
        if (mode.is("Legit")) {
            h = Math.max(0.6, h);
            v = Math.max(0.6, v);
        }

        // жёсткий пол 30% — ниже палится на любом AC
        h = Math.max(0.30, h);
        v = Math.max(0.30, v);

        return new double[]{ x * h, y * v, z * h };
    }

    @Override
    public void onDisable() {
        // сброс не нужен — модуль stateless
    }
}