package com.example.bfb;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public final class ProjectileSim {
    private ProjectileSim() {
    }

    public static final class Path {
        public final List<Vec3d> points = new ArrayList<>();
        public boolean entity;
    }

    public static Vec3d motion(float yaw, float pitch, float speed, ClientPlayerEntity player) {
        float yr = yaw * ((float) Math.PI / 180f);
        float pr = pitch * ((float) Math.PI / 180f);
        double x = -MathHelper.sin(yr) * MathHelper.cos(pr);
        double y = -MathHelper.sin(pr);
        double z = MathHelper.cos(yr) * MathHelper.cos(pr);
        Vec3d dir = new Vec3d(x, y, z).normalize().multiply(speed);
        Vec3d vel = player.getVelocity();
        return dir.add(vel.x, player.isOnGround() ? 0 : vel.y, vel.z);
    }

    public static Path trace(ClientPlayerEntity player, float yaw, float pitch, float speed, float gravity) {
        Path path = new Path();
        Vec3d pos = player.getEyePos();
        Vec3d motion = motion(yaw, pitch, speed, player);
        var world = player.getEntityWorld();
        path.points.add(pos);
        for (int i = 0; i < 140; i++) {
            Vec3d next = pos.add(motion);
            BlockHitResult block = world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            Vec3d end = next;
            if (block.getType() == HitResult.Type.BLOCK) end = block.getPos();
            Box swept = new Box(pos, end).expand(0.35);
            Entity struck = null;
            double best = Double.MAX_VALUE;
            for (Entity entity : world.getOtherEntities(player, swept, candidate -> candidate instanceof LivingEntity living && living.isAlive() && !candidate.isSpectator())) {
                var hit = entity.getBoundingBox().expand(0.3).raycast(pos, end);
                if (hit.isEmpty()) continue;
                double dist = pos.squaredDistanceTo(hit.get());
                if (dist < best) {
                    best = dist;
                    struck = entity;
                    end = hit.get();
                }
            }
            path.points.add(end);
            if (struck != null) {
                path.entity = true;
                break;
            }
            if (block.getType() == HitResult.Type.BLOCK || pos.y < world.getBottomY()) break;
            pos = next;
            motion = motion.multiply(0.99).add(0, -gravity, 0);
        }
        return path;
    }

        public static Vec3d aimPoint(ClientPlayerEntity player, Entity target, float speed, float gravity, float pitchOffset) {
        // целимся в "грудь" (~62% высоты тела), а не в центр хитбокса
        net.minecraft.util.math.Box box = target.getBoundingBox();
        double aimHeight = box.minY + (box.maxY - box.minY) * 0.62;
        Vec3d base = new Vec3d(target.getX(), aimHeight, target.getZ());

        // предикт: время полёта стрелы по прямой / скорость
        double flatDist = Math.hypot(target.getX() - player.getX(), target.getZ() - player.getZ());
        float ticks = (float) Math.min(40.0, flatDist / Math.max(0.5, speed));
        Vec3d vel = target.getVelocity();
        // вертикаль предиктим слабее: серверная инерция у целей "глючит"
        Vec3d center = base.add(vel.x * ticks, vel.y * ticks * 0.35, vel.z * ticks);

        float yaw = (float) (Math.toDegrees(Math.atan2(center.z - player.getZ(), center.x - player.getX())) - 90.0);
        float pitch = bestPitch(player, yaw, speed, gravity, center);
        float look = MathHelper.clamp(pitch - pitchOffset, -90f, 90f);
        return player.getEyePos().add(motion(yaw, look, 10f, player).normalize().multiply(10));
    }

    private static float bestPitch(ClientPlayerEntity player, float yaw, float speed, float gravity, Vec3d center) {
        float bestPitch = 0f;
        double best = Double.MAX_VALUE;
        for (int step = 0; step <= 36; step++) {
            float pitch = -89f + step * (178f / 36f);
            double score = miss(player, yaw, pitch, speed, gravity, center);
            if (score < best) {
                best = score;
                bestPitch = pitch;
            }
        }
        float base = bestPitch;
        for (int step = -6; step <= 6; step++) {
            float pitch = base + step * 0.6f;
            double score = miss(player, yaw, pitch, speed, gravity, center);
            if (score < best) {
                best = score;
                bestPitch = pitch;
            }
        }
        return bestPitch;
    }

    private static double miss(ClientPlayerEntity player, float yaw, float pitch, float speed, float gravity, Vec3d center) {
        Vec3d pos = player.getEyePos();
        Vec3d motion = motion(yaw, pitch, speed, player);
        double best = Double.MAX_VALUE;
        var world = player.getEntityWorld();
        for (int i = 0; i < 80; i++) {
            Vec3d next = pos.add(motion);
            Box box = new Box(center, center).expand(0.4, 0.9, 0.4);
            if (box.raycast(pos, next).isPresent()) return 0;
            best = Math.min(best, next.squaredDistanceTo(center));
            BlockHitResult block = world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (block.getType() == HitResult.Type.BLOCK) break;
            pos = next;
            motion = motion.multiply(0.99).add(0, -gravity, 0);
        }
        return best;
    }
}
