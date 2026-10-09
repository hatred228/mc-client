package com.example.bfb.mixin;

import com.example.bfb.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Silent rotation на исходящих PlayerMoveC2SPacket.
 *
 * Целевой класс — ClientCommonNetworkHandler (родитель ClientPlayNetworkHandler),
 * потому что sendPacket объявлен там, а не в ClientPlayNetworkHandler.
 */
@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {

    @ModifyVariable(
        method = "sendPacket",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Packet<?> bfb$silentRotation(Packet<?> packet) {
        if (!RotationManager.isActive()) return packet;
        if (!(packet instanceof PlayerMoveC2SPacket move)) return packet;

        float[] fixed = RotationManager.applyGcd(
                RotationManager.getYaw(),
                RotationManager.getPitch()
        );

        float dYaw = Math.abs(MathHelper.wrapDegrees(
                fixed[0] - RotationManager.getLastSentYaw()));
        float dPitch = Math.abs(fixed[1] - RotationManager.getLastSentPitch());
        if (dYaw < 1.0e-4f && dPitch < 1.0e-4f) {
            return packet;
        }

        RotationManager.markSent(fixed[0], fixed[1]);

        if (move instanceof PlayerMoveC2SPacket.LookAndOnGround look) {
            return new PlayerMoveC2SPacket.LookAndOnGround(
                    fixed[0], fixed[1], look.isOnGround(), look.horizontalCollision());
        }
        if (move instanceof PlayerMoveC2SPacket.Full full) {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return packet;
            return new PlayerMoveC2SPacket.Full(
                    player.getX(), player.getY(), player.getZ(),
                    fixed[0], fixed[1],
                    full.isOnGround(), full.horizontalCollision());
        }
        // PositionAndOnGround rotation не несёт — оставляем как есть
        return packet;
    }
}