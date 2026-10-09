package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import net.minecraft.item.BowItem;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class FastBowModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));

    private static final Random RNG = new Random();
    private int wait;

    public FastBowModule() {
        super("FastBow", "Bypasses bow delay (Matrix safe)", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) return;
        if (wait > 0) { wait--; return; }

        Hand hand = player.getMainHandStack().getItem() instanceof BowItem ? Hand.MAIN_HAND
                : player.getOffHandStack().getItem() instanceof BowItem ? Hand.OFF_HAND : null;
        if (hand == null) return;
        if (mode.is("Legit") && !client.options.useKey.isPressed()) return;

        // 350-450ms рандом (21-27 тиков)
        int delayTicks = 21 + RNG.nextInt(7); // 350-450ms

        boolean drawing = player.isUsingItem() && player.getActiveItem().getItem() instanceof BowItem;
        if (drawing && player.getItemUseTime() >= 14) {
            // держим лук не меньше 14 тиков (ванильное время заряда)
            if (player.getItemUseTime() >= 14 + RNG.nextInt(3)) {
                client.interactionManager.stopUsingItem(player);
                wait = delayTicks;
            }
        } else if (!player.isUsingItem()) {
            client.interactionManager.interactItem(player, hand);
            wait = 2 + RNG.nextInt(3);
        }
    }
}