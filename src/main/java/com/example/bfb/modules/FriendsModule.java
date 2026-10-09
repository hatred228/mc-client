package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public class FriendsModule extends CheatModule {
    public FriendsModule() {
        super("Friends", "Keybind toggles friend from crosshair", Category.MISC, GLFW.GLFW_KEY_P);
    }

    /** Вызывается из BfbMod когда нажат бинд. */
    public static void toggleUnderCrosshair() {
        var client = BfbMod.getClient();
        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity pe) {
            boolean added = FriendManager.toggle(pe.getGameProfile().name());
            if (client.player != null) {
                client.player.sendMessage(Text.literal(
                        (added ? "§a[Friends] Added: " : "§c[Friends] Removed: ") + pe.getGameProfile().name()
                ), true);
            }
        }
    }
}