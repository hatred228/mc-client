package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class WaterSpeedModule extends CheatModule {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.6, 0.1, 1.0, 0.05));

    public WaterSpeedModule() {
        super("WaterSpeed", "Swim faster", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null || !player.isTouchingWater()) return;
        if (player.forwardSpeed == 0 && player.sidewaysSpeed == 0) return;
        if (player.isSprinting() || player.forwardSpeed > 0) {
            double[] wish = CombatUtil.wish(player);
            double boost = Math.min(0.15, speed.get() * 0.2);
            player.addVelocity(wish[0] * boost, 0, wish[1] * boost);
        }
    }
}