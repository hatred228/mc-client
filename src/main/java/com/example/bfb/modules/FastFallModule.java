package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class FastFallModule extends CheatModule {
    public final NumberSetting strength = add(new NumberSetting("Strength", 0.04, 0.02, 0.08, 0.01));

    public FastFallModule() {
        super("FastFall", "Falls down faster", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null || player.isOnGround() || player.isTouchingWater() || player.getAbilities().flying) return;
        var velocity = player.getVelocity();
        if (velocity.y < -0.04) player.setVelocity(velocity.x, -0.04, velocity.z);
    }
}