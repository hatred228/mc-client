package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ClickPearlModule extends CheatModule {
    private boolean wasDown;

    private int pearlPhase = 0;    // 0=idle, 1=slot set, 2=interact, 3=slot restore
    private int pearlSlot = -1;
    private int pearlPrev = -1;
    private int pearlTtl = 0;

    public ClickPearlModule() {
        super("ClickPearl", "Middle click throws an ender pearl", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        wasDown = false;
        resetPearl();
    }

    private void resetPearl() {
        pearlPhase = 0;
        pearlSlot = -1;
        pearlPrev = -1;
        pearlTtl = 0;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) {
            wasDown = false;
            resetPearl();
            return;
        }

        // phase-машина
        if (pearlPhase != 0) {
            tickPearl(client, player);
            return;
        }

        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed || player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) return;

        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;

        pearlSlot = slot;
        pearlPrev = player.getInventory().getSelectedSlot();
        pearlTtl = 4;

        if (pearlPrev != pearlSlot) {
            CombatUtil.syncSlot(player, pearlSlot);
            pearlPhase = 1;
        } else {
            pearlPhase = 2;
        }
    }

    private void tickPearl(net.minecraft.client.MinecraftClient client,
                           net.minecraft.client.network.ClientPlayerEntity player) {
        if (--pearlTtl <= 0) {
            if (pearlPrev >= 0 && pearlPrev != pearlSlot) CombatUtil.syncSlot(player, pearlPrev);
            resetPearl();
            return;
        }
        if (pearlPhase == 1) {
            pearlPhase = 2;
            return;
        }
        if (pearlPhase == 2) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
            if (pearlPrev >= 0 && pearlPrev != pearlSlot) {
                pearlPhase = 3;
            } else {
                resetPearl();
            }
            return;
        }
        if (pearlPhase == 3) {
            CombatUtil.syncSlot(player, pearlPrev);
            resetPearl();
        }
    }
}