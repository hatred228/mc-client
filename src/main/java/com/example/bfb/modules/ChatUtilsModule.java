package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class ChatUtilsModule extends CheatModule {
    public final ModeSetting suffix = add(new ModeSetting("Suffix", "None", "None", "BFB", "Custom"));
    public final BooleanSetting antiSpam = add(new BooleanSetting("AntiSpam", true));
    public final BooleanSetting antiAd = add(new BooleanSetting("AntiAd", true));
    public final BooleanSetting nameProtect = add(new BooleanSetting("NameProtect", false));
    public final BooleanSetting autoTip = add(new BooleanSetting("AutoTip", false));

    public ChatUtilsModule() {
        super("ChatUtils", "Chat helpers: suffix, anti-spam, name protect", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }
}