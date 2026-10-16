package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class RotationManager {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final RotationTrajectory trajectory = new RotationTrajectory();
    private static float serverYaw;
    private static float serverPitch;
    private static boolean active;
    private static long lastUpdateMs;
    private static ClientPlayerEntity lastPlayer;

    private static int preAimTicks = 0;

    private RotationManager() {}

    public static void tick(ClientPlayerEntity player) {
        if (player == null) {
            if (lastPlayer != null) { clear(); lastPlayer = null; }
            return;
        }
        if (lastPlayer != player) { init(player); lastPlayer = player; }

        if (preAimTicks > 0) preAimTicks--;

        if (trajectory.isActive()) {
            trajectory.update();
            serverYaw = trajectory.getCurrentYaw();
            serverPitch = trajectory.getCurrentPitch();
            lastUpdateMs = System.currentTimeMillis();
            if (!trajectory.isActive()) active = true;
        }
    }

    public static void init(ClientPlayerEntity player) {
        serverYaw = player.getYaw();
        serverPitch = player.getPitch();
        active = false;
        trajectory.stop();
        lastUpdateMs = 0L;
        preAimTicks = 0;
    }

    /** Мгновенный — для старого API. */
    public static void set(float yaw, float pitch) {
        float fromYaw = active ? serverYaw : (mc.player != null ? mc.player.getYaw() : 0f);
        float fromPitch = active ? serverPitch : (mc.player != null ? mc.player.getPitch() : 0f);
        trajectory.start(fromYaw, fromPitch, yaw, pitch, 1);
        active = true;
    }

    public static void set(float yaw, float pitch, int holdForTicks) {
        set(yaw, pitch);
    }

    /** Плавный поворот за N тиков. */
    public static void startRotation(float yaw, float pitch, int ticks) {
        float fromYaw = active ? serverYaw : (mc.player != null ? mc.player.getYaw() : 0f);
        float fromPitch = active ? serverPitch : (mc.player != null ? mc.player.getPitch() : 0f);
        trajectory.start(fromYaw, fromPitch, yaw, pitch, ticks);
        active = true;
    }

    /** Смена цели без пересоздания trajectory — плавно. */
    public static void retarget(float yaw, float pitch) {
        if (trajectory.isActive()) {
            trajectory.retarget(yaw, pitch);
        } else {
            startRotation(yaw, pitch, 12);
        }
    }

    /** Прогресс trajectory (0-1). */
    public static float progress() {
        return trajectory.progress();
    }

    /** Pre-aim: начать поворот заранее. */
    public static void preAim(float yaw, float pitch, int totalTicks, int preTicks) {
        startRotation(yaw, pitch, totalTicks);
        preAimTicks = preTicks;
    }

    public static boolean isPreAiming() { return preAimTicks > 0; }

    public static void release() {
        active = false;
        trajectory.stop();
        preAimTicks = 0;
    }

    public static void clear() {
        active = false;
        trajectory.stop();
        preAimTicks = 0;
    }

    public static boolean isActive() { return active; }
    public static boolean isTrajectoryActive() { return trajectory.isActive(); }

    public static float getYaw() {
        if (active) return serverYaw;
        return mc.player != null ? mc.player.getYaw() : 0f;
    }

    public static float getPitch() {
        if (active) return serverPitch;
        return mc.player != null ? mc.player.getPitch() : 0f;
    }

    public static long lastUpdateMs() { return lastUpdateMs; }

    /** GCD-chain — держит rotation кратным GCD. */
    public static float[] gcdChain(float yaw, float pitch) {
        float gcd = gcd();
        float lastYaw = active ? serverYaw : 0f;
        float lastPitch = active ? serverPitch : 0f;

        float dYaw = wrap(yaw - lastYaw);
        float dPitch = pitch - lastPitch;

        float snappedDy = Math.round(dYaw / gcd) * gcd;
        float snappedDp = Math.round(dPitch / gcd) * gcd;

        return new float[]{ lastYaw + snappedDy, lastPitch + snappedDp };
    }

    public static void sendNow() { /* no-op */ }

    public static void setAndSend(float yaw, float pitch, int holdTicks) {
        set(yaw, pitch);
    }

    public static float gcd() { return GcdManager.getGcd(); }

    public static float snap(float value, float gcd) {
        return Math.round(value / gcd) * gcd;
    }

    public static float[] applyGcd(float yaw, float pitch) {
        return new float[]{ snap(yaw, gcd()), snap(pitch, gcd()) };
    }

    public static void markSent(float yaw, float pitch) {
        serverYaw = yaw;
        serverPitch = pitch;
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}