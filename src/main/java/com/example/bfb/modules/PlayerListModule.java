package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class PlayerListModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 128, 32, 256, 4));
    public final BooleanSetting health = add(new BooleanSetting("Health", true));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    public final BooleanSetting gear = add(new BooleanSetting("Gear", false));

    public PlayerListModule() {
        super("PlayerList", "HUD list of nearby players", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }
}