package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * FastFall — быстрое падение через addVelocity.
 *
 * РАНЬШЕ: setVelocity(v.x, -0.04, v.z) — flag.
 * ТЕПЕРЬ: addVelocity с рандомом шага. Граф падения приближается
 * к ванильному, но чуть быстрее.
 */
public class FastFallModule extends CheatModule {
    public final NumberSetting step = add(new NumberSetting("Step", 0.02, 0.01, 0.04, 0.005));
    public final NumberSetting jitter = add(new NumberSetting("Jitter", 0.005, 0.0, 0.02, 0.001));

    private static final Random RNG = new Random();
    private int tick;

    public FastFallModule() {
        super("FastFall", "Fall down faster (safe mode)", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;

        if (player.isOnGround() || player.isTouchingWater() || player.isInLava()
                || player.getAbilities().flying) return;
        if (player.getVelocity().y >= 0) return;

        // === рандом интервала ===
        if ((tick++ & 1) == 0) {
            // шаг с учётом профиля
            double base = step.get();
            double cap = switch (CombatController.mode()) {
                case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 0.025;
                case SPOOKYTIME -> 0.030;
                default -> 0.040;
            };
            double applied = Math.min(cap, base + (RNG.nextDouble() - 0.5) * 2 * jitter.get());
            player.addVelocity(0, -applied, 0);
        }
    }

    @Override
    public void onDisable() {
        tick = 0;
    }
}