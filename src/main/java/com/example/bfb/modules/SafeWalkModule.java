package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class SafeWalkModule extends CheatModule {
    public final BooleanSetting lava = add(new BooleanSetting("Lava", true));
    public final BooleanSetting fire = add(new BooleanSetting("Fire", true));
    public final BooleanSetting edges = add(new BooleanSetting("Edges", true));
    public final BooleanSetting voidWorld = add(new BooleanSetting("Void", false));

    public SafeWalkModule() {
        super("SafeWalk", "Won't walk into lava / fire / edges", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }
}