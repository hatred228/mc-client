package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.NotificationSystem;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * AutoGap — использует золотое яблоко при низком HP.
 *
 * РАНЬШЕ: setSelectedSlot без пакета → сервер видел interact на СТАРОМ слоте.
 *         В 90% случаев ничего не происходило.
 *
 * ТЕПЕРЬ: фазовая машина:
 *   Phase 1 = syncSlot (пакет уходит)
 *   Phase 2 = interactItem (сервер уже знает про новый слот)
 *   Phase 3 = syncSlot обратно
 *
 * Каждая фаза = 1 тик. Максимум 3 тика на операцию.
 * Не палится.
 */
public class AutoGapModule extends CheatModule {
    public final NumberSetting health = add(new NumberSetting("Health", 10, 1, 19, 0.5));

    private int phase = 0;
    private int phaseTtl = 0;
    private int targetSlot = -1;
    private int savedSlot = -1;
    private int cooldown = 0;

    public AutoGapModule() {
        super("AutoGap", "Uses golden apple on low health", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        phase = 0;
        phaseTtl = 0;
        targetSlot = -1;
        savedSlot = -1;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null) {
            reset();
            return;
        }

        // ведём фазу до конца
        if (phase != 0) {
            tickPhase(client, player);
            return;
        }

        if (cooldown > 0) { cooldown--; return; }

        float hp = player.getHealth() + player.getAbsorptionAmount();
        if (hp > health.get()) return;

        int slot = findAppleSlot(player);
        if (slot < 0) return;

        targetSlot = slot;
        savedSlot = player.getInventory().getSelectedSlot();

        if (savedSlot != targetSlot) {
            // Phase 1: смена слота
            CombatUtil.syncSlot(player, targetSlot);
            phase = 1;
        } else {
            // слот уже правильный — сразу interact
            phase = 2;
        }
        phaseTtl = 4;
    }

    private void tickPhase(net.minecraft.client.MinecraftClient client,
                           ClientPlayerEntity player) {
        if (--phaseTtl <= 0) {
            // время вышло — возвращаем слот если меняли
            if (savedSlot >= 0 && savedSlot != targetSlot
                    && player.getInventory().getSelectedSlot() == targetSlot) {
                CombatUtil.syncSlot(player, savedSlot);
            }
            reset();
            return;
        }

        switch (phase) {
            case 1 -> {
                // слот сменён — теперь interact
                phase = 2;
            }
            case 2 -> {
                // interact — сервер уже видит новый selectedSlot
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                NotificationSystem.push("Used Golden Apple", 0xFF4CC3FF, 1500);
                if (savedSlot >= 0 && savedSlot != targetSlot) {
                    phase = 3;
                } else {
                    cooldown = 20;
                    reset();
                }
            }
            case 3 -> {
                // возврат слота
                CombatUtil.syncSlot(player, savedSlot);
                cooldown = 20;
                reset();
            }
        }
    }

    private static int findAppleSlot(ClientPlayerEntity player) {
        var inv = player.getInventory();
        // сначала хотбар
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.GOLDEN_APPLE)
                    || inv.getStack(i).isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                return i;
            }
        }
        // потом инвентарь — НО только для syncSlot через hotbar
        // (клиентский interact может только по хотбару)
        return -1;
    }
}