package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.GcdManager;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

public class FlyModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Motion", "Motion", "Creative"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.2, 0.2, 5.0, 0.1));

    public FlyModule() {
        super("Fly", "Flight: Motion or Creative", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;
        if (mode.is("Creative")) {
            var abilities = player.getAbilities();
            abilities.allowFlying = true;
            abilities.flying = true;
            abilities.setFlySpeed(speed.getFloat() * 0.05f);
            player.fallDistance = 0;
            return;
        }
        // Motion — через GCD-compliant пакеты для Intave compatibility
        if (player.networkHandler == null) return;

        double[] wish = CombatUtil.wish(player);
        double y = 0;
        if (client.options.jumpKey.isPressed()) y += speed.get() * 0.2;
        if (client.options.sneakKey.isPressed()) y -= speed.get() * 0.2;

        double nx = player.getX() + wish[0] * speed.get() * 0.15;
        double nz = player.getZ() + wish[1] * speed.get() * 0.15;
        double ny = player.getY() + y;

        // Применяем GCD к позиции для Intave compatibility
        // Используем GcdManager для синхронизированного снапа
        float[] gcdSnap = GcdManager.applyGcd(
            (float) player.getYaw(),
            (float) player.getPitch()
        );

        // Отправляем пакет с delta-позицией (как при движении)
        double dx = nx - player.getX();
        double dz = nz - player.getZ();
        double dy = ny - player.getY();

        // GCD для позиции - сервер ожидает определённые интервалы
        float gcd = GcdManager.getGcd();
        double snappedDx = Math.round(dx / gcd) * gcd;
        double snappedDy = Math.round(dy / gcd) * gcd;
        double snappedDz = Math.round(dz / gcd) * gcd;

        double finalX = player.getX() + snappedDx;
        double finalY = player.getY() + snappedDy;
        double finalZ = player.getZ() + snappedDz;

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                finalX, finalY, finalZ, false, player.horizontalCollision));

        player.setPosition(finalX, finalY, finalZ);
        player.fallDistance = 0;

        // Синхронизируем ротацию через RotationManager
        if (RotationManager.isActive()) {
            float[] fixed = GcdManager.applyGcd(
                RotationManager.getYaw(),
                RotationManager.getPitch()
            );
            RotationManager.markSent(fixed[0], fixed[1]);
        }
    }

    @Override
    public void onDisable() {
        ClientPlayerEntity player = BfbMod.getClient().player;
        if (player == null) return;
        var abilities = player.getAbilities();
        if (!abilities.creativeMode) {
            abilities.flying = false;
            abilities.allowFlying = false;
        }
        abilities.setFlySpeed(0.05f);
    }
}
