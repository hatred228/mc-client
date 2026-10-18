package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class FastPlaceModule extends CheatModule {
    public static FastPlaceModule instance;
    public final NumberSetting delay = add(new NumberSetting("Delay", 3, 2, 4, 1));

    public FastPlaceModule() {
        super("FastPlace", "Places blocks faster (safe mode)", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    /**
     * Итоговая задержка между кликами ПКМ.
     * Grim палит даже 3, если ты сидишь без движения.
     * Минимум 2, для Grim/Polar — 3-4.
     */
    public int effectiveDelay() {
        int base = delay.getInt();
        return switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> Math.max(3, base);
            case SPOOKYTIME -> Math.max(3, base);
            default -> Math.max(2, base);
        };
    }
}