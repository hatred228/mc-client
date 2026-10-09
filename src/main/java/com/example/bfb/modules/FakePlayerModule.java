package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class FakePlayerModule extends CheatModule {
    private OtherClientPlayerEntity fake;
    private Vec3d spot;
    private float yaw;

    public FakePlayerModule() {
        super("FakePlayer", "Client-side clone for testing combat", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        spawn();
    }

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;
        if (fake == null || fake.isRemoved() || fake.getEntityWorld() != client.world) {
            spawn();
            return;
        }
        fake.noClip = true;
        fake.setVelocity(Vec3d.ZERO);
        fake.setOnGround(true);
        fake.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw, 0f);
        fake.setHeadYaw(yaw);
        fake.setBodyYaw(yaw);
        if (fake.getHealth() < 8f) fake.setHealth(20f);
    }

    private void spawn() {
        remove();
        var client = BfbMod.getClient();
        var player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;
        yaw = player.getYaw();
        double rad = Math.toRadians(yaw);
        spot = new Vec3d(player.getX() - Math.sin(rad) * 3.0, player.getY(), player.getZ() + Math.cos(rad) * 3.0);
        GameProfile real = player.getGameProfile();
        GameProfile profile = new GameProfile(UUID.randomUUID(), real.name(), real.properties());
        fake = new OtherClientPlayerEntity(world, profile);
        fake.setId(-20000 - (int) (System.nanoTime() & 4095));
        fake.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw + 180f, 0f);
        fake.setHeadYaw(yaw + 180f);
        fake.setBodyYaw(yaw + 180f);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) fake.equipStack(slot, stack.copy());
        }
        fake.setHealth(20f);
        world.addEntity(fake);
    }

    private void remove() {
        var world = BfbMod.getClient().world;
        if (fake != null && world != null) world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        fake = null;
    }
}
