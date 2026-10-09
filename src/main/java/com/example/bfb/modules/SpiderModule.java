package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class SpiderModule extends CheatModule {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.12, 0.05, 0.2, 0.01));

    public SpiderModule() {
        super("Spider", "Climb up walls", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null || !player.horizontalCollision || player.isOnGround()) return;
        if (player.forwardSpeed <= 0) return;
        var velocity = player.getVelocity();
        player.setVelocity(velocity.x, Math.min(0.12, speed.get()), velocity.z);
        player.fallDistance = 0;
    }
}