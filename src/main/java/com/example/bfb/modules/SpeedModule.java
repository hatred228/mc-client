package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * Speed — ground speed boost через addVelocity (safe).
 *
 * РАНЬШЕ: setVelocity(wish * 0.26, ...) — мгновенная смена. Flag на Intave/Vulcan.
 *
 * ТЕПЕРЬ: addVelocity микрошагом с рандомом. Скорость набирается за 2-4 тика,
 * как если бы ты ускорялся от sprint-jump.
 */
public class SpeedModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Strafe", "Strafe", "Bhop"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.15, 1.0, 1.4, 0.05));
    public final BooleanSetting onlyMoving = add(new BooleanSetting("Only moving", true));
    public final BooleanSetting randomDelay = add(new BooleanSetting("Random delay", true));

    private static final Random RNG = new Random();
    private int cooldown;

    public SpeedModule() {
        super("Speed", "Ground speed boost", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (cooldown > 0) { cooldown--; return; }

        var client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (!player.isOnGround()) return;
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;
        if (player.getAbilities().flying || player.getVehicle() != null || player.isSpectator()) return;

        double[] wish = CombatUtil.wish(player);
        if (wish[0] == 0 && wish[1] == 0) {
            if (onlyMoving.get()) return;
        }

        // === скорость с учётом профиля античита ===
        double base = 0.045 * speed.get();
        double cap = switch (CombatController.mode()) {
            case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 0.22;   // <10% над ванилой
            case SPOOKYTIME -> 0.24;
            case VULCAN, MATRIX -> 0.26;
            default -> 0.30;
        };
        double boost = Math.min(cap, base);

        // только на земле и не постоянно — раз в 2-4 тика
        var velocity = player.getVelocity();
        double horizontal = Math.hypot(velocity.x, velocity.z);

        if (horizontal < cap) {
            // addVelocity в направлении движения
            double dx = wish[0] * boost;
            double dz = wish[1] * boost;
            player.addVelocity(dx, 0, dz);
        }

        // == Bhop — прыжок при движении вперёд ==
        if (mode.is("Bhop") && player.isOnGround() && player.forwardSpeed > 0.1f) {
            player.jump();
        }

        cooldown = randomDelay.get() ? 1 + RNG.nextInt(2) : 1;
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }
}