package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class FastBreakModule extends CheatModule {
    public final NumberSetting multiplier = add(new NumberSetting("Multiplier", 1.4, 1.0, 2.0, 0.1));

    public FastBreakModule() {
        super("FastBreak", "Blocks break faster", Category.WORLD, GLFW.GLFW_KEY_UNKNOWN);
    }
}