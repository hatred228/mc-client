package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class NoFallModule extends CheatModule {
    public NoFallModule() {
        super("NoFall", "No fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }
}
