package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

/**
 * NoSlowdown — не замедляемся когда едим/стреляем из лука.
 *
 * Логика в LivingEntityMovementMixin.bfb$noSlowdown.
 * Этот класс хранит настройки + добавляет проверки.
 */
public class NoSlowdownModule extends CheatModule {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.22, 0.10, 0.40, 0.01));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Default",
            "Default", "SpookyTime", "Legit"));
    public final BooleanSetting onlyMoving = add(new BooleanSetting("Only moving", true));

    public NoSlowdownModule() {
        super("NoSlow", "No slowdown from eating/bow", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    /**
     * Итоговая скорость при использовании item.
     */
    public float effectiveSpeed() {
        // режим SpookyTime — кап 0.15
        if (mode.is("SpookyTime")) {
            return Math.min(speed.getFloat(), 0.15f);
        }
        // Legit — максимум 0.18
        if (mode.is("Legit")) {
            return Math.min(speed.getFloat(), 0.18f);
        }
        // Default — по профилю античита
        return switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> Math.min(speed.getFloat(), 0.16f);
            case SPOOKYTIME -> Math.min(speed.getFloat(), 0.15f);
            case VULCAN, MATRIX -> Math.min(speed.getFloat(), 0.20f);
            default -> speed.getFloat();
        };
    }

    public boolean checkOnlyMoving(ClientPlayerEntity player) {
        if (!onlyMoving.get()) return true;
        return player.forwardSpeed != 0 || player.sidewaysSpeed != 0;
    }
}