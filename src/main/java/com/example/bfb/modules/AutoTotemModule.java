package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.NotificationSystem;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/**
 * AutoTotem — ставит тотем в оффхенд при низком HP.
 *
 * Фазовая машина — смена фазы = 1 тик:
 *   Phase 0 = idle (поиск цели)
 *   Phase 1 = syncSlot хотбара (если тотем в хотбаре)
 *   Phase 2 = SWAP_ITEM_WITH_OFFHAND
 *   Phase 3 = возврат слота (если меняли)
 *
 * Если тотем в инвентаре (не хотбар):
 *   Phase 10 = clickSlot SWAP с хотбаром
 *   Phase 11 = SWAP_ITEM_WITH_OFFHAND
 *   Phase 12 = finish + notification
 */
public class AutoTotemModule extends CheatModule {
    public final NumberSetting health = add(new NumberSetting("Health", 10, 1, 19, 0.5));
    public final NumberSetting ttl = add(new NumberSetting("Phase TTL", 4, 2, 8, 1));

    // phase-машина
    private int phase = 0;
    private int phaseTtl = 0;
    private int savedSlot = -1;      // слот до операции (хотбар)
    private int targetSlot = -1;     // слот с тотемом (хотбар 0..8 или инвентарь 9..35)
    private int cooldown = 0;

    public AutoTotemModule() {
        super("AutoTotem", "Place totem in offhand on low HP", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        phase = 0;
        phaseTtl = 0;
        savedSlot = -1;
        targetSlot = -1;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null) {
            reset();
            return;
        }

        // ведём фазу до конца независимо от HP
        if (phase != 0) {
            tickPhase(client, player);
            return;
        }

        if (cooldown > 0) { cooldown--; return; }

        // проверка HP
        float totalHp = player.getHealth() + player.getAbsorptionAmount();
        if (totalHp > health.get()) return;

        // в оффхенде уже тотем — ничего не делаем
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        // ищем тотем
        int slot = findTotemSlot(player);
        if (slot < 0) return;

        targetSlot = slot;
        savedSlot = player.getInventory().getSelectedSlot();

        if (slot >= 0 && slot < 9) {
            // тотем в хотбаре — syncSlot → SWAP → вернуть
            if (savedSlot != slot) {
                CombatUtil.syncSlot(player, slot);
                phase = 1;
            } else {
                phase = 2;
            }
        } else {
            // тотем в инвентаре — clickSlot SWAP + SWAP_ITEM_WITH_OFFHAND
            phase = 10;
        }
        phaseTtl = ttl.getInt();
    }

    /** Основная фазовая машина. Каждая фаза = 1 тик. */
    private void tickPhase(net.minecraft.client.MinecraftClient client,
                           ClientPlayerEntity player) {
        if (--phaseTtl <= 0) {
            // время вышло — откат
            if (savedSlot >= 0 && targetSlot >= 0 && targetSlot < 9 && savedSlot != targetSlot
                    && player.getInventory().getSelectedSlot() == targetSlot) {
                CombatUtil.syncSlot(player, savedSlot);
            }
            reset();
            return;
        }

        if (client.interactionManager == null) { reset(); return; }

        switch (phase) {
            case 1 -> {
                // хотбар слот уже сменён — теперь SWAP с оффхендом
                phase = 2;
            }
            case 2 -> {
                // SWAP_ITEM_WITH_OFFHAND
                player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        Action.SWAP_ITEM_WITH_OFFHAND,
                        player.getBlockPos(),
                        player.getHorizontalFacing()));
                if (savedSlot >= 0 && savedSlot != targetSlot && targetSlot < 9) {
                    phase = 3;
                } else {
                    cooldown = 20;
                    reset();
                }
            }
            case 3 -> {
                // вернуть слот после SWAP
                CombatUtil.syncSlot(player, savedSlot);
                cooldown = 20;
                reset();
            }
            case 10 -> {
                // тотем в инвентаре (9..35) — SWAP через clickSlot в хотбар
                int screenSlot = targetSlot;
                int hotbarSlot = player.getInventory().getSelectedSlot();
                client.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId,
                        screenSlot,
                        hotbarSlot,
                        SlotActionType.SWAP,
                        player);
                // на следующем тике — SWAP_ITEM_WITH_OFFHAND
                phase = 11;
                phaseTtl = ttl.getInt();
            }
            case 11 -> {
                // теперь тотем в хотбаре — SWAP с оффхендом
                player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        Action.SWAP_ITEM_WITH_OFFHAND,
                        player.getBlockPos(),
                        player.getHorizontalFacing()));
                // возврат: SWAP обратно
                phase = 12;
                phaseTtl = ttl.getInt();
            }
            case 12 -> {
                // хотбар снова правильный — слот возвращать не надо,
                // потому что SWAP_ITEM_WITH_OFFHAND вернул старое содержимое хотбара
                cooldown = 20;
                NotificationSystem.push("Totem equipped", 0xFF4CC3FF, 1500);
                reset();
            }
        }
    }

    private static int findTotemSlot(ClientPlayerEntity player) {
        var inv = player.getInventory();
        // сначала ищем в хотбаре
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        // потом в инвентаре
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }
}