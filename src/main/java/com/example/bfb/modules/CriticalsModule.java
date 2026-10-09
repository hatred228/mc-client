package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

/**
 * Criticals — БЕЗОПАСНЫЙ режим.
 * Отправляет ТОЛЬКО ОДИН position-пакет (Vulcan не кикает).
 * Не делает ground spoof, если игрок уже в воздухе.
 * НЕ ИСПОЛЬЗОВАТЬ на серверах с Matrix + Vulcan (RaidMine) — риск setback.
 */
public class CriticalsModule extends CheatModule {
    public CriticalsModule() {
        super("Criticals", "Fall criticals without breaking jump", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    public void prep(ClientPlayerEntity player) {
        if (player.networkHandler == null) return;
        if (player.input.playerInput.jump()) return;
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;
        if (player.getVehicle() != null || player.getAbilities().flying) return;

        // Если игрок УЖЕ в воздухе — не делаем ground spoof, это палево
        if (!player.isOnGround()) return;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        boolean collision = player.horizontalCollision;

        // Отправляем ТОЛЬКО ОДИН пакет — минимальный прыжок для крита
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y + 0.0625, z, false, collision));
    }
}