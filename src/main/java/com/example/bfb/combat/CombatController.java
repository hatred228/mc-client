package com.example.bfb.combat;

import com.example.bfb.AnticheatBypass;
import com.example.bfb.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Универсальный контроллер combat-байпасса.
 * Читает текущий CombatMode + CombatProfile, применяет rotation
 * и проверяет допустимость атаки.
 */
public final class CombatController {

    private static CombatMode mode = CombatMode.VANILLA;
    private static CombatProfile profile = CombatProfile.forMode(mode);

    private static float yawVel = 0f;
    private static float pitchVel = 0f;

    private CombatController() {}

    public static void setMode(CombatMode m) {
        mode = m;
        profile = CombatProfile.forMode(m);
        yawVel = 0f;
        pitchVel = 0f;
    }

    public static CombatMode mode() { return mode; }
    public static CombatProfile profile() { return profile; }

    /**
     * Вращение к цели с байпассом под текущий античит.
     */
    public static boolean rotateTo(ClientPlayerEntity player, Vec3d target, float dt) {
        if (player == null) return false;

        float idealYaw = (float) (Math.toDegrees(Math.atan2(
                target.z - player.getZ(), target.x - player.getX())) - 90.0);
        float idealPitch = (float) (-Math.toDegrees(Math.atan2(
                target.y - player.getEyeY(),
                Math.hypot(target.x - player.getX(), target.z - player.getZ()))));

        float stiffness = profile.rotationSmooth() * 12f;
        float damping = 2f * (float) Math.sqrt(stiffness) * 0.9f;

        float curYaw = RotationManager.isActive() ? RotationManager.getYaw() : player.getYaw();
        float curPitch = RotationManager.isActive() ? RotationManager.getPitch() : player.getPitch();

        float yawErr = wrap(idealYaw - curYaw);
        float pitchErr = idealPitch - curPitch;

        yawVel += (yawErr * stiffness - yawVel * damping) * dt;
        pitchVel += (pitchErr * stiffness - pitchVel * damping) * dt;

        float maxVel = profile.rotationSpeed() * 8f;
        yawVel = MathHelper.clamp(yawVel, -maxVel, maxVel);
        pitchVel = MathHelper.clamp(pitchVel, -maxVel * 0.7f, maxVel * 0.7f);

        float newYaw = curYaw + yawVel * dt;
        float newPitch = MathHelper.clamp(curPitch + pitchVel * dt, -90f, 90f);

        if (profile.rotationJitter() > 0f) {
            long t = System.currentTimeMillis();
            newYaw += (float) Math.sin(t / 47.0) * profile.rotationJitter();
            newPitch += (float) Math.cos(t / 53.0) * profile.rotationJitter() * 0.6f;
        }

        if (profile.gcdTolerance() < 0.5f) {
            float gcd = gcd(player);
            newYaw = Math.round(newYaw / gcd) * gcd;
            newPitch = Math.round(newPitch / gcd) * gcd;
        }

        if (profile.silentRotation()) {
            RotationManager.setAndSend(newYaw, newPitch, 2);
        } else {
            player.setYaw(newYaw);
            player.setPitch(newPitch);
        }
        return true;
    }

    /**
     * Проверка: можно ли атаковать цель под текущим античитом.
     */
    public static boolean canAttack(ClientPlayerEntity player, Entity target) {
        if (player == null || target == null) return false;

        double dist = player.distanceTo(target);
        if (dist > 3.0 + profile.hitboxExpand()) return false;

        float yaw = RotationManager.isActive() ? RotationManager.getYaw() : player.getYaw();
        float pitch = RotationManager.isActive() ? RotationManager.getPitch() : player.getPitch();
        float targetYaw = (float) (Math.toDegrees(Math.atan2(
                target.getZ() - player.getZ(), target.getX() - player.getX())) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(
                target.getY() + target.getHeight() * 0.5 - player.getEyeY(),
                Math.hypot(target.getX() - player.getX(), target.getZ() - player.getZ()))));

        float yawDiff = Math.abs(wrap(targetYaw - yaw));
        float pitchDiff = Math.abs(targetPitch - pitch);
        float totalAngle = (float) Math.hypot(yawDiff, pitchDiff);

        if (totalAngle > profile.hitAngleThreshold()) return false;

        if (profile.requireLineOfSight() && !AnticheatBypass.canSee(player,
                net.minecraft.util.math.BlockPos.ofFloored(target.getEntityPos()))) {
            return false;
        }

        if (profile.onlyWhenMoving()) {
            double speed = Math.hypot(player.getVelocity().x, player.getVelocity().z);
            if (speed < 0.02) return false;
        }

        return true;
    }

    public static int attackDelay() {
        int min = profile.attackDelayMin();
        int max = profile.attackDelayMax();
        return min + (int) (Math.random() * (max - min + 1));
    }

    private static float gcd(ClientPlayerEntity player) {
        var mc = com.example.bfb.BfbMod.getClient();
        double sens = mc.options.getMouseSensitivity().getValue();
        float f = (float) (sens * 0.6 + 0.2);
        return Math.max(1.0e-5f, f * f * f * 1.2f);
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}