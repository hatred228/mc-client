package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class WorldVisualsModule extends CheatModule {
    public final ModeSetting weather = add(new ModeSetting("Weather", "Default", "Default", "Clear", "No fog", "No rain", "Rain", "Rain fog"));
    public final BooleanSetting colorClouds = add(new BooleanSetting("Cloud color", false));
    public final ColorSetting clouds = add(new ColorSetting("Clouds", 0xD8E4FF));

    public WorldVisualsModule() {
        super("World", "Fog, rain and cloud color", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public boolean hideFog() {
        return weather.is("Clear") || weather.is("No fog");
    }

    public boolean heavyFog() {
        return weather.is("Rain fog");
    }

    @Override
    public void onTick() {
        var world = BfbMod.getClient().world;
        if (world == null) return;
        if (weather.is("Clear") || weather.is("No rain")) {
            world.setRainGradient(0f);
            world.setThunderGradient(0f);
        } else if (weather.is("Rain") || weather.is("Rain fog")) {
            world.setRainGradient(1f);
        }
    }
}
