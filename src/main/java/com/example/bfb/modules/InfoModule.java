package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class InfoModule extends CheatModule {
    public InfoModule() {
        super("Info", "Client name and FPS top right", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }
}
