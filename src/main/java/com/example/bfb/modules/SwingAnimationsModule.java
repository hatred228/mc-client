package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class SwingAnimationsModule extends CheatModule {
    public final ModeSetting animation = add(new ModeSetting("Animation", "Custom", "Default", "Custom", "Spin", "Side"));
    public final NumberSetting handX = add(new NumberSetting("Hand X", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting handY = add(new NumberSetting("Hand Y", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting handZ = add(new NumberSetting("Hand Z", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting rotateX = add(new NumberSetting("Rotate X", 0.0, -180.0, 180.0, 5.0));
    public final NumberSetting rotateY = add(new NumberSetting("Rotate Y", 0.0, -180.0, 180.0, 5.0));
    public final NumberSetting rotateZ = add(new NumberSetting("Rotate Z", 0.0, -180.0, 180.0, 5.0));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final BooleanSetting noSwing = add(new BooleanSetting("No Swing", false));

    public SwingAnimationsModule() {
        super("SwingAnimations", "Customize hand position and rotation", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }
}