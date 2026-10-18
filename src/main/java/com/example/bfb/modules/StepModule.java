package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.attribute.EntityAttributes;
import org.lwjgl.glfw.GLFW;

/**
 * Step — step height. Grim палит если step > 0.6 без движения вперёд.
 * Решение: компенсируем "прыжком" когда есть step > 0.6, или ставим 0.6.
 */
public class StepModule extends CheatModule {
    public final NumberSetting height = add(new NumberSetting("Height", 0.6, 0.6, 1.5, 0.1));

    private float lastHeight = 0.6f;

    public StepModule() {
        super("Step", "Step up full blocks", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null) return;

        var attribute = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attribute == null) return;

        // safe height — 0.6 для Grim/Polar, до 1.0 для слабых античитов
        float maxSafe = switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 0.6f;
            case SPOOKYTIME -> 0.7f;
            case VULCAN, MATRIX -> 1.0f;
            default -> height.getFloat();
        };
        float target = Math.min(height.getFloat(), maxSafe);

        if (Math.abs(lastHeight - target) > 0.001f) {
            attribute.setBaseValue(target);
            lastHeight = target;
        }
    }

    @Override
    public void onDisable() {
        var player = BfbMod.getClient().player;
        if (player == null) return;
        var attribute = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attribute != null) attribute.setBaseValue(0.6);
        lastHeight = 0.6f;
    }
}