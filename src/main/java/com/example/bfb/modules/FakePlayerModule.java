package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class FakePlayerModule extends CheatModule {

    public final BooleanSetting autoRespawn = add(new BooleanSetting("Auto respawn", true));
    public final BooleanSetting copyArmor = add(new BooleanSetting("Copy armor", true));
    public final NumberSetting fakeHp = add(new NumberSetting("Fake HP", 20, 1, 40, 1));
    public final NumberSetting distance = add(new NumberSetting("Spawn distance", 3.0, 1.5, 8.0, 0.5));
    public final BooleanSetting patrol = add(new BooleanSetting("Patrol", false));
    public final NumberSetting patrolRange = add(new NumberSetting("Patrol range", 2.0, 0.5, 6.0, 0.5));
    public final NumberSetting patrolSpeed = add(new NumberSetting("Patrol speed", 0.08, 0.02, 0.3, 0.02));

    private OtherClientPlayerEntity fake;
    private Vec3d spot;
    private float yaw;
    private Vec3d patrolOrigin;
    private int patrolDir = 1;
    private int patrolTick;

    public FakePlayerModule() {
        super("FakePlayer", "Client-side clone for testing", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() { spawn(); }

    @Override
    public void onDisable() { remove(); }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;

        if (fake == null || fake.isRemoved() || fake.getEntityWorld() != client.world) {
            spawn();
            return;
        }

        float targetHp = fakeHp.getFloat();
        if (autoRespawn.get()) {
            if (fake.getHealth() <= 0.5f || fake.isRemoved() || !fake.isAlive()) {
                respawn();
                return;
            }
            if (fake.getHealth() < targetHp) fake.setHealth(targetHp);
        }

        if (copyArmor.get()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                var stack = client.player.getEquippedStack(slot);
                if (!stack.isEmpty()) fake.equipStack(slot, stack.copy());
            }
        }

        if (patrol.get()) {
            patrolTick++;
            double range = patrolRange.get();
            double speed = patrolSpeed.get();
            patrolOrigin = patrolOrigin == null ? spot : patrolOrigin;

            double offset = Math.sin(patrolTick * speed) * range;
            Vec3d newPos = patrolOrigin.add(
                    Math.cos(Math.toRadians(yaw)) * offset,
                    0,
                    Math.sin(Math.toRadians(yaw)) * offset);

            fake.refreshPositionAndAngles(newPos.x, newPos.y, newPos.z, yaw, 0f);
            fake.setHeadYaw(yaw);
            fake.setBodyYaw(yaw);
        } else {
            fake.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw, 0f);
            fake.setHeadYaw(yaw);
            fake.setBodyYaw(yaw);
        }

        fake.setVelocity(Vec3d.ZERO);
        fake.noClip = true;
        fake.setOnGround(true);
        fake.setHealth(targetHp);
    }

    private void respawn() {
        remove();
        spawn();
    }

    private void spawn() {
        var client = BfbMod.getClient();
        PlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;

        yaw = player.getYaw();
        double rad = Math.toRadians(yaw);
        double dist = distance.get();
        spot = new Vec3d(
                player.getX() - Math.sin(rad) * dist,
                player.getY(),
                player.getZ() + Math.cos(rad) * dist);
        patrolOrigin = spot;
        patrolTick = 0;

        GameProfile real = player.getGameProfile();
        GameProfile profile = new GameProfile(UUID.randomUUID(), real.name(), real.properties());
        fake = new OtherClientPlayerEntity(world, profile);
        fake.setId(-20000 - (int) (System.nanoTime() & 4095));
        fake.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw + 180f, 0f);
        fake.setHeadYaw(yaw + 180f);
        fake.setBodyYaw(yaw + 180f);

        if (copyArmor.get()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                var stack = player.getEquippedStack(slot);
                if (!stack.isEmpty()) fake.equipStack(slot, stack.copy());
            }
        }

        fake.setHealth(fakeHp.getFloat());
        world.addEntity(fake);
    }

    private void remove() {
        var world = BfbMod.getClient().world;
        if (fake != null && world != null) {
            world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        }
        fake = null;
    }

    public OtherClientPlayerEntity fake() { return fake; }
}