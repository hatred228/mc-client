package com.example.bfb.modules;

import com.example.bfb.AnticheatBypass;
import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AntiAFKModule extends CheatModule {
    public final NumberSetting seconds = add(new NumberSetting("Seconds", 40, 10, 180, 5));
    private int ticks;

    public AntiAFKModule() {
        super("AntiAFK", "Jumps so you don't get kicked", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null) return;
        if (++ticks < seconds.getInt() * 20) return;
        ticks = 0;
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        // silent rotation вместо setYaw — Matrix палит резкий snap
        float[] rot = AnticheatBypass.lookAt(player.getEyePos(),
                player.getEyePos().add(player.getRotationVec(1f).multiply(3)).add(0.1, 0, 0));
        AnticheatBypass.smoothLook(player, rot[0], rot[1]);
        if (player.isOnGround()) {
            var velocity = player.getVelocity();
            player.setVelocity(velocity.x, 0.42, velocity.z);
        }
    }
}