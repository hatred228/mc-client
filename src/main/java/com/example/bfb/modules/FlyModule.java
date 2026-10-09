package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
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
        // Motion — только через пакеты, не трогаем velocity клиента
        if (player.networkHandler == null) return;
        double[] wish = CombatUtil.wish(player);
        double y = 0;
        if (client.options.jumpKey.isPressed()) y += speed.get() * 0.2;
        if (client.options.sneakKey.isPressed()) y -= speed.get() * 0.2;
        double nx = player.getX() + wish[0] * speed.get() * 0.15;
        double nz = player.getZ() + wish[1] * speed.get() * 0.15;
        double ny = player.getY() + y;
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(
                nx, ny, nz, false, player.horizontalCollision));
        player.setPosition(nx, ny, nz);
        player.fallDistance = 0;
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