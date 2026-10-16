package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudStyle;
import com.example.bfb.ModuleManager;
import com.example.bfb.RenderUtil;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * TargetStrafe — обход цели с ВИЗУАЛЬНЫМ вращением камеры.
 *
 * Отличия от прошлой версии:
 *   1. Камера РЕАЛЬНО крутится к цели (player.setYaw/setPitch) — если
 *      античит требует silent, это можно выключить через autoAim.
 *   2. Движение считается относительно ЦЕЛИ, не относительно камеры —
 *      поэтому не дёргается когда камера поворачивается.
 *   3. Camera lock режим — камера всегда смотрит в центр цели, WASD
 *      двигает относительно camera yaw.
 */
public class TargetStrafeModule extends CheatModule {
    public final ModeSetting orbitStyle = add(new ModeSetting("Orbit", "Circle",
            "Circle", "Ellipse", "Figure-8", "Square", "Triangle"));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Smooth"));
    public final NumberSetting radius = add(new NumberSetting("Radius", 2.0, 1.0, 4.0, 0.1));
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.25, 0.15, 0.5, 0.01));

    // === AIM ===
    public final BooleanSetting cameraLock = add(new BooleanSetting("Camera lock", true));
    public final BooleanSetting silentAim = add(new BooleanSetting("Silent aim", false));
    public final NumberSetting aimSpeed = add(new NumberSetting("Aim speed", 0.6, 0.2, 1.0, 0.05));

    // === TARGET ===
    public final BooleanSetting withKillAura = add(new BooleanSetting("With KillAura", true));
    public final BooleanSetting visualOrbit = add(new BooleanSetting("Visual orbit", true));
    public final BooleanSetting visualDot = add(new BooleanSetting("Current pos dot", true));
    public final BooleanSetting switchOnDamage = add(new BooleanSetting("Switch on hit", true));

    public static volatile int strafeDir = 0;
    public static volatile PlayerInput overrideInput = null;

    private final Random RNG = new Random();
    private int direction = 1;
    private int switchCooldown;
    private int hitCooldown;
    private float time = 0f;
    private float orbitAngle = 0f;
    private float visualYaw = 0f;
    private float visualPitch = 0f;
    private Entity renderTarget;

    public TargetStrafeModule() {
        super("TargetStrafe", "Orbits target with camera rotation", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        direction = RNG.nextBoolean() ? 1 : -1;
        switchCooldown = 0;
        hitCooldown = 0;
        time = 0f;
        orbitAngle = 0f;
        visualYaw = Float.NaN;
        visualPitch = Float.NaN;
    }

    @Override
    public void onDisable() {
        direction = 1;
        switchCooldown = 0;
        hitCooldown = 0;
        strafeDir = 0;
        renderTarget = null;
        overrideInput = null;
        RotationManager.release();
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null || client.currentScreen != null) {
            strafeDir = 0;
            renderTarget = null;
            overrideInput = null;
            return;
        }

        if (switchCooldown > 0) switchCooldown--;
        if (hitCooldown > 0) hitCooldown--;

        Entity target = pickTarget(client);
        renderTarget = target;
        if (target == null || !target.isAlive()) {
            strafeDir = 0;
            overrideInput = null;
            RotationManager.release();
            return;
        }

        // === AIM — куда смотреть ===
        Vec3d aimPoint = target.getBoundingBox().getCenter();
        float[] ideal = calculateRotations(player.getEyePos(), aimPoint);

        if (Float.isNaN(visualYaw)) {
            visualYaw = player.getYaw();
            visualPitch = player.getPitch();
        }

        // Плавное приближение камеры
        float k = aimSpeed.getFloat();
        float dy = wrap(ideal[0] - visualYaw);
        float dp = ideal[1] - visualPitch;
        visualYaw += dy * k;
        visualPitch += dp * k;
        visualPitch = MathHelper.clamp(visualPitch, -90f, 90f);

        if (silentAim.get()) {
            // Silent — сервер видит rotation, камера свободна
            RotationManager.setAndSend(visualYaw, visualPitch, 4);
        } else if (cameraLock.get()) {
            // Camera lock — камера реально крутится к цели
            player.setYaw(visualYaw);
            player.setPitch(visualPitch);
        }

        // === Плавное переключение направления ===
        if (switchCooldown == 0 && RNG.nextInt(100) == 0) {
            direction = -direction;
            switchCooldown = 80 + RNG.nextInt(60);
        }

        if (switchOnDamage.get() && hitCooldown == 0 && player.hurtTime > 0) {
            direction = -direction;
            hitCooldown = 10;
        }

        // === ORBIT — позиция на орбите ===
        time += 0.03f;
        float targetMult = 0.9f + 0.2f * (float) Math.sin(time * 0.5f);
        double maxLinear = 0.18 * targetMult;
        double radiusVal = radius.getFloat();
        double angularSpeed = maxLinear / radiusVal;
        orbitAngle += angularSpeed * direction * 0.5f;
        orbitAngle %= (float) (Math.PI * 2);

        Vec3d targetPos = target.getEntityPos();
        double orbitX, orbitZ;
        switch (orbitStyle.get()) {
            case "Ellipse" -> { orbitX = Math.cos(orbitAngle) * radiusVal; orbitZ = Math.sin(orbitAngle) * radiusVal; }
            case "Figure-8" -> { orbitX = Math.sin(orbitAngle) * radiusVal; orbitZ = Math.sin(orbitAngle * 2) * radiusVal * 0.5; }
            case "Square" -> {
                double t = (orbitAngle / (Math.PI * 2)) * 4;
                int side = (int) t;
                double p = t - side;
                switch (side) {
                    case 0 -> { orbitX = radiusVal * (0.5 - p); orbitZ = radiusVal * 0.5; }
                    case 1 -> { orbitX = -radiusVal * 0.5; orbitZ = radiusVal * (0.5 - p); }
                    case 2 -> { orbitX = radiusVal * (p - 0.5); orbitZ = -radiusVal * 0.5; }
                    default -> { orbitX = radiusVal * 0.5; orbitZ = radiusVal * (p - 0.5); }
                }
            }
            case "Triangle" -> {
                double t = (orbitAngle / (Math.PI * 2)) * 3;
                int side = (int) t;
                double p = t - side;
                double angle1 = side * (Math.PI * 2 / 3);
                double angle2 = ((side + 1) % 3) * (Math.PI * 2 / 3);
                double x1 = Math.cos(angle1) * radiusVal, z1 = Math.sin(angle1) * radiusVal;
                double x2 = Math.cos(angle2) * radiusVal, z2 = Math.sin(angle2) * radiusVal;
                orbitX = x1 + (x2 - x1) * p;
                orbitZ = z1 + (z2 - z1) * p;
            }
            default -> { orbitX = Math.cos(orbitAngle) * radiusVal; orbitZ = Math.sin(orbitAngle) * radiusVal; }
        }

        Vec3d desired = new Vec3d(targetPos.x + orbitX, targetPos.y + 0.2, targetPos.z + orbitZ);
        Vec3d toDesired = desired.subtract(player.getEntityPos());
        double dist = toDesired.length();
        if (dist < 0.01) {
            strafeDir = 0;
            overrideInput = null;
            return;
        }

        Vec3d dir = toDesired.multiply(1.0 / dist);

        // ВАЖНО: движение считаем относительно КАМЕРЫ игрока (visualYaw),
        // не относительно цели. Иначе получаются рывки.
        float yawRad = (float) Math.toRadians(visualYaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        // forward = -sin * x + cos * z,  right = cos * x + sin * z
        double forwardDot = dir.x * (-sin) + dir.z * cos;
        double rightDot = dir.x * cos + dir.z * sin;

        // Строим WASD input
        double threshold = 0.25;
        boolean fwd = false;
        boolean back = false;
        boolean left = false;
        boolean right = false;

        // Всегда жмём forward если цель впереди
        if (forwardDot > threshold) fwd = true;
        else if (forwardDot < -threshold) back = true;

        if (rightDot > threshold) right = true;
        else if (rightDot < -threshold) left = true;

        // Если ничего не выбрано — двигаемся вперёд (fallback)
        if (!fwd && !back && !left && !right) {
            if (Math.abs(forwardDot) > Math.abs(rightDot)) {
                if (forwardDot > 0) fwd = true; else back = true;
            } else {
                if (rightDot > 0) right = true; else left = true;
            }
        }

        overrideInput = new PlayerInput(
                fwd, back, left, right,
                false,
                player.input.playerInput.sneak(),
                false
        );

        strafeDir = direction;
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;
        if (renderTarget == null || !renderTarget.isAlive()) return;

        Vec3d targetPos = renderTarget.getEntityPos();
        long time = System.currentTimeMillis();

        if (visualOrbit.get()) {
            int seg = 36;
            for (int i = 0; i < seg; i++) {
                double t1 = i / (double) seg * Math.PI * 2;
                double t2 = (i + 1) / (double) seg * Math.PI * 2;
                Vec3d p1 = orbitPoint(targetPos, t1);
                Vec3d p2 = orbitPoint(targetPos, t2);
                float pulse = 0.5f + 0.5f * (float) Math.sin(time / 500.0 + i * 0.1);
                int alpha = (int) (40 + 80 * pulse);
                int c = HudStyle.lerpColor(0x9C5CFF, 0xFF5CA8, i / (float) seg);
                RenderUtil.bone(matrices, consumers, camera,
                        new Vec3d(p1.x, targetPos.y + 0.02, p1.z),
                        new Vec3d(p2.x, targetPos.y + 0.02, p2.z),
                        (alpha << 24) | c, 1.0f);
            }
        }

        if (visualDot.get()) {
            Vec3d cur = orbitPoint(targetPos, orbitAngle);
            double ringR = 0.1;
            int seg = 8;
            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg;
                double a2 = Math.PI * 2 * (i + 1) / seg;
                RenderUtil.bone(matrices, consumers, camera,
                        new Vec3d(cur.x + Math.cos(a1) * ringR, targetPos.y + 0.04, cur.z + Math.sin(a1) * ringR),
                        new Vec3d(cur.x + Math.cos(a2) * ringR, targetPos.y + 0.04, cur.z + Math.sin(a2) * ringR),
                        (255 << 24) | 0xFF5CA8, 1.5f);
            }
        }
    }

    private static float[] calculateRotations(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, flat)));
        return new float[]{yaw, pitch};
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private Vec3d orbitPoint(Vec3d targetPos, double angle) {
        double rx = radius.getFloat();
        double orbitX = 0, orbitZ = 0;
        switch (orbitStyle.get()) {
            case "Ellipse" -> { orbitX = Math.cos(angle) * rx; orbitZ = Math.sin(angle) * rx; }
            case "Figure-8" -> { orbitX = Math.sin(angle) * rx; orbitZ = Math.sin(angle * 2) * rx * 0.5; }
            case "Square" -> {
                double t = (angle / (Math.PI * 2)) * 4;
                int side = (int) t;
                double p = t - side;
                switch (side) {
                    case 0 -> { orbitX = rx * (0.5 - p); orbitZ = rx * 0.5; }
                    case 1 -> { orbitX = -rx * 0.5; orbitZ = rx * (0.5 - p); }
                    case 2 -> { orbitX = rx * (p - 0.5); orbitZ = -rx * 0.5; }
                    default -> { orbitX = rx * 0.5; orbitZ = rx * (p - 0.5); }
                }
            }
            case "Triangle" -> {
                double t = (angle / (Math.PI * 2)) * 3;
                int side = (int) t;
                double p = t - side;
                double angle1 = side * (Math.PI * 2 / 3);
                double angle2 = ((side + 1) % 3) * (Math.PI * 2 / 3);
                double x1 = Math.cos(angle1) * rx, z1 = Math.sin(angle1) * rx;
                double x2 = Math.cos(angle2) * rx, z2 = Math.sin(angle2) * rx;
                orbitX = x1 + (x2 - x1) * p;
                orbitZ = z1 + (z2 - z1) * p;
            }
            default -> { orbitX = Math.cos(angle) * rx; orbitZ = Math.sin(angle) * rx; }
        }
        return new Vec3d(targetPos.x + orbitX, targetPos.y, targetPos.z + orbitZ);
    }

    private Entity pickTarget(MinecraftClient client) {
        if (withKillAura.get()) {
            KillAuraModule aura = ModuleManager.get(KillAuraModule.class);
            if (aura != null && aura.isEnabled() && KillAuraModule.target != null && KillAuraModule.target.isAlive()) {
                return KillAuraModule.target;
            }
        }
        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        return null;
    }
}