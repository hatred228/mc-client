package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class AirJumpModule extends CheatModule {
    private boolean wasJump;
    private int cooldown;

    public AirJumpModule() {
        super("AirJump", "Jump in mid-air", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;
        if (cooldown > 0) cooldown--;
        boolean jump = client.options.jumpKey.isPressed();
        if (jump && !wasJump && cooldown == 0 && !player.isOnGround()
                && !player.isTouchingWater() && !player.isInLava()) {
            var velocity = player.getVelocity();
            player.setVelocity(velocity.x, 0.42, velocity.z);
            player.fallDistance = 0;
            cooldown = 8;
        }
        wasJump = jump;
    }
}