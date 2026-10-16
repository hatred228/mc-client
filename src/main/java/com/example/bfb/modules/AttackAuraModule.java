package com.example.bfb.modules;

import com.example.bfb.AnticheatBypass;
import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.EntityCache;
import com.example.bfb.FriendManager;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * AttackAura — Legit / Rage.
 *
 * Legit:
 *   - камера реально крутится
 *   - ДВОЙНОЙ lerp + smoothstep — максимально плавное движение
 *   - ease-in / cruise / ease-out на одной кривой
 */
public class AttackAuraModule extends CheatModule {
    public static Entity target;

    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));

    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 3.5, 0.05));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    // Legit
    public final NumberSetting aimSpeed = add(new NumberSetting("Aim speed", 0.35, 0.15, 0.70, 0.05));
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 0.55, 0.30, 0.90, 0.05));
    public final NumberSetting attackAngle = add(new NumberSetting("Attack angle", 8.0, 3.0, 30.0, 1.0));
    public final BooleanSetting attackInAir = add(new BooleanSetting("Attack in air", true));

    // Rage
    public final NumberSetting rageTicks = add(new NumberSetting("Rage ticks", 4, 1, 8, 1));
    public final NumberSetting rageDelay = add(new NumberSetting("Rage delay", 3, 2, 6, 1));
    public final BooleanSetting visual = add(new BooleanSetting("Show rotation (F5)", true));

    private int attackCooldown;
    private boolean rotationStarted;

    public AttackAuraModule() {
        super("AttackAura", "Auto attacks — Legit / Rage", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        target = null;
        rotationStarted = false;
        RotationManager.release();
    }

    @Override
    public void onTick() {
        if (attackCooldown > 0) attackCooldown--;

        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null) return;

        Entity best = pickTarget(player);
        if (best == null) {
            if (target != null) RotationManager.release();
            target = null;
            rotationStarted = false;
            return;
        }

        target = best;

        if (mode.is("Legit")) tickLegit(player, best);
        else tickRage(player, best);
    }

    // ============================================================
    // LEGIT — камера крутится с двойным сглаживанием
    // ============================================================

    private void tickLegit(ClientPlayerEntity player, Entity best) {
        Vec3d aim = best.getBoundingBox().getCenter();

        float idealYaw = (float) (Math.toDegrees(Math.atan2(
                aim.z - player.getZ(), aim.x - player.getX())) - 90.0);
        float idealPitch = (float) (-Math.toDegrees(Math.atan2(
                aim.y - player.getEyeY(),
                Math.hypot(aim.x - player.getX(), aim.z - player.getZ()))));

        float curYaw = player.getYaw();
        float curPitch = player.getPitch();

        float dy = wrap(idealYaw - curYaw);
        float dp = idealPitch - curPitch;

        float dist = (float) Math.hypot(dy, dp);
        if (dist < 0.05f) {
            player.setYaw(idealYaw);
            player.setPitch(MathHelper.clamp(idealPitch, -90f, 90f));
            player.setHeadYaw(idealYaw);
            player.setBodyYaw(idealYaw);
            tickLegitAttack(player, best, idealYaw, idealPitch);
            return;
        }

        float k = aimSpeed.getFloat();
        float sm = smoothness.getFloat();     // 0.55 default — чем меньше, тем плавнее

        // smoothstep по дистанции
        float t = Math.min(1f, dist / 40f);
        float smooth = t * t * (3f - 2f * t);

        float factor = 0.15f + 0.85f * smooth;
        float easedK = k * factor;

        // первый lerp — базовый шаг
        float stepYaw = dy * easedK;
        float stepPitch = dp * easedK;

        // второй lerp — сглаживание самого шага
        stepYaw *= sm;
        stepPitch *= sm;

        float newYaw = curYaw + stepYaw;
        float newPitch = MathHelper.clamp(curPitch + stepPitch, -90f, 90f);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
        player.setHeadYaw(newYaw);
        player.setBodyYaw(newYaw);

        tickLegitAttack(player, best, idealYaw, idealPitch);
    }

    private void tickLegitAttack(ClientPlayerEntity player, Entity best,
                                  float idealYaw, float idealPitch) {
        if (!attackInAir.get() && !player.isOnGround()) return;
        if (attackCooldown > 0) return;
        if (player.distanceTo(best) > range.get()) return;
        if (player.getAttackCooldownProgress(0f) < 0.95f) return;

        float angleToTarget = (float) Math.hypot(
                Math.abs(wrap(idealYaw - player.getYaw())),
                Math.abs(idealPitch - player.getPitch()));

        if (angleToTarget > attackAngle.getFloat()) return;

        CombatUtil.attack(best, false);
        attackCooldown = 2;
    }

    // ============================================================
    // RAGE
    // ============================================================

    private void tickRage(ClientPlayerEntity player, Entity best) {
        Vec3d aim = best.getBoundingBox().getCenter();
        float[] ideal = AnticheatBypass.lookAt(player.getEyePos(), aim);

        float gcd = RotationManager.gcd();
        float yaw = Math.round(ideal[0] / gcd) * gcd;
        float pitch = Math.round(ideal[1] / gcd) * gcd;

        if (!rotationStarted) {
            RotationManager.startRotation(yaw, pitch, rageTicks.getInt());
            rotationStarted = true;
        } else {
            RotationManager.retarget(yaw, pitch);
        }

        if (visual.get()) {
            player.setBodyYaw(RotationManager.getYaw());
            player.setHeadYaw(RotationManager.getYaw());
        }

        if (attackCooldown > 0) return;
        if (player.distanceTo(best) > range.get()) return;
        if (player.getAttackCooldownProgress(0f) < 0.95f) return;
        if (RotationManager.isTrajectoryActive() && RotationManager.progress() < 0.9f) return;

        CombatUtil.attack(best, false);
        attackCooldown = rageDelay.getInt();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Entity pickTarget(ClientPlayerEntity player) {
        Entity best = null;
        double bestD = range.get() * range.get();
        for (Entity e : EntityCache.living()) {
            if (!CombatUtil.accept(e, player, players.get(), mobs.get(), animals.get())) continue;
            if (FriendManager.isFriend(e)) continue;   // <-- FIX: не бьём друзей
            double d = e.squaredDistanceTo(player);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}