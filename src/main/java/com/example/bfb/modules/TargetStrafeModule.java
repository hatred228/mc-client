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
import net.minecraft.client.network.ClientPlayerEntity;
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

public class TargetStrafeModule extends CheatModule {
    public final ModeSetting orbitStyle = add(new ModeSetting("Orbit", "Circle",
            "Circle", "Ellipse", "Figure-8", "Square", "Triangle"));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));
    public final NumberSetting radius = add(new NumberSetting("Radius X", 2.2, 1.0, 4.5, 0.1));
    public final NumberSetting radiusY = add(new NumberSetting("Radius Y", 2.2, 1.0, 4.5, 0.1));
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.18, 0.08, 0.32, 0.02));
    public final NumberSetting radialGain = add(new NumberSetting("Radial gain", 0.08, 0.02, 0.25, 0.01));
    public final BooleanSetting withKillAura = add(new BooleanSetting("With KillAura", true));
    public final BooleanSetting withAimAssist = add(new BooleanSetting("With AimAssist", true));
    public final BooleanSetting withTriggerBot = add(new BooleanSetting("With TriggerBot", true));
    public final BooleanSetting withCrosshair = add(new BooleanSetting("With Crosshair", false));
    public final BooleanSetting switchOnHit = add(new BooleanSetting("Switch on hit", true));
    public final BooleanSetting visualOrbit = add(new BooleanSetting("Visual orbit", true));
    public final BooleanSetting visualDot = add(new BooleanSetting("Current pos dot", true));

    public static volatile int strafeDir = 0;
    public static volatile PlayerInput overrideInput = null;

    private final Random RNG = new Random();
    private int direction = 1;
    private int switchCooldown;
    private int hitCooldown;
    private float time = 0f;
    private float speedMultiplier = 1f;
    private float orbitAngle = 0f;
    private Entity renderTarget;

    public TargetStrafeModule() {
        super("TargetStrafe", "Orbits target with multiple styles", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        direction = RNG.nextBoolean() ? 1 : -1;
        switchCooldown = 0;
        hitCooldown = 0;
        time = 0f;
        orbitAngle = 0f;
        speedMultiplier = 1f;
    }

    @Override
    public void onDisable() {
        direction = 1;
        switchCooldown = 0;
        hitCooldown = 0;
        speedMultiplier = 1f;
        strafeDir = 0;
        renderTarget = null;
        overrideInput = null;
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
            return;
        }

        if (switchCooldown == 0 && RNG.nextInt(140) == 0) {
            direction = -direction;
            switchCooldown = 100 + RNG.nextInt(80);
        }

        if (switchOnHit.get() && hitCooldown == 0 && player.hurtTime > 0) {
            direction = -direction;
            hitCooldown = 14;
        }

        time += 0.05f;
        float targetMult = 0.9f + 0.2f * (float) Math.sin(time * 0.7f) + (RNG.nextFloat() - 0.5f) * 0.05f;
        speedMultiplier += (targetMult - speedMultiplier) * 0.03f;
        speedMultiplier = MathHelper.clamp(speedMultiplier, 0.8f, 1.2f);

        double rMax = Math.max(1.0, Math.max(radius.getFloat(), radiusY.getFloat()));
        double maxLinear = 0.26 * (speed.getFloat() / 0.18f);
        double angularSpeed = maxLinear / rMax;
        orbitAngle += angularSpeed * direction * 0.5;
        orbitAngle %= (float) (Math.PI * 2);

        double rx = radius.getFloat();
        double ry = radiusY.getFloat();
        double orbitX = 0, orbitZ = 0;
        switch (orbitStyle.get()) {
            case "Ellipse" -> { orbitX = Math.cos(orbitAngle) * rx; orbitZ = Math.sin(orbitAngle) * ry; }
            case "Figure-8" -> { orbitX = Math.sin(orbitAngle) * rx; orbitZ = Math.sin(orbitAngle * 2) * ry * 0.5; }
            case "Square" -> {
                double t = (orbitAngle / (Math.PI * 2)) * 4;
                int side = (int) t;
                double p = t - side;
                switch (side) {
                    case 0 -> { orbitX = rx * (0.5 - p); orbitZ = ry * 0.5; }
                    case 1 -> { orbitX = -rx * 0.5; orbitZ = ry * (0.5 - p); }
                    case 2 -> { orbitX = rx * (p - 0.5); orbitZ = -ry * 0.5; }
                    default -> { orbitX = rx * 0.5; orbitZ = ry * (p - 0.5); }
                }
                orbitX *= 2; orbitZ *= 2;
            }
            case "Triangle" -> {
                double t = (orbitAngle / (Math.PI * 2)) * 3;
                int side = (int) t;
                double p = t - side;
                double angle1 = side * (Math.PI * 2 / 3);
                double angle2 = ((side + 1) % 3) * (Math.PI * 2 / 3);
                double x1 = Math.cos(angle1) * rx, z1 = Math.sin(angle1) * ry;
                double x2 = Math.cos(angle2) * rx, z2 = Math.sin(angle2) * ry;
                orbitX = x1 + (x2 - x1) * p;
                orbitZ = z1 + (z2 - z1) * p;
            }
            default -> { orbitX = Math.cos(orbitAngle) * rx; orbitZ = Math.sin(orbitAngle) * ry; }
        }

        Vec3d targetPos = target.getEntityPos();
        Vec3d desired = new Vec3d(targetPos.x + orbitX, targetPos.y, targetPos.z + orbitZ);
        Vec3d toDesired = desired.subtract(player.getEntityPos());
        double dist = toDesired.length();
        if (dist < 0.01) {
            strafeDir = 0;
            overrideInput = null;
            return;
        }

        Vec3d dir = toDesired.multiply(1.0 / dist);

        // строим override input — движение только через WASD, без velocity nudge
        float yawRad = (float) Math.toRadians(
                RotationManager.isActive() ? RotationManager.getYaw() : player.getYaw()
        );
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double forwardDot = dir.x * (-sin) + dir.z * cos;
        double rightDot = dir.x * cos + dir.z * sin;

        double threshold = 0.25;
        boolean fwd = forwardDot > threshold;
        boolean back = forwardDot < -threshold;
        boolean left = rightDot < -threshold;
        boolean right = rightDot > threshold;

        if (!fwd && !back && !left && !right) {
            if (Math.abs(forwardDot) > Math.abs(rightDot)) {
                if (forwardDot > 0) fwd = true; else back = true;
            } else {
                if (rightDot > 0) right = true; else left = true;
            }
        }

        boolean sprint = mode.is("Rage") || direction == 1;

        overrideInput = new PlayerInput(
                fwd, back, left, right,
                false,
                player.input.playerInput.sneak(),
                sprint
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
            int seg = 72;
            for (int i = 0; i < seg; i++) {
                double t1 = i / (double) seg * Math.PI * 2;
                double t2 = (i + 1) / (double) seg * Math.PI * 2;
                Vec3d p1 = orbitPoint(targetPos, t1);
                Vec3d p2 = orbitPoint(targetPos, t2);
                float pulse = 0.5f + 0.5f * (float) Math.sin(time / 500.0 + i * 0.15);
                int alpha = (int) (60 + 100 * pulse);
                int c = HudStyle.lerpColor(0x9C5CFF, 0xFF5CA8, i / (float) seg);
                RenderUtil.bone(matrices, consumers, camera,
                        new Vec3d(p1.x, targetPos.y + 0.03, p1.z),
                        new Vec3d(p2.x, targetPos.y + 0.03, p2.z),
                        (alpha << 24) | c, 1.4f);
            }
        }

        if (visualDot.get()) {
            Vec3d cur = orbitPoint(targetPos, orbitAngle);
            double ringR = 0.15;
            int seg = 16;
            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg;
                double a2 = Math.PI * 2 * (i + 1) / seg;
                RenderUtil.bone(matrices, consumers, camera,
                        new Vec3d(cur.x + Math.cos(a1) * ringR, targetPos.y + 0.06, cur.z + Math.sin(a1) * ringR),
                        new Vec3d(cur.x + Math.cos(a2) * ringR, targetPos.y + 0.06, cur.z + Math.sin(a2) * ringR),
                        (255 << 24) | 0xFF5CA8, 2.5f);
            }
        }
    }

    private Vec3d orbitPoint(Vec3d targetPos, double angle) {
        double rx = radius.getFloat();
        double ry = radiusY.getFloat();
        double x, z;
        switch (orbitStyle.get()) {
            case "Ellipse" -> { x = Math.cos(angle) * rx; z = Math.sin(angle) * ry; }
            case "Figure-8" -> { x = Math.sin(angle) * rx; z = Math.sin(angle * 2) * ry * 0.5; }
            case "Square" -> {
                double t = (angle / (Math.PI * 2)) * 4;
                int side = (int) t; double p = t - side;
                switch (side) {
                    case 0 -> { x = rx * (0.5 - p); z = ry * 0.5; }
                    case 1 -> { x = -rx * 0.5; z = ry * (0.5 - p); }
                    case 2 -> { x = rx * (p - 0.5); z = -ry * 0.5; }
                    default -> { x = rx * 0.5; z = ry * (p - 0.5); }
                }
                x *= 2; z *= 2;
            }
            case "Triangle" -> {
                double t = (angle / (Math.PI * 2)) * 3;
                int side = (int) t; double p = t - side;
                double a1 = side * (Math.PI * 2 / 3);
                double a2 = ((side + 1) % 3) * (Math.PI * 2 / 3);
                double x1 = Math.cos(a1) * rx, z1 = Math.sin(a1) * ry;
                double x2 = Math.cos(a2) * rx, z2 = Math.sin(a2) * ry;
                x = x1 + (x2 - x1) * p; z = z1 + (z2 - z1) * p;
            }
            default -> { x = Math.cos(angle) * rx; z = Math.sin(angle) * ry; }
        }
        return new Vec3d(targetPos.x + x, targetPos.y, targetPos.z + z);
    }

    private Entity pickTarget(MinecraftClient client) {
        if (withKillAura.get()) {
            KillAuraModule aura = ModuleManager.get(KillAuraModule.class);
            if (aura != null && aura.isEnabled() && KillAuraModule.target != null && KillAuraModule.target.isAlive()) {
                return KillAuraModule.target;
            }
        }
        if (withAimAssist.get()) {
            AimAssistModule assist = ModuleManager.get(AimAssistModule.class);
            if (assist != null && assist.isEnabled() && AimAssistModule.target != null && AimAssistModule.target.isAlive()) {
                return AimAssistModule.target;
            }
        }
        if (withTriggerBot.get()) {
            TriggerBotModule tb = ModuleManager.get(TriggerBotModule.class);
            if (tb != null && tb.isEnabled() && client.crosshairTarget instanceof EntityHitResult ehr
                    && ehr.getEntity() instanceof LivingEntity le && le.isAlive()) {
                return le;
            }
        }
        if (withCrosshair.get() && client.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        return null;
    }
}