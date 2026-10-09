package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class CombatUtil {
    private CombatUtil() {}

    public static boolean accept(Entity entity, PlayerEntity self, boolean players, boolean mobs, boolean animals) {
        if (entity == self || !(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        if (entity.isSpectator()) return false;
        if (entity instanceof PlayerEntity) return players;
        if (entity instanceof PassiveEntity) return animals;
        if (entity instanceof HostileEntity || entity instanceof MobEntity) return mobs;
        return false;
    }

    /**
     * Угол между silent-yaw и целью.
     * Если silent rotation активна — читаем её, иначе камеру.
     * Это ключевой метод: без него KillAura/AutoClicker/AttackAura не видят silent aim.
     */
    public static float angle(Entity player, Entity target) {
        float yaw = RotationManager.isActive()
                ? RotationManager.getYaw()
                : player.getYaw();
        return Math.abs(wrap(yawTo(player, target) - yaw));
    }

    /** Только pitch-компонент. */
    public static float pitchAngle(Entity player, Entity target) {
        float pitch = RotationManager.isActive()
                ? RotationManager.getPitch()
                : player.getPitch();
        return Math.abs(pitchTo(player, target) - pitch);
    }

    /** Комбинированный угол: sqrt(dYaw² + dPitch²). */
    public static float fullAngle(Entity player, Entity target) {
        float yaw = RotationManager.isActive() ? RotationManager.getYaw() : player.getYaw();
        float pitch = RotationManager.isActive() ? RotationManager.getPitch() : player.getPitch();
        float dy = Math.abs(wrap(yawTo(player, target) - yaw));
        float dp = Math.abs(pitchTo(player, target) - pitch);
        return (float) Math.hypot(dy, dp);
    }

    public static void look(ClientPlayerEntity player, Entity target, float smooth) {
        float yaw = yawTo(player, target);
        float pitch = pitchTo(player, target);
        float amount = Math.max(0.05f, Math.min(1f, smooth));
        player.setYaw(player.getYaw() + wrap(yaw - player.getYaw()) * amount);
        player.setPitch(player.getPitch() + (pitch - player.getPitch()) * amount);
        player.setHeadYaw(player.getYaw());
        player.setBodyYaw(player.getYaw());
        syncLook(player);
    }

    public static boolean inFov(Entity player, Entity target, float fov) {
        float half = fov * 0.5f;
        return Math.abs(wrap(yawTo(player, target) - player.getYaw())) <= half
                && Math.abs(pitchTo(player, target) - player.getPitch()) <= half;
    }

    public static Box aimBox(Entity target) {
        return target.getBoundingBox().expand(com.example.bfb.modules.HitBoxesModule.extra());
    }

    public static Vec3d closest(Box box, Vec3d point) {
        return new Vec3d(
                MathHelper.clamp(point.x, box.minX, box.maxX),
                MathHelper.clamp(point.y, box.minY, box.maxY),
                MathHelper.clamp(point.z, box.minZ, box.maxZ));
    }

    public static boolean rayHits(PlayerEntity player, Entity target, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1f).multiply(range));
        Box box = aimBox(target);
        return box.contains(eye) || box.raycast(eye, end).isPresent();
    }

    public static Vec3d aimPoint(PlayerEntity player, Entity target, double range) {
        Box box = aimBox(target);
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1f).multiply(range));
        return box.raycast(eye, end).orElseGet(() -> closest(box, eye));
    }

    public static boolean canSee(PlayerEntity player, Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return true;
        Vec3d eye = player.getEyePos();
        Vec3d aim = aimPoint(player, target, 8);
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                eye, aim, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    public static void lookAt(ClientPlayerEntity player, Vec3d point, float smooth) {
        double dx = point.x - player.getX();
        double dy = point.y - player.getEyeY();
        double dz = point.z - player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, flat)));
        float amount = Math.max(0.05f, Math.min(1f, smooth));
        player.setYaw(player.getYaw() + wrap(yaw - player.getYaw()) * amount);
        player.setPitch(player.getPitch() + (pitch - player.getPitch()) * amount);
        player.setHeadYaw(player.getYaw());
        player.setBodyYaw(player.getYaw());
        syncLook(player);
    }

    public static void syncLook(ClientPlayerEntity player) {
        if (player.networkHandler == null) return;
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                player.getYaw(), player.getPitch(), player.isOnGround(), player.horizontalCollision));
    }

    /** Silent rotation через AnticheatBypass (пишет в RotationManager). */
    public static void silentLookAt(ClientPlayerEntity player, Vec3d point) {
        float[] rot = AnticheatBypass.lookAt(player.getEyePos(), point);
        AnticheatBypass.snapLook(player, rot[0], rot[1]);
    }

    /** Плавное silent-наведение с шагом. */
    public static void gradualLookAt(ClientPlayerEntity player, Vec3d point, float maxStep) {
        float[] rot = AnticheatBypass.lookAt(player.getEyePos(), point);
        AnticheatBypass.gradualLook(player, rot[0], rot[1], maxStep);
    }

    public static void syncSlot(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot > 8 || player.networkHandler == null) return;
        if (player.getInventory().getSelectedSlot() != slot) player.getInventory().setSelectedSlot(slot);
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    /** Обычная атака. Без reach spoof. */
    public static void attack(Entity target) {
        attack(target, false);
    }

    /**
     * Атака с опциональным reach bypass.
     *
     * spoof = false — обычная атака. Всегда работает.
     * spoof = true  — position offset 0.08 к цели перед attackEntity.
     *                 Расширяет серверный reach с 3.0 до ~3.5.
     *                 Может палиться на Intave/Verus/Matrix при стоянии на месте.
     *                 Не палится при движении (offset маскируется дельтой движения).
     *
     * Работает только если дистанция в 2.95..3.5 — иначе spoof не нужен или бесполезен.
     * Миксин ClientPlayNetworkHandlerMixin подменяет только rotation, position уходит как есть.
     */
    public static void attack(Entity target, boolean spoof) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null || target == null) return;

        double dist = player.distanceTo(target);
        if (dist > 3.5) return;

        if (spoof && dist > 2.95 && player.networkHandler != null) {
            Vec3d playerPos = player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();
            Vec3d dir = targetPos.subtract(playerPos);
            double len = dir.length();
            if (len > 1.0e-4) {
                double offset = 0.08;
                Vec3d off = dir.multiply(offset / len);
                double nx = player.getX() + off.x;
                double nz = player.getZ() + off.z;
                player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        nx, player.getY(), nz, player.isOnGround(), player.horizontalCollision));
            }
        }

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
    }

    public static double[] wish(LivingEntity player) {
        float yaw = (float) Math.toRadians(player.getYaw());
        float forward = player.forwardSpeed;
        float strafe = player.sidewaysSpeed;
        if (forward == 0 && strafe == 0) return new double[]{0, 0};
        float len = (float) Math.sqrt(forward * forward + strafe * strafe);
        forward /= len;
        strafe /= len;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new double[]{forward * -sin + strafe * cos, forward * cos + strafe * sin};
    }

    private static float yawTo(Entity player, Entity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    private static float pitchTo(Entity player, Entity target) {
        double dx = target.getX() - player.getX();
        double dy = (target.getY() + target.getHeight() * 0.5) - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.toDegrees(Math.atan2(dy, flat)));
    }

    private static float wrap(float delta) {
        delta %= 360f;
        if (delta >= 180f) delta -= 360f;
        if (delta < -180f) delta += 360f;
        return delta;
    }
}