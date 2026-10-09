package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.gui.screen.DeathScreen;
import org.lwjgl.glfw.GLFW;

public class AutoRespawnModule extends CheatModule {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 0, 40, 1));
    private int wait;

    public AutoRespawnModule() {
        super("AutoRespawn", "Respawns instantly", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (!(client.currentScreen instanceof DeathScreen) || client.player == null) {
            wait = delay.getInt();
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        client.player.requestRespawn();
        client.setScreen(null);
    }
}
