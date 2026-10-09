package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import org.lwjgl.glfw.GLFW;

public class NoSlowdownModule extends CheatModule {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.22, 0.1, 0.4, 0.01));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Default", "Default", "SpookyTime"));

    public NoSlowdownModule() {
        super("NoSlow", "No slowdown from eating and bow", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null) return;

        // velocity убрана — Matrix/Vulcan палят клиентский impulse.
        // Реальная логика NoSlow — в KeyboardInputMixin: пока модуль включён
        // и игрок использует еду/лук, множитель 1.0 вместо ванильного 0.2.
    }
}   