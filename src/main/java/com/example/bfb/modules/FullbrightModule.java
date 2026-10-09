package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class FullbrightModule extends CheatModule {
    public final NumberSetting level = add(new NumberSetting("Level", 15, 4, 15, 1));

    public FullbrightModule() {
        super("Fullbright", "Maximum brightness", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }
}
