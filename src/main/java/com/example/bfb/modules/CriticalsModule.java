package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.EntityCache;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * Criticals — безопасный режим.
 *
 * Раньше: реальный прыжок каждые 10-14 тиков без условия.
 * На Matrix/Intave это палится: игрок прыгает когда никого рядом нет.
 *
 * Теперь: прыгаем только когда рядом цель в радиусе.
 * Плюс рандом задержки по профилю.
 */
public class CriticalsModule extends CheatModule {
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final NumberSetting triggerRange = add(new NumberSetting("Trigger range", 3.5, 2.0, 5.0, 0.1));

    private static final Random RNG = new Random();
    private int cooldown;

    public CriticalsModule() {
        super("Criticals", "Force critical hits via real jump", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (cooldown > 0) { cooldown--; return; }

        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null) return;

        if (!player.isOnGround()) return;
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;
        if (player.getVehicle() != null || player.getAbilities().flying) return;
        if (player.input.playerInput.jump()) return;

        // === Прыгаем только если рядом есть цель ===
        boolean hasTarget = false;
        double rangeSq = triggerRange.get() * triggerRange.get();
        for (Entity e : EntityCache.living()) {
            if (e == player) continue;
            if (e instanceof PlayerEntity && !players.get()) continue;
            if (!(e instanceof PlayerEntity) && !mobs.get()) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (e.squaredDistanceTo(player) <= rangeSq) {
                hasTarget = true;
                break;
            }
        }
        if (!hasTarget) return;

        player.jump();

        // === Задержка по профилю античита ===
        int base = switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 14 + RNG.nextInt(6);
            case SPOOKYTIME -> 12 + RNG.nextInt(5);
            case VULCAN, MATRIX -> 10 + RNG.nextInt(4);
            default -> 8 + RNG.nextInt(3);
        };
        cooldown = base;
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    public static void prep(PlayerEntity player) {
        // no-op
    }
}