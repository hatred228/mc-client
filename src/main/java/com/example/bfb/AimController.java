package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class AimController {
    private static final Random RNG = new Random();

    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static boolean hasTarget = false;

    private static float aimYaw = Float.NaN;
    private static float aimPitch = Float.NaN;

    private static float integralYaw = 0f;
    private static float integralPitch = 0f;
    private static float lastErrorYaw = 0f;
    private static float lastErrorPitch = 0f;

    private static float bezierStartYaw = 0f;
    private static float bezierStartPitch = 0f;
    private static float bezierMidYaw = 0f;
    private static float bezierMidPitch = 0f;
    private static float bezierProgress = 0f;
    private static boolean bezierActive = false;
    private static float bezierDuration = 0f;

    private static float tremorYaw = 0f;
    private static float tremorPitch = 0f;
    private static float tremorTargetYaw = 0f;
    private static float tremorTargetPitch = 0f;
    private static int tremorRefresh = 0;

    private static float driftPhase = 0f;
    private static float driftSpeedYaw = 0.7f;
    private static float driftSpeedPitch = 0.5f;
    private static float driftAmpYaw = 0f;
    private static float driftAmpPitch = 0f;

    private static long lastFrameNanos = 0L;
    private static long nextSyncMs = 0L;

    private static float kp = 0.12f;
    private static float ki = 0.002f;
    private static float kd = 0.04f;
    private static float maxVel = 12f;
    private static float baseTremor = 0.10f;
    private static float gcdJitter = 1.0f;
    private static int syncJitter = 0;

    private AimController() {}

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float gcd() {
        MinecraftClient client = MinecraftClient.getInstance();
        double sens = client.options.getMouseSensitivity().getValue();
        float f = (float) (sens * 0.6 + 0.2);
        return Math.max(1.0e-5f, f * f * f * 1.2f) * (0.96f + RNG.nextFloat() * 0.08f) * gcdJitter;
    }

    private static float snap(float value, float gcd) {
        return Math.round(value / gcd) * gcd;
    }

    private static float gaussian() {
        double u1 = Math.max(1.0e-10, RNG.nextDouble());
        double u2 = RNG.nextDouble();
        return (float) (Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2));
    }

    private static void refreshTremor(float amp) {
        if (tremorRefresh-- > 0) return;
        tremorRefresh = 2 + RNG.nextInt(4);
        tremorTargetYaw = gaussian() * amp;
        tremorTargetPitch = gaussian() * amp * 0.6f;
    }

    private static float bezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }

    private static void startBezier(float toYaw, float toPitch) {
        if (Float.isNaN(aimYaw)) return;
        bezierStartYaw = aimYaw;
        bezierStartPitch = aimPitch;

        float dy = wrap(toYaw - aimYaw);
        float dp = toPitch - aimPitch;

        float overshootY = (RNG.nextFloat() - 0.5f) * Math.abs(dy) * 0.3f;
        float overshootP = (RNG.nextFloat() - 0.5f) * Math.abs(dp) * 0.3f;

        bezierMidYaw = aimYaw + dy * 0.5f + overshootY;
        bezierMidPitch = aimPitch + dp * 0.5f + overshootP;

        bezierProgress = 0f;
        bezierDuration = 0.12f + RNG.nextFloat() * 0.08f;
        bezierActive = true;

        integralYaw = 0f;
        integralPitch = 0f;
        lastErrorYaw = 0f;
        lastErrorPitch = 0f;
    }

    public static void setTarget(float yaw, float pitch, float speed, float tremor, int syncJitterMs) {
        if (Float.isNaN(aimYaw)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                aimYaw = client.player.getYaw();
                aimPitch = client.player.getPitch();
            }
        }
        targetYaw = yaw;
        targetPitch = pitch;

        if (!hasTarget) startBezier(yaw, pitch);
        hasTarget = true;

        kp = 0.06f + speed * 0.18f;
        ki = 0.001f + speed * 0.003f;
        kd = 0.02f + speed * 0.06f;
        maxVel = 6f + speed * 14f;
        baseTremor = tremor;
        syncJitter = syncJitterMs;

        driftPhase = RNG.nextFloat() * 6.28f;
        driftSpeedYaw = 0.4f + RNG.nextFloat() * 0.6f;
        driftSpeedPitch = 0.3f + RNG.nextFloat() * 0.5f;
        driftAmpYaw = 0.15f + RNG.nextFloat() * 0.35f;
        driftAmpPitch = 0.08f + RNG.nextFloat() * 0.18f;
        gcdJitter = 0.98f + RNG.nextFloat() * 0.04f;
    }

    public static void clearTarget() {
        hasTarget = false;
        bezierActive = false;
        RotationManager.release();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            aimYaw = client.player.getYaw();
            aimPitch = client.player.getPitch();
        }
    }

    public static void rageTo(ClientPlayerEntity player, float yaw, float pitch) {
        if (player == null) return;
        RotationManager.set(yaw, pitch);
        aimYaw = yaw;
        aimPitch = pitch;
    }

    public static void updateFrame() {
        if (!hasTarget) return;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) { lastFrameNanos = now; return; }
        float dt = (now - lastFrameNanos) / 1.0e9f;
        lastFrameNanos = now;
        if (dt <= 0f) dt = 1f / 60f;
        if (dt > 0.1f) dt = 0.1f;

        if (Float.isNaN(aimYaw)) { aimYaw = player.getYaw(); aimPitch = player.getPitch(); }

        if (bezierActive) {
            bezierProgress += dt / bezierDuration;
            if (bezierProgress >= 1f) {
                bezierProgress = 1f;
                bezierActive = false;
                aimYaw = targetYaw;
                aimPitch = targetPitch;
            } else {
                float t = bezierProgress;
                float eased = t * t * (3f - 2f * t);
                aimYaw = bezier(bezierStartYaw, bezierMidYaw, bezierMidYaw, targetYaw, eased);
                aimPitch = bezier(bezierStartPitch, bezierMidPitch, bezierMidPitch, targetPitch, eased);
            }
        }

        if (!bezierActive) {
            float errorYaw = wrap(targetYaw - aimYaw);
            float errorPitch = targetPitch - aimPitch;

            integralYaw += errorYaw * dt;
            integralPitch += errorPitch * dt;
            integralYaw = MathHelper.clamp(integralYaw, -30f, 30f);
            integralPitch = MathHelper.clamp(integralPitch, -30f, 30f);

            float derivYaw = (errorYaw - lastErrorYaw) / dt;
            float derivPitch = (errorPitch - lastErrorPitch) / dt;
            lastErrorYaw = errorYaw;
            lastErrorPitch = errorPitch;

            float outputYaw = kp * errorYaw + ki * integralYaw + kd * derivYaw;
            float outputPitch = kp * errorPitch + ki * integralPitch + kd * derivPitch;

            outputYaw = MathHelper.clamp(outputYaw, -maxVel, maxVel);
            outputPitch = MathHelper.clamp(outputPitch, -maxVel, maxVel);

            aimYaw += outputYaw * dt * 60f;
            aimPitch += outputPitch * dt * 60f;
        }

        float playerSpeed = (float) Math.hypot(player.getVelocity().x, player.getVelocity().z);
        float speedFactor = MathHelper.clamp(playerSpeed / 0.15f, 0.1f, 1f);
        refreshTremor(baseTremor * speedFactor);

        float tremorFactor = 1f - (float) Math.exp(-8f * dt);
        tremorYaw += (tremorTargetYaw - tremorYaw) * tremorFactor;
        tremorPitch += (tremorTargetPitch - tremorPitch) * tremorFactor;

        driftPhase += dt * 2.2f;
        float driftY = (float) Math.sin(driftPhase * driftSpeedYaw) * driftAmpYaw;
        float driftP = (float) Math.sin(driftPhase * driftSpeedPitch + 1.3f) * driftAmpPitch;

        float fy = aimYaw + tremorYaw + driftY;
        float fp = MathHelper.clamp(aimPitch + tremorPitch + driftP, -90f, 90f);

        float g = gcd();
        fy = snap(fy, g);
        fp = snap(fp, g);

        // silent — пишем в менеджер, не в камеру
        RotationManager.set(fy, fp);
    }

    public static void syncServer(ClientPlayerEntity player) {
        // no-op: rotation уходит через ClientPlayNetworkHandlerMixin
    }

    public static float aimYaw() { return Float.isNaN(aimYaw) ? 0f : aimYaw; }
    public static float aimPitch() { return Float.isNaN(aimPitch) ? 0f : aimPitch; }
    public static boolean hasAim() { return !Float.isNaN(aimYaw); }
    public static boolean hasTarget() { return hasTarget; }

    public static void reset() {
        aimYaw = Float.NaN; aimPitch = Float.NaN;
        integralYaw = 0f; integralPitch = 0f;
        lastErrorYaw = 0f; lastErrorPitch = 0f;
        bezierActive = false; bezierProgress = 0f;
        tremorYaw = 0f; tremorPitch = 0f;
        tremorTargetYaw = 0f; tremorTargetPitch = 0f;
        tremorRefresh = 0; driftPhase = 0f;
        lastFrameNanos = 0L; nextSyncMs = 0L;
        hasTarget = false;
    }
}