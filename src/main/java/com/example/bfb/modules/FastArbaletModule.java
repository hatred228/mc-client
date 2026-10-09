package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class FastArbaletModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));

    private static final Random RNG = new Random();
    private int wait;

    public FastArbaletModule() {
        super("FastArbalet", "Crossbow with Matrix-safe delay", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) return;
        if (wait > 0) { wait--; return; }

        Hand hand = hand(player.getMainHandStack(), player.getOffHandStack());
        if (hand == null) return;
        if (mode.is("Legit") && !client.options.useKey.isPressed()) return;

        ItemStack stack = player.getStackInHand(hand);

        // 1) если арбалет НЕ заряжен и не заряжается — начинаем зарядку
        if (!CrossbowItem.isCharged(stack) && !player.isUsingItem()) {
            client.interactionManager.interactItem(player, hand);
            wait = 2 + RNG.nextInt(3);
            return;
        }

        // 2) если заряжается — проверяем, закончился ли заряд
        boolean drawing = player.isUsingItem() && player.getActiveItem().getItem() instanceof CrossbowItem;
        if (drawing && player.getItemUseTime() >= CrossbowItem.getPullTime(stack, player)) {
            client.interactionManager.stopUsingItem(player);
            wait = 2 + RNG.nextInt(3);
            return;
        }

        // 3) если заряжен — стреляем
        if (CrossbowItem.isCharged(stack)) {
            client.interactionManager.interactItem(player, hand);
            // 350-450ms задержка после выстрела
            wait = 21 + RNG.nextInt(7);
        }
    }

    private static Hand hand(ItemStack main, ItemStack off) {
        if (main.getItem() instanceof CrossbowItem) return Hand.MAIN_HAND;
        if (off.getItem() instanceof CrossbowItem) return Hand.OFF_HAND;
        return null;
    }
}