package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.combat.CombatMode;
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
        super("FastBow", "Bypasses bow delay", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
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

        // === Задержка по профилю античита ===
        int baseDelay = switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 22 + RNG.nextInt(6); // 22-27 тиков
            case SPOOKYTIME -> 20 + RNG.nextInt(5);
            case VULCAN, MATRIX -> 16 + RNG.nextInt(5);
            case VIMEMC, RAIDMINE -> 14 + RNG.nextInt(5);
            default -> 12 + RNG.nextInt(4);
        };

        // Минимальное время натяжения тоже зависит от профиля
        int minDraw = switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 16;
            case SPOOKYTIME -> 14;
            default -> 12;
        };

        boolean drawing = player.isUsingItem() && player.getActiveItem().getItem() instanceof BowItem;
        if (drawing && player.getItemUseTime() >= minDraw + RNG.nextInt(3)) {
            client.interactionManager.stopUsingItem(player);
            wait = baseDelay;
        } else if (!player.isUsingItem()) {
            client.interactionManager.interactItem(player, hand);
            wait = 2 + RNG.nextInt(3);
        }
    }

    @Override
    public void onDisable() {
        wait = 0;
    }
}