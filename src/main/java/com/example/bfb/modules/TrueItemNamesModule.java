package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class TrueItemNamesModule extends CheatModule {
    public TrueItemNamesModule() {
        super("TrueItemNames", "Strips server custom item names", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }
}