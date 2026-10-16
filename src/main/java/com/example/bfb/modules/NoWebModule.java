package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class NoWebModule extends CheatModule {

    public final BooleanSetting onlySelf = add(new BooleanSetting("Only self", true));
    public final BooleanSetting boostSpeed = add(new BooleanSetting("Restore full speed", true));

    public NoWebModule() {
        super("NoWeb", "Ignore cobweb slowdown", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static boolean isActive() {
        NoWebModule m = com.example.bfb.ModuleManager.get(NoWebModule.class);
        return m != null && m.isEnabled();
    }

    public static boolean boost() {
        NoWebModule m = com.example.bfb.ModuleManager.get(NoWebModule.class);
        return m != null && m.isEnabled() && m.boostSpeed.get();
    }
}