package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Единственный источник правды для server-side rotation.
 * Камера (player.getYaw/getPitch) не трогается — silent rotation.
 *
 * Все модули пишут сюда. ClientPlayNetworkHandlerMixin подменяет
 * yaw/pitch в исходящих PlayerMoveC2SPacket по этим значениям.
 */
public final class RotationManager {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static float serverYaw;
    private static float serverPitch;
    private static float lastSentYaw;
    private static float lastSentPitch;
    private static boolean active;
    private static int holdTicks;
    private static ClientPlayerEntity lastPlayer;

    private RotationManager() {}

    public static void tick(ClientPlayerEntity player) {
        if (player == null) {
            if (lastPlayer != null) {
                clear();
                lastPlayer = null;
            }
            return;
        }
        if (lastPlayer != player) {
            init(player);
            lastPlayer = player;
        }
        if (holdTicks > 0) {
            holdTicks--;
            if (holdTicks == 0) active = false;
        }
    }

    public static void init(ClientPlayerEntity player) {
        serverYaw = player.getYaw();
        serverPitch = player.getPitch();
        lastSentYaw = serverYaw;
        lastSentPitch = serverPitch;
        active = false;
        holdTicks = 0;
    }

    public static void set(float yaw, float pitch) {
        serverYaw = yaw;
        serverPitch = MathHelper.clamp(pitch, -90f, 90f);
        active = true;
        holdTicks = 0;
    }

    public static void set(float yaw, float pitch, int holdForTicks) {
        set(yaw, pitch);
        holdTicks = holdForTicks;
    }

    /** Отпустить через 2 тика — без snap-back на камеру. */
    public static void release() {
        if (active) holdTicks = 2;
    }

    public static void clear() {
        active = false;
        holdTicks = 0;
    }

    public static boolean isActive() { return active; }

    public static float getYaw() {
        if (active) return serverYaw;
        return mc.player != null ? mc.player.getYaw() : 0f;
    }

    public static float getPitch() {
        if (active) return serverPitch;
        return mc.player != null ? mc.player.getPitch() : 0f;
    }

    public static float getLastSentYaw()   { return lastSentYaw; }
    public static float getLastSentPitch() { return lastSentPitch; }

    /**
     * Точный vanilla GCD 1.21: f = sens*0.6+0.2; gcd = f^3 * 1.2
     * БЕЗ jitter — сервер ждёт идеально кратное.
     */
    public static float gcd() {
        double sens = mc.options.getMouseSensitivity().getValue();
        float f = (float) (sens * 0.6 + 0.2);
        return Math.max(1.0e-5f, f * f * f * 1.2f);
    }

    public static float snap(float value, float gcd) {
        return Math.round(value / gcd) * gcd;
    }

    /**
     * Квантуем ДЕЛЬТУ от последнего отправленного — так сервер видит
     * консистентный поток, а не плавающий абсолют.
     */
    public static float[] applyGcd(float yaw, float pitch) {
        float g = gcd();
        float dYaw = MathHelper.wrapDegrees(yaw - lastSentYaw);
        float dPitch = pitch - lastSentPitch;
        float fYaw = lastSentYaw + Math.round(dYaw / g) * g;
        float fPitch = lastSentPitch + Math.round(dPitch / g) * g;
        fPitch = MathHelper.clamp(fPitch, -90f, 90f);
        return new float[]{ fYaw, fPitch };
    }

    public static void markSent(float yaw, float pitch) {
        lastSentYaw = yaw;
        lastSentPitch = pitch;
    }
}