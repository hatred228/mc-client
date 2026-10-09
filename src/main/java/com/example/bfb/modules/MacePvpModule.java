package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import org.lwjgl.glfw.GLFW;

public class MacePvpModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 4, 2.5, 6, 0.1));
    public final NumberSetting minFall = add(new NumberSetting("Min fall", 1.5, 1, 10, 0.5));
    public final BooleanSetting switchBack = add(new BooleanSetting("Switch back", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    private int restoreSlot = -1;
    private int macePhase = 0;   // 0=idle, 1=attack pending
    private LivingEntity pendingTarget = null;
    private int pendingTtl = 0;

    public MacePvpModule() {
        super("MacePvP", "Mace hits while falling (no rotation)", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        restore();
        macePhase = 0;
        pendingTarget = null;
        pendingTtl = 0;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null || client.interactionManager == null || client.currentScreen != null) {
            macePhase = 0;
            pendingTarget = null;
            return;
        }

        // восстановление слота при приземлении
        if (restoreSlot >= 0 && player.isOnGround()) {
            CombatUtil.syncSlot(player, restoreSlot);
            restoreSlot = -1;
        }

        // phase: атака на следующем тике после syncSlot
        if (macePhase == 1) {
            if (--pendingTtl <= 0 || pendingTarget == null || !pendingTarget.isAlive()
                    || player.distanceTo(pendingTarget) > range.get() + 0.5) {
                macePhase = 0;
                pendingTarget = null;
                return;
            }
            client.interactionManager.attackEntity(player, pendingTarget);
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            macePhase = 0;
            pendingTarget = null;
            return;
        }

        if (player.fallDistance < minFall.get() || !MaceItem.shouldDealAdditionalDamage(player)) return;

        LivingEntity target = null;
        double best = range.get() * range.get();
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range.get()), candidate ->
                CombatUtil.accept(candidate, player, players.get(), mobs.get(), animals.get())
                        && CombatUtil.rayHits(player, candidate, range.get()))) {
            double dist = entity.squaredDistanceTo(player);
            if (dist < best) {
                best = dist;
                target = entity;
            }
        }
        if (target == null) return;
        int slot = maceSlot(player);
        if (slot < 0) return;
        int previous = player.getInventory().getSelectedSlot();

        if (previous != slot) {
            CombatUtil.syncSlot(player, slot);
            if (switchBack.get() && restoreSlot < 0) restoreSlot = previous;
            // атаку откладываем на следующий тик — сервер сначала должен увидеть смену слота
            pendingTarget = target;
            pendingTtl = 2;
            macePhase = 1;
            return;
        }

        // слот уже правильный — атакуем сразу
        client.interactionManager.attackEntity(player, target);
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    private static int maceSlot(net.minecraft.client.network.ClientPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private void restore() {
        var player = BfbMod.getClient().player;
        if (player != null && restoreSlot >= 0) CombatUtil.syncSlot(player, restoreSlot);
        restoreSlot = -1;
    }
}