package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

public class NoJumpDelayModule extends CheatModule {
    public NoJumpDelayModule() {
        super("NoJumpDelay", "Instant jump, no delay", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }
}
