package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.DiscordPresence;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class DiscordModule extends CheatModule {
    public DiscordModule() {
        super("DiscordRPC", "Discord status. App ID in config/undetected/discord.txt", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        if (DiscordPresence.start()) return;
        var player = BfbMod.getClient().player;
        if (player != null) {
            player.sendMessage(Text.literal("§cDiscord: put Application ID into config/undetected/discord.txt"), false);
        }
    }

    @Override
    public void onDisable() {
        DiscordPresence.stop();
    }
}
