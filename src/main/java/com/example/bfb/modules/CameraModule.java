package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class CameraModule extends CheatModule {
    public final BooleanSetting hurt = add(new BooleanSetting("No hurt", true));
    public final BooleanSetting bob = add(new BooleanSetting("No bob", false));
    public final NumberSetting zoom = add(new NumberSetting("Zoom", 3, 1.5, 8, 0.1));

    public CameraModule() {
        super("Camera", "No shaking. Zoom: hold C or Left Alt", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public boolean zoomHeld() {
        var client = BfbMod.getClient();
        if (client.player == null || client.currentScreen != null || client.getWindow() == null) return false;
        long window = client.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }
}
