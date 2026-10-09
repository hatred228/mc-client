package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class ArraylistModule extends CheatModule {
    public ArraylistModule() {
        super("Arraylist", "Enabled modules list on the right", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }
}