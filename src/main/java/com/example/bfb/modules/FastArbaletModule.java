package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class FastArbaletModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));
    public final NumberSetting chargeTimeout = add(new NumberSetting("Charge timeout", 40, 25, 60, 1));

    private static final Random RNG = new Random();
    private int wait;
    private int chargingTicks = 0;
    private boolean wasCharging = false;

    public FastArbaletModule() {
        super("FastArbalet", "Crossbow with anticheat-safe charge", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        wait = 0;
        chargingTicks = 0;
        wasCharging = false;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) {
            reset();
            return;
        }

        if (wait > 0) { wait--; return; }

        Hand hand = hand(player.getMainHandStack(), player.getOffHandStack());
        if (hand == null) { reset(); return; }
        if (mode.is("Legit") && !client.options.useKey.isPressed()) { reset(); return; }

        ItemStack stack = player.getStackInHand(hand);
        boolean charged = CrossbowItem.isCharged(stack);
        boolean drawing = player.isUsingItem()
                && player.getActiveItem().getItem() instanceof CrossbowItem;

        // === 1. Уже заряжен — стреляем ===
        if (charged) {
            client.interactionManager.interactItem(player, hand);
            player.swingHand(hand);
            wait = afterShotDelay();
            reset();
            return;
        }

        // === 2. Заряжаемся — считаем тики ===
        if (drawing) {
            chargingTicks++;
            wasCharging = true;

            // Натяжение завершено — нужно отпустить лук чтобы зарядить
            // В 1.21.11 заряд завершается автоматически при достижении pull time
            int needed = CrossbowItem.getPullTime(stack, player);
            if (chargingTicks >= needed) {
                // Отпускаем — это завершит заряд
                client.interactionManager.stopUsingItem(player);
                wait = 3 + RNG.nextInt(2);
                reset();
            }
            // Таймаут — что-то пошло не так, откатываемся
            else if (chargingTicks > chargeTimeout.getInt()) {
                reset();
            }
            return;
        }

        // === 3. Не заряжается и не заряжен — начинаем зарядку ===
        if (!charged && !drawing) {
            if (wasCharging) {
                // Только что завершили отпускание — даём тик на синхронизацию
                wait = 2;
                reset();
                return;
            }
            client.interactionManager.interactItem(player, hand);
            chargingTicks = 0;
            wait = 2 + RNG.nextInt(2);
        }
    }

    private int afterShotDelay() {
        return switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 24 + RNG.nextInt(6);
            case SPOOKYTIME -> 22 + RNG.nextInt(5);
            case VULCAN, MATRIX -> 18 + RNG.nextInt(5);
            case VIMEMC, RAIDMINE -> 16 + RNG.nextInt(5);
            default -> 14 + RNG.nextInt(4);
        };
    }

    private void reset() {
        chargingTicks = 0;
        wasCharging = false;
    }

    private static Hand hand(ItemStack main, ItemStack off) {
        if (main.getItem() instanceof CrossbowItem) return Hand.MAIN_HAND;
        if (off.getItem() instanceof CrossbowItem) return Hand.OFF_HAND;
        return null;
    }
}