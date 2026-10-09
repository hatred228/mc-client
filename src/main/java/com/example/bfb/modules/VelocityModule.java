package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class VelocityModule extends CheatModule {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal", 40, 30, 100, 1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", 40, 30, 100, 1));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Default", "Default", "Legit", "SpookyTime"));

    public VelocityModule() {
        super("Velocity", "Knockback strength", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    public double[] smooth(double x, double y, double z) {
        double h = horizontal.get() / 100.0;
        double v = vertical.get() / 100.0;
        if (mode.is("SpookyTime")) { h = Math.min(h, 0.5); v = Math.min(v, 0.5); }
        // минимум 30% — 0% это мгновенный флаг Vulcan
        h = Math.max(0.30, h);
        v = Math.max(0.30, v);
        return new double[]{x * h, y * v, z * h};
    }

    public boolean delayed() { return mode.is("SpookyTime"); }
}