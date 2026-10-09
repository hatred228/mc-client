package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class NoRotateModule extends CheatModule {
    public NoRotateModule() {
        super("NoRotate", "Server can't rotate your camera", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }
}
