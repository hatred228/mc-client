package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.mixin.FishingBobberAccessor;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AutoFishModule extends CheatModule {
    private int wait;

    public AutoFishModule() {
        super("AutoFish", "Casts and reels in the rod", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) return;
        if (wait > 0) { wait--; return; }
        if (!holdRod(player)) return;
        var hook = player.fishHook;
        if (hook == null) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            wait = 12 + (int) (Math.random() * 6);
            return;
        }
        if (((FishingBobberAccessor) hook).bfb$caughtFish()) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            wait = 8 + (int) (Math.random() * 5);
        }
    }

    private static boolean holdRod(net.minecraft.client.network.ClientPlayerEntity player) {
        if (player.getMainHandStack().isOf(Items.FISHING_ROD)) return true;
        var inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (!inventory.getStack(i).isOf(Items.FISHING_ROD)) continue;
            CombatUtil.syncSlot(player, i);
            return true;
        }
        return false;
    }
}