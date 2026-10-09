package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class FastPlaceModule extends CheatModule {
    public static FastPlaceModule instance;
    public final NumberSetting delay = add(new NumberSetting("Delay", 2, 2, 4, 1));

    public FastPlaceModule() {
        super("FastPlace", "Places blocks faster (min 2 ticks)", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }
}