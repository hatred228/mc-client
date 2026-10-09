package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class FreecamModule extends CheatModule {
    public static FreecamModule instance;
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1));
    public final BooleanSetting showStand = add(new BooleanSetting("Show stand", false));

    private ArmorStandEntity cameraEntity;
    private Vec3d anchor;
    private float camYaw;
    private float camPitch;

    public FreecamModule() {
        super("Freecam", "Client-side camera (does not touch player)", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    @Override
    public void onEnable() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null) return;

        anchor = new Vec3d(player.getX(), player.getY(), player.getZ());
        camYaw = player.getYaw();
        camPitch = player.getPitch();

        if (cameraEntity == null || cameraEntity.isRemoved()) {
            cameraEntity = new ArmorStandEntity(client.world,
                    anchor.x, anchor.y + player.getStandingEyeHeight(), anchor.z);
            cameraEntity.setInvisible(!showStand.get());
            cameraEntity.setNoGravity(true);
            cameraEntity.noClip = true;
            cameraEntity.setId(-31415);
            client.world.addEntity(cameraEntity);
        }

        client.setCameraEntity(cameraEntity);
    }

    @Override
    public void onDisable() {
        var client = BfbMod.getClient();
        if (client.player != null) {
            client.setCameraEntity(client.player);
        }
        if (cameraEntity != null && !cameraEntity.isRemoved()) {
            cameraEntity.discard();
        }
        cameraEntity = null;
        anchor = null;
    }

    @Override
    public void onTick() {
        // nothing — движение камеры в onFrame из MouseMixin
    }

    /** Вызывается каждый кадр из Mouse.updateMouse. */
    public void onFrame(float frameSeconds) {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || cameraEntity == null || anchor == null) return;
        if (cameraEntity.isRemoved()) { onEnable(); return; }

        // camYaw/camPitch следуют за player (который vanilla двигает мышью)
        // мы НЕ восстанавливаем player.yaw — он должен двигаться как обычно
        camYaw = player.getYaw();
        camPitch = player.getPitch();

        cameraEntity.setYaw(camYaw);
        cameraEntity.lastYaw = camYaw;
        cameraEntity.setHeadYaw(camYaw);
        cameraEntity.lastHeadYaw = camYaw;
        cameraEntity.setPitch(camPitch);
        cameraEntity.lastPitch = camPitch;
        cameraEntity.setVelocity(0, 0, 0);
        cameraEntity.fallDistance = 0;

        // WASD движение камеры
        double forward = 0, strafe = 0;
        var opts = client.options;
        if (opts.forwardKey.isPressed()) forward += 1;
        if (opts.backKey.isPressed()) forward -= 1;
        if (opts.leftKey.isPressed()) strafe += 1;
        if (opts.rightKey.isPressed()) strafe -= 1;

        double len = Math.hypot(forward, strafe);
        if (len > 0) { forward /= len; strafe /= len; }

        double step = speed.get() * frameSeconds * 20;
        double yawRad = Math.toRadians(camYaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double dx = (forward * -sin + strafe * cos) * step;
        double dz = (forward * cos + strafe * sin) * step;
        double dy = 0;
        if (opts.jumpKey.isPressed()) dy += step;
        if (opts.sneakKey.isPressed()) dy -= step;

        Vec3d pos = cameraEntity.getEntityPos();
        double newY = pos.y + dy;
        if (client.world != null) {
            int bottom = client.world.getBottomY();
            int top = client.world.getTopYInclusive();
            if (newY < bottom + 2) newY = bottom + 2;
            if (newY > top - 2) newY = top - 2;
        }

        Vec3d newPos = new Vec3d(pos.x + dx, newY, pos.z + dz);
        cameraEntity.setPosition(newPos);
        cameraEntity.lastX = newPos.x;
        cameraEntity.lastY = newPos.y;
        cameraEntity.lastZ = newPos.z;
        cameraEntity.setBoundingBox(cameraEntity.getBoundingBox().offset(newPos.subtract(pos)));

        if (client.getCameraEntity() != cameraEntity) {
            client.setCameraEntity(cameraEntity);
        }
    }
}