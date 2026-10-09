package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class SprintModule extends CheatModule {
    public final BooleanSetting omni = add(new BooleanSetting("Omni", true));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Default", "Default", "SpookyTime"));

    public SprintModule() {
        super("Sprint", "Auto-sprint, including sideways", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null || player.isSneaking()) return;
        boolean moving = omni.get() ? player.forwardSpeed != 0 || player.sidewaysSpeed != 0 : player.forwardSpeed > 0;
        if (!moving) return;
        if (player.isSprinting()) return;
        player.setSprinting(true);
    }
}