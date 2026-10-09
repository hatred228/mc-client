package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class SpeedModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Strafe", "Strafe", "Bhop"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.15, 1.0, 1.6, 0.05));

    public SpeedModule() {
        super("Speed", "Ground speed boost", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null || !player.isOnGround()) return;
        double[] wish = CombatUtil.wish(player);
        if (wish[0] == 0 && wish[1] == 0) return;
        double scale = Math.min(0.26, 0.22 * speed.get());
        var velocity = player.getVelocity();
        double y = velocity.y;
        if (mode.is("Bhop")) y = 0.42;
        player.setVelocity(wish[0] * scale, y, wish[1] * scale);
    }
}