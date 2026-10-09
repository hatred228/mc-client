package com.example.bfb;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class ProjectileAim {
    private static final Random RNG = new Random();

    private ProjectileAim() {}

    /** Итеративный предикт — цель движется во время полёта снаряда. */
    public static float[] solve(ClientPlayerEntity player, Entity target, float speed, float gravity) {
        double aimHeight = target.getY() + target.getHeight() * 0.62;
        Vec3d eye = player.getEyePos();
        double dx = target.getX() - eye.x;
        double dz = target.getZ() - eye.z;
        double dy = aimHeight - eye.y;
        double flatDist = Math.hypot(dx, dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = 0f;

        // kinetic solver — 6 итераций
        for (int iter = 0; iter < 6; iter++) {
            double t = flatDist / Math.max(0.5, speed * Math.cos(Math.toRadians(pitch)));
            Vec3d vel = target.getVelocity();
            double px = target.getX() + vel.x * t;
            double pz = target.getZ() + vel.z * t;
            double py = aimHeight + vel.y * t * 0.6;

            dx = px - eye.x;
            dz = pz - eye.z;
            dy = py - eye.y;
            flatDist = Math.hypot(dx, dz);
            yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);

            double v2 = speed * speed;
            double root = v2 * v2 - gravity * (gravity * dx * dx + gravity * dz * dz + 2 * dy * v2);
            if (root < 0) {
                pitch = -MathHelper.clamp((float) Math.toDegrees(Math.atan2(dy, flatDist)), -90f, 90f);
                break;
            }
            pitch = (float) -Math.toDegrees(Math.atan((v2 - Math.sqrt(root)) / (gravity * flatDist)));
        }

        return new float[]{yaw, pitch};
    }

    /** Микро-рандом для андекта (не палит идеальную наводку). */
    public static float[] jitter(float[] rot, float amount) {
        return new float[]{
                rot[0] + (RNG.nextFloat() - 0.5f) * 2f * amount,
                rot[1] + (RNG.nextFloat() - 0.5f) * 2f * amount
        };
    }
}