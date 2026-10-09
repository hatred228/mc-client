package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.FriendManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AimAssistModule extends CheatModule {
    public static Entity target;

    // === targeting ===
    public final ModeSetting priority = add(new ModeSetting("Priority", "Angle",
            "Nearest", "Health", "Armor", "Angle", "Crosshair"));
    public final NumberSetting range = add(new NumberSetting("Range", 4.2, 2.5, 6.0, 0.1));
    public final NumberSetting fovThreshold = add(new NumberSetting("Assist FOV", 30.0, 10.0, 180.0, 5.0));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch delay", 15, 0, 40, 1));

    // === aim ===
    public final NumberSetting smoothSpeed = add(new NumberSetting("Aim Smoothness", 12.0, 1.0, 15.0, 0.5));
    public final NumberSetting noiseAmount = add(new NumberSetting("Micro tremor", 1.0, 0.0, 2.0, 0.1));
    public final NumberSetting reactionDelay = add(new NumberSetting("Reaction delay", 4, 0, 10, 1));
    public final NumberSetting maxAimSpeed = add(new NumberSetting("Max angular speed", 18, 10, 60, 1));
    public final NumberSetting missChance = add(new NumberSetting("Miss chance %", 10, 0, 30, 1));
    public final NumberSetting targetJitter = add(new NumberSetting("Target jitter", 0.10, 0.0, 0.3, 0.01));
    public final BooleanSetting aimAtCenter = add(new BooleanSetting("Aim at body", true));
    public final BooleanSetting leadPredict = add(new BooleanSetting("Lead prediction", true));
    public final NumberSetting leadFactor = add(new NumberSetting("Lead factor", 0.7, 0.0, 2.0, 0.1));
    public final BooleanSetting gcdFix = add(new BooleanSetting("GCD fix", true));

    // === attack ===
    public final BooleanSetting autoAttack = add(new BooleanSetting("Auto attack", false));
    public final NumberSetting attackAngle = add(new NumberSetting("Attack angle", 4.0, 1.0, 20.0, 0.5));
    public final NumberSetting cooldownReady = add(new NumberSetting("Cooldown", 0.95, 0.70, 1.00, 0.01));
    public final BooleanSetting rageMode = add(new BooleanSetting("Rage (snap)", false));

    // === multitarget ===
    public final BooleanSetting multiTarget = add(new BooleanSetting("Multi target", false));
    public final NumberSetting maxTargets = add(new NumberSetting("Max targets", 2, 1, 5, 1));

    // === filters ===
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    // === spring state ===
    private float currentYawVel = 0f;
    private float currentPitchVel = 0f;
    private float tremorYaw = 0f;
    private float tremorPitch = 0f;
    private float tremorTargetYaw = 0f;
    private float tremorTargetPitch = 0f;
    private int tremorRefresh = 0;
    private long lastAimFrameNanos = 0L;

    // === humanization ===
    private float overshootYaw = 0f;
    private float overshootPitch = 0f;
    private int overshootTicks = 0;
    private int microPauseTicks = 0;
    private int missTicks = 0;
    private Vec3d currentTargetPoint = null;

    private float aimTargetYaw = 0f;
    private float aimTargetPitch = 0f;
    private boolean hasAimTarget = false;

    private final Random RNG = new Random();
    private int attackCooldown;
    private int switchCooldown;
    private int reactionCooldown;
    private Entity lastTarget;

    public AimAssistModule() {
        super("AimAssist", "Smoothly aligns camera and attacks targets", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        target = null;
        attackCooldown = 0;
        switchCooldown = 0;
        reactionCooldown = 0;
        lastTarget = null;
        resetSpring();
        hasAimTarget = false;
        lastAimFrameNanos = 0L;
        currentTargetPoint = null;
    }

    private void resetSpring() {
        currentYawVel = 0;
        currentPitchVel = 0;
        tremorYaw = 0;
        tremorPitch = 0;
        overshootYaw = 0;
        overshootPitch = 0;
        overshootTicks = 0;
        microPauseTicks = 0;
        missTicks = 0;
    }

    // ==================== PUBLIC для MouseMixin ====================

    public boolean hasAimTargetPublic() {
        return hasAimTarget && !rageMode.get();
    }

    public float getAimTargetYaw() { return aimTargetYaw; }
    public float getAimTargetPitch() { return aimTargetPitch; }

    public void tickMouse(double frameSeconds) {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || !hasAimTarget || rageMode.get()) return;

        float dt = (float) frameSeconds;
        if (dt <= 0f) dt = 1f / 60f;
        if (dt > 0.1f) dt = 0.1f;

        lastAimFrameNanos = System.nanoTime();
        aimSmoothInternal(player, aimTargetYaw, aimTargetPitch, dt);
    }

    // ==================== TICK ====================

    @Override
    public void onTick() {
        if (attackCooldown > 0) attackCooldown--;
        if (switchCooldown > 0) switchCooldown--;
        if (reactionCooldown > 0) reactionCooldown--;

        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null || client.currentScreen != null) {
            target = null;
            hasAimTarget = false;
            currentTargetPoint = null;
            return;
        }

        double reach = range.get();
        double reachSq = reach * reach;

        List<Entity> candidates = new ArrayList<>();
        for (Entity e : com.example.bfb.EntityCache.living()) {
            if (!CombatUtil.accept(e, player, players.get(), mobs.get(), animals.get())) continue;
            if (FriendManager.isFriend(e)) continue;
            if (!invisible.get() && e.isInvisible() && !(e instanceof PlayerEntity)) continue;
            if (e.squaredDistanceTo(player) > reachSq) continue;
            if (!throughWalls.get() && !CombatUtil.canSee(player, e)) continue;

            float[] rot = calculateRotations(player.getEyePos(), getAimPoint(e));
            float yawDelta = Math.abs(MathHelper.wrapDegrees(rot[0] - player.getYaw()));
            float pitchDelta = Math.abs(rot[1] - player.getPitch());
            float totalAngle = (float) Math.hypot(yawDelta, pitchDelta);
            if (totalAngle > fovThreshold.getFloat()) continue;

            candidates.add(e);
        }

        if (candidates.isEmpty()) {
            target = null;
            lastTarget = null;
            hasAimTarget = false;
            currentTargetPoint = null;
            currentYawVel *= 0.5f;
            currentPitchVel *= 0.5f;
            return;
        }

        switch (priority.get()) {
            case "Health" -> candidates.sort(Comparator.comparingDouble(e ->
                    e instanceof LivingEntity le ? le.getHealth() : Float.MAX_VALUE));
            case "Armor" -> candidates.sort(Comparator.comparingDouble(e -> {
                if (!(e instanceof LivingEntity le)) return Float.MAX_VALUE;
                var attr = le.getAttributeInstance(EntityAttributes.ARMOR);
                return attr == null ? 0f : (float) attr.getValue();
            }));
            case "Angle" -> candidates.sort(Comparator.comparingDouble(e -> angleTo(player, e)));
            case "Crosshair" -> candidates.sort(Comparator.comparingDouble(e -> crosshairScore(player, e)));
            default -> candidates.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
        }

        Entity newTarget = candidates.get(0);
        if (newTarget != lastTarget) {
            if (switchCooldown == 0 && lastTarget != null) {
                switchCooldown = switchDelay.getInt();
                resetSpring();
                return;
            }
            lastTarget = newTarget;
            reactionCooldown = reactionDelay.getInt();
            currentTargetPoint = null;
            resetSpring();
        }
        target = newTarget;

        if (reactionCooldown > 0) {
            hasAimTarget = false;
            return;
        }

        if (missTicks > 0) {
            missTicks--;
            hasAimTarget = false;
            return;
        }
        if (target != null && player.distanceTo(target) > 3.5
                && RNG.nextFloat() * 100f < missChance.getFloat()) {
            missTicks = 2 + RNG.nextInt(4);
            hasAimTarget = false;
            return;
        }

        int max = multiTarget.get() ? Math.min(candidates.size(), maxTargets.getInt()) : 1;
        int hits = 0;

        for (Entity ent : candidates) {
            if (hits >= max) break;

            Vec3d aimPoint = getAimPoint(ent);

            if (leadPredict.get() && ent instanceof LivingEntity living) {
                double dist = player.distanceTo(ent);
                if (dist > 3.0) {
                    Vec3d vel = living.getVelocity();
                    double ticksAhead = (dist / 3.0) * leadFactor.get();
                    double dx = vel.x * ticksAhead;
                    double dy = vel.y * ticksAhead * 0.5;
                    double dz = vel.z * ticksAhead;
                    double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    double maxLead = 2.0;
                    if (len > maxLead) {
                        double scale = maxLead / len;
                        dx *= scale; dy *= scale; dz *= scale;
                    }
                    aimPoint = aimPoint.add(dx, dy, dz);
                }
            }

            if (currentTargetPoint == null || RNG.nextInt(4) == 0) {
                float jit = targetJitter.getFloat();
                double dist = player.distanceTo(ent);
                if (dist < 2.5) jit = 0f;
                else if (dist < 4.0) jit *= 0.4f;
                currentTargetPoint = aimPoint.add(
                        (RNG.nextGaussian()) * jit,
                        (RNG.nextGaussian()) * jit * 0.7,
                        (RNG.nextGaussian()) * jit
                );
            }

            float[] ideal = calculateRotations(player.getEyePos(), currentTargetPoint);

            if (rageMode.get()) {
                player.setYaw(ideal[0]);
                player.setPitch(MathHelper.clamp(ideal[1], -90f, 90f));
                resetSpring();
                hasAimTarget = false;
            } else {
                aimTargetYaw = ideal[0];
                aimTargetPitch = ideal[1];
                hasAimTarget = true;
            }

            if (autoAttack.get() && attackCooldown == 0) {
                float yawDelta = Math.abs(MathHelper.wrapDegrees(ideal[0] - player.getYaw()));
                float pitchDelta = Math.abs(ideal[1] - player.getPitch());
                float angleThresh = attackAngle.getFloat();

                boolean inAngle = yawDelta <= angleThresh && pitchDelta <= angleThresh;
                boolean inRange = player.distanceTo(ent) <= reach + 0.3f;
                boolean ready = player.getAttackCooldownProgress(0f) >= cooldownReady.getFloat();

                if (inAngle && inRange && ready) {
                    CombatUtil.attack(ent, false);
                    attackCooldown = 1 + RNG.nextInt(2);
                    hits++;
                }
            } else if (!autoAttack.get() && hits == 0) {
                hits++;
            }
        }
    }

    // ==================== RENDER fallback ====================

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;
        if (!hasAimTarget || rageMode.get()) return;
        if (client.currentScreen != null) return;

        long now = System.nanoTime();
        if (now - lastAimFrameNanos > 20_000_000L) {
            float dt = 1f / 60f;
            lastAimFrameNanos = now;
            aimSmoothInternal(player, aimTargetYaw, aimTargetPitch, dt);
        }
    }

    // ==================== SPRING ====================

    private void aimSmoothInternal(net.minecraft.client.network.ClientPlayerEntity player,
                                   float targetYaw, float targetPitch, float dt) {

        if (microPauseTicks > 0) {
            microPauseTicks--;
            return;
        }
        if (RNG.nextFloat() < 0.008f) {
            microPauseTicks = 1 + RNG.nextInt(3);
            return;
        }

        float yawError = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchError = targetPitch - player.getPitch();
        float distance = (float) Math.hypot(yawError, pitchError);

        if (distance < 0.6f) {
            currentYawVel *= 0.7f;
            currentPitchVel *= 0.7f;
            return;
        }

        // мягкий spring: smoothSpeed 1..15 → stiffness 200..60
        // max при smoothSpeed=1: ω = 14.1 рад/сек, 80% за 0.21 сек
        // min при smoothSpeed=15: ω = 7.7 рад/сек, 80% за 0.39 сек
        float baseSpeed = MathHelper.clamp(smoothSpeed.getFloat(), 1f, 15f);
        float stiffness = 200f - (baseSpeed - 1f) * ((200f - 60f) / 14f);
        float damping = 2f * (float) Math.sqrt(stiffness) * 1.0f;

        float yawAccel = yawError * stiffness - currentYawVel * damping;
        float pitchAccel = pitchError * stiffness - currentPitchVel * damping;

        currentYawVel += yawAccel * dt;
        currentPitchVel += pitchAccel * dt;

        // жёсткий клип скорости — медленнее чем в прошлой версии
        float maxYawVel = maxAimSpeed.getFloat() * 40f;
        float maxPitchVel = maxAimSpeed.getFloat() * 0.7f * 40f;
        currentYawVel = MathHelper.clamp(currentYawVel, -maxYawVel, maxYawVel);
        currentPitchVel = MathHelper.clamp(currentPitchVel, -maxPitchVel, maxPitchVel);

        float newYaw = player.getYaw() + currentYawVel * dt;
        float newPitch = player.getPitch() + currentPitchVel * dt;

        // overshoot — редко, только при быстром движении
        float speed = (float) Math.hypot(currentYawVel, currentPitchVel);
        if (speed > 180f && overshootTicks == 0 && RNG.nextFloat() < 0.012f) {
            overshootYaw = (RNG.nextFloat() - 0.5f) * 2f;
            overshootPitch = (RNG.nextFloat() - 0.5f) * 1.2f;
            overshootTicks = 2 + RNG.nextInt(3);
        }

        if (overshootTicks > 0) {
            overshootTicks--;
            newYaw += overshootYaw;
            newPitch += overshootPitch;
            overshootYaw *= 0.75f;
            overshootPitch *= 0.75f;
        }

        // gaussian tremor — не синусоидальный
        float tremorAmp = noiseAmount.getFloat();
        if (tremorAmp > 0f && distance > 3f) {
            if (tremorRefresh-- <= 0) {
                tremorRefresh = 2 + RNG.nextInt(4);
                tremorTargetYaw = (float) RNG.nextGaussian() * 0.10f;
                tremorTargetPitch = (float) RNG.nextGaussian() * 0.06f;
            }
            float tremorFactor = 1f - (float) Math.exp(-8f * dt);
            tremorYaw += (tremorTargetYaw - tremorYaw) * tremorFactor;
            tremorPitch += (tremorTargetPitch - tremorPitch) * tremorFactor;

            newYaw += tremorYaw * tremorAmp;
            newPitch += tremorPitch * tremorAmp;
        } else if (distance <= 3f) {
            tremorYaw *= 0.85f;
            tremorPitch *= 0.85f;
        }

        // GCD
        if (gcdFix.get()) {
            float g = gcd();
            newYaw = applyGcd(player.getYaw(), newYaw, g);
            newPitch = applyGcd(player.getPitch(), newPitch, g);
        }

        player.setYaw(newYaw);
        player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));
    }

    private static float gcd() {
        var mc = BfbMod.getClient();
        double sens = mc.options.getMouseSensitivity().getValue();
        float f = (float) (sens * 0.6 + 0.2);
        return Math.max(1.0e-5f, f * f * f * 1.2f);
    }

    private static float applyGcd(float from, float to, float gcd) {
        float delta = MathHelper.wrapDegrees(to - from);
        return from + Math.round(delta / gcd) * gcd;
    }

    private Vec3d getAimPoint(Entity e) {
        if (aimAtCenter.get()) return e.getBoundingBox().getCenter();
        var box = e.getBoundingBox();
        double y = box.minY + (box.maxY - box.minY) * 0.62;
        return new Vec3d(e.getX(), y, e.getZ());
    }

    private static double angleTo(net.minecraft.client.network.ClientPlayerEntity player, Entity e) {
        float[] rot = calculateRotations(player.getEyePos(), e.getBoundingBox().getCenter());
        return Math.abs(MathHelper.wrapDegrees(rot[0] - player.getYaw()));
    }

    private static double crosshairScore(net.minecraft.client.network.ClientPlayerEntity player, Entity e) {
        Vec3d look = player.getRotationVec(1f);
        Vec3d to = e.getBoundingBox().getCenter().subtract(player.getEyePos());
        double len = to.length();
        if (len < 1.0e-4) return 0;
        return 1.0 - look.dotProduct(to.multiply(1.0 / len));
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
}