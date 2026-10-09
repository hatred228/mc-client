package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Random;

/**
 * Обёртки над RotationManager. Публичный API сохранён для CombatUtil.
 * Больше не шлёт пакеты сам — это делает ClientPlayNetworkHandlerMixin.
 */
public final class AnticheatBypass {

    private static final Random RNG = new Random();

    private static float wobblePhase = 0f;
    private static int missTick = 0;
    private static float missYawOffset = 0f;
    private static float missPitchOffset = 0f;

    private AnticheatBypass() {}

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float curYaw(ClientPlayerEntity p) {
        return RotationManager.isActive() ? RotationManager.getYaw() : p.getYaw();
    }

    private static float curPitch(ClientPlayerEntity p) {
        return RotationManager.isActive() ? RotationManager.getPitch() : p.getPitch();
    }

    public static void silentLook(ClientPlayerEntity player, float yaw, float pitch) {
        if (player == null) return;
        RotationManager.set(yaw, pitch);
    }

    public static void snapLook(ClientPlayerEntity player, float yaw, float pitch) {
        silentLook(player, yaw, pitch);
    }

    public static void smoothLook(ClientPlayerEntity player, float targetYaw, float targetPitch) {
        float fy = curYaw(player);
        float fp = curPitch(player);
        float dy = wrap(targetYaw - fy);
        float dp = targetPitch - fp;
        float dist = (float) Math.hypot(dy, dp);
        if (dist < 15f) {
            silentLook(player, targetYaw, targetPitch);
            return;
        }
        RotationManager.set(fy + dy * 0.5f, fp + dp * 0.5f, 1);
    }

    public static void gradualLook(ClientPlayerEntity player, float targetYaw, float targetPitch, float maxStep) {
        float fy = curYaw(player);
        float fp = curPitch(player);
        float dy = wrap(targetYaw - fy);
        float dp = targetPitch - fp;
        float dist = (float) Math.hypot(dy, dp);
        if (dist < 0.05f) return;
        float step = Math.min(maxStep, dist);
        step *= 0.85f + RNG.nextFloat() * 0.3f;
        float ny = fy + (dy / dist) * step;
        float np = fp + (dp / dist) * step;
        RotationManager.set(ny, np);
    }

    public static void humanTrack(ClientPlayerEntity player, float targetYaw, float targetPitch,
                                  float maxStep, float wobbleAmount, float missChance) {
        float fy = curYaw(player);
        float fp = curPitch(player);
        float dy = wrap(targetYaw - fy);
        float dp = targetPitch - fp;

        wobblePhase += 0.15f + RNG.nextFloat() * 0.1f;
        if (wobblePhase > (float) (Math.PI * 2)) wobblePhase -= (float) (Math.PI * 2);
        dy += (float) Math.sin(wobblePhase) * wobbleAmount;
        dp += (float) Math.cos(wobblePhase * 0.7f) * wobbleAmount * 0.5f;

        if (missTick > 0) {
            missTick--;
            dy += missYawOffset;
            dp += missPitchOffset;
        } else if (RNG.nextFloat() < missChance) {
            missTick = 2 + RNG.nextInt(3);
            missYawOffset = (RNG.nextFloat() - 0.5f) * 6f;
            missPitchOffset = (RNG.nextFloat() - 0.5f) * 4f;
        }

        float dist = (float) Math.hypot(dy, dp);
        if (dist < 0.05f) return;
        float step = Math.min(maxStep, dist);
        step *= 0.85f + RNG.nextFloat() * 0.3f;

        RotationManager.set(fy + (dy / dist) * step, fp + (dp / dist) * step);
    }

    public static void rageTrack(ClientPlayerEntity player, float yaw, float pitch) {
        snapLook(player, yaw, pitch);
    }

    public static float[] lookAt(Vec3d eye, Vec3d target) {
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0),
                (float) (-Math.toDegrees(Math.atan2(dy, flat)))
        };
    }

    public static boolean canSee(ClientPlayerEntity player, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        Vec3d eye = player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                eye, center, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        return hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(pos);
    }

    public static Vec3d aimPoint(ClientPlayerEntity player, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos);
        return new Vec3d(
                c.x + (RNG.nextDouble() - 0.5) * 0.2,
                c.y + (RNG.nextDouble() - 0.5) * 0.2,
                c.z + (RNG.nextDouble() - 0.5) * 0.2);
    }

    public static double randomReach() {
        return 2.95 + RNG.nextDouble() * 0.05;
    }

    public static void reset() {
        wobblePhase = 0f;
        missTick = 0;
        missYawOffset = 0f;
        missPitchOffset = 0f;
    }
}