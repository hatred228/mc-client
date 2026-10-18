package com.example.bfb.mixin;

import com.example.bfb.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Подмена rotation в PlayerMoveC2SPacket.
 *
 * Ключевое:
 *   - применяет GCD-chain (rotation кратна GCD)
 *   - работает на земле и в воздухе
 *   - НЕ заменяет rotation больше чем на 30° за тик (SkyCave палит snap)
 */
@Mixin(ClientCommonNetworkHandler.class)
public class SilentRotationPacketMixin {

    private static final float MAX_ROTATION_DELTA = 30f;
    private static float lastPacketYaw = 0f;
    private static float lastPacketPitch = 0f;
    private static boolean hasLast = false;

    @ModifyVariable(
            method = "sendPacket",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Packet<?> bfb$swapRotation(Packet<?> packet) {
        if (!RotationManager.isActive()) return packet;
        if (!(packet instanceof PlayerMoveC2SPacket move)) return packet;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return packet;

        float yaw = RotationManager.getYaw();
        float pitch = RotationManager.getPitch();

        // === GCD-chain ===
        float[] chained = RotationManager.gcdChain(yaw, pitch);
        yaw = chained[0];
        pitch = chained[1];

        // === ограничение дельты между пакетами ===
        if (hasLast) {
            float dYaw = wrap(yaw - lastPacketYaw);
            float dPitch = pitch - lastPacketPitch;

            if (Math.abs(dYaw) > MAX_ROTATION_DELTA) {
                dYaw = Math.signum(dYaw) * MAX_ROTATION_DELTA;
            }
            if (Math.abs(dPitch) > MAX_ROTATION_DELTA) {
                dPitch = Math.signum(dPitch) * MAX_ROTATION_DELTA;
            }
            yaw = lastPacketYaw + dYaw;
            pitch = lastPacketPitch + dPitch;
        }
        lastPacketYaw = yaw;
        lastPacketPitch = pitch;
        hasLast = true;

        RotationManager.markSent(yaw, pitch);

        if (move instanceof PlayerMoveC2SPacket.Full full) {
            return new PlayerMoveC2SPacket.Full(
                    full.getX(0), full.getY(0), full.getZ(0),
                    yaw, pitch,
                    full.isOnGround(), full.horizontalCollision());
        }

        if (move instanceof PlayerMoveC2SPacket.LookAndOnGround look) {
            return new PlayerMoveC2SPacket.LookAndOnGround(
                    yaw, pitch,
                    look.isOnGround(), look.horizontalCollision());
        }

        return packet;
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}