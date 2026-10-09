package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.mixin.KeyBindingAccessor;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class AutoClickerModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Hold", "Hold", "Always"));
    public final NumberSetting cpsMin = add(new NumberSetting("CPS min", 8, 1, 20, 1));
    public final NumberSetting cpsMax = add(new NumberSetting("CPS max", 14, 1, 20, 1));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    private static final Random RNG = new Random();
    private int ticks;
    private int burstCooldown;
    private int burstClicks;

    public AutoClickerModule() {
        super("AutoClicker", "Gaussian CPS distribution", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.currentScreen != null || client.interactionManager == null) return;
        if (mode.is("Hold") && !holdingAttack(client)) { ticks = 0; return; }
        if (client.crosshairTarget instanceof BlockHitResult block && holdingAttack(client)) {
            client.interactionManager.updateBlockBreakingProgress(block.getBlockPos(), block.getSide());
        }

        if (burstCooldown > 0) { burstCooldown--; ticks = 0; return; }

        int min = Math.min(cpsMin.getInt(), cpsMax.getInt());
        int max = Math.max(cpsMin.getInt(), cpsMax.getInt());
        double mean = (min + max) / 2.0;
        double stdDev = (max - min) / 4.0;

        double cps = mean + RNG.nextGaussian() * stdDev;
        cps = Math.max(min, Math.min(max, cps));

        int base = Math.max(1, (int) Math.round(20.0 / cps));
        int gap = Math.max(1, base + RNG.nextInt(3) - 1);

        if (++ticks < gap) return;
        ticks = 0;

        Entity target = aimed(client, player);
        if (target != null && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
            // без reach spoof — это обычный кликер
            CombatUtil.attack(target, false);
        } else {
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }

        burstClicks++;
        if (burstClicks >= 8 + RNG.nextInt(8)) {
            burstCooldown = 6 + RNG.nextInt(7);
            burstClicks = 0;
        }
    }

    private Entity aimed(MinecraftClient client, net.minecraft.client.network.ClientPlayerEntity player) {
        if (client.crosshairTarget instanceof EntityHitResult hit
                && CombatUtil.accept(hit.getEntity(), player, players.get(), mobs.get(), animals.get())) {
            return hit.getEntity();
        }
        LivingEntity best = null;
        double bestDist = 9.0;
        Box area = player.getBoundingBox().expand(3.0);
        for (LivingEntity entity : player.getEntityWorld().getEntitiesByClass(LivingEntity.class, area, candidate ->
                CombatUtil.accept(candidate, player, players.get(), mobs.get(), animals.get()))) {
            // CombatUtil.angle читает silent yaw — если silent rotation активна, кликер бьёт по её цели
            if (CombatUtil.angle(player, entity) > 30f) continue;
            double dist = entity.squaredDistanceTo(player);
            if (dist < bestDist) { bestDist = dist; best = entity; }
        }
        return best;
    }

    private static boolean holdingAttack(MinecraftClient client) {
        if (client.options.attackKey.isPressed()) return true;
        var key = ((KeyBindingAccessor) client.options.attackKey).bfb$boundKey();
        long handle = client.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, key.getCode()) == GLFW.GLFW_PRESS;
    }
}