package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.FriendManager;
import com.example.bfb.RotationManager;
import com.example.bfb.combat.CombatController;
import com.example.bfb.combat.CombatProfile;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
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

    public final ModeSetting priority = add(new ModeSetting("Priority", "Angle",
            "Nearest", "Health", "Armor", "Angle", "Crosshair"));
    public final NumberSetting range = add(new NumberSetting("Range", 4.2, 2.5, 6.0, 0.1));
    public final NumberSetting fovThreshold = add(new NumberSetting("Assist FOV", 30.0, 10.0, 180.0, 5.0));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch delay", 15, 0, 40, 1));

    // === SMOOTHNESS ===
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 0.6, 0.1, 2.0, 0.05));
    public final NumberSetting noiseAmount = add(new NumberSetting("Micro tremor", 0.5, 0.0, 2.0, 0.1));
    public final NumberSetting reactionDelay = add(new NumberSetting("Reaction delay", 4, 0, 10, 1));
    public final NumberSetting maxAimSpeed = add(new NumberSetting("Max angular speed", 15, 8, 40, 1));
    public final NumberSetting missChance = add(new NumberSetting("Miss chance %", 10, 0, 30, 1));
    public final NumberSetting targetJitter = add(new NumberSetting("Target jitter", 0.05, 0.0, 0.2, 0.01));
    public final BooleanSetting aimAtCenter = add(new BooleanSetting("Aim at body", true));
    public final BooleanSetting leadPredict = add(new BooleanSetting("Lead prediction", true));
    public final NumberSetting leadFactor = add(new NumberSetting("Lead factor", 0.5, 0.0, 2.0, 0.1));
    public final BooleanSetting gcdFix = add(new BooleanSetting("GCD fix", true));

    // === ATTACK ===
    public final BooleanSetting autoAttack = add(new BooleanSetting("Auto attack", false));
    public final NumberSetting attackAngle = add(new NumberSetting("Attack angle", 4.0, 1.0, 20.0, 0.5));
    public final NumberSetting cooldownReady = add(new NumberSetting("Cooldown", 0.95, 0.70, 1.00, 0.01));
    public final BooleanSetting rageMode = add(new BooleanSetting("Rage (snap)", false));

    // === TARGETING ===
    public final BooleanSetting multiTarget = add(new BooleanSetting("Multi target", false));
    public final NumberSetting maxTargets = add(new NumberSetting("Max targets", 2, 1, 5, 1));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    // === STATE ===
    private float currentYawVel = 0f;
    private float currentPitchVel = 0f;
    private float smoothYaw = Float.NaN;
    private float smoothPitch = Float.NaN;
    private float tremorYaw = 0f;
    private float tremorPitch = 0f;
    private float tremorTargetYaw = 0f;
    private float tremorTargetPitch = 0f;
    private int tremorRefresh = 0;

    private float overshootYaw = 0f;
    private float overshootPitch = 0f;
    private int overshootTicks = 0;
    private int microPauseTicks = 0;
    private int missTicks = 0;
    private Vec3d currentTargetPoint = null;

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
        RotationManager.release();
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
        smoothYaw = Float.NaN;
        smoothPitch = Float.NaN;
    }

    public boolean hasAimTargetPublic() {
        return target != null && !rageMode.get();
    }

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
            currentTargetPoint = null;
            RotationManager.release();
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
            if (CombatUtil.fullAngle(player, e) > fovThreshold.getFloat()) continue;
            candidates.add(e);
        }

        if (candidates.isEmpty()) {
            target = null;
            lastTarget = null;
            currentTargetPoint = null;
            RotationManager.release();
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
            case "Angle" -> candidates.sort(Comparator.comparingDouble(e -> CombatUtil.angle(player, e)));
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
        }
        target = newTarget;

        if (reactionCooldown > 0) return;

        if (missTicks > 0) {
            missTicks--;
            return;
        }
        if (target != null && player.distanceTo(target) > 3.5
                && RNG.nextFloat() * 100f < missChance.getFloat()) {
            missTicks = 2 + RNG.nextInt(4);
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
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double maxLead = 2.0;
                    if (len > maxLead) {
                        double scale = maxLead / len;
                        dx *= scale;
                        dy *= scale;
                        dz *= scale;
                    }
                    aimPoint = aimPoint.add(dx, dy, dz);
                }
            }

            if (currentTargetPoint == null || RNG.nextInt(6) == 0) {
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
                continue;
            }

            // === ПЛАВНАЯ ИНТЕРПОЛЯЦИЯ ===
            aimSmooth(player, ideal[0], ideal[1]);

            if (autoAttack.get() && attackCooldown == 0) {
                boolean ready = player.getAttackCooldownProgress(0f) >= cooldownReady.getFloat();
                if (ready && CombatController.canAttack(player, ent)) {
                    CombatUtil.attack(ent, false);
                    attackCooldown = CombatController.attackDelay();
                    hits++;
                }
            } else if (!autoAttack.get() && hits == 0) {
                hits++;
            }
        }
    }

    /**
     * Плавное наведение через spring + smoothness factor.
     *
     * Ключевое отличие от CombatController.rotateTo:
     *   - smoothness увеличивает время реакции
     *   - GCD-snap применяется только если разница > 3 градусов
     *   - tremor и overshoot добавляются для человечности
     */
    private void aimSmooth(net.minecraft.client.network.ClientPlayerEntity player,
                           float targetYaw, float targetPitch) {
        if (Float.isNaN(smoothYaw)) {
            smoothYaw = player.getYaw();
            smoothPitch = player.getPitch();
        }

        if (microPauseTicks > 0) {
            microPauseTicks--;
            return;
        }
        if (RNG.nextFloat() < 0.008f) {
            microPauseTicks = 1 + RNG.nextInt(3);
            return;
        }

        float yawError = wrap(targetYaw - smoothYaw);
        float pitchError = targetPitch - smoothPitch;
        float distance = (float) Math.hypot(yawError, pitchError);

        if (distance < 0.4f) {
            currentYawVel *= 0.7f;
            currentPitchVel *= 0.7f;
            return;
        }

        // smoothness 0.6 = default, 2.0 = очень плавно
        float smoothMul = MathHelper.clamp(smoothness.getFloat(), 0.1f, 2.0f);
        float stiffness = 45f / smoothMul;
        float damping = 2f * (float) Math.sqrt(stiffness) * 1.0f;

        float yawAccel = yawError * stiffness - currentYawVel * damping;
        float pitchAccel = pitchError * stiffness - currentPitchVel * damping;

        float dt = 1f / 20f; // 20 TPS
        currentYawVel += yawAccel * dt;
        currentPitchVel += pitchAccel * dt;

        float maxYawVel = maxAimSpeed.getFloat() * 2f;
        float maxPitchVel = maxAimSpeed.getFloat() * 1.4f;
        currentYawVel = MathHelper.clamp(currentYawVel, -maxYawVel, maxYawVel);
        currentPitchVel = MathHelper.clamp(currentPitchVel, -maxPitchVel, maxPitchVel);

        smoothYaw += currentYawVel * dt;
        smoothPitch += currentPitchVel * dt;

        // tremor — только на малых дистанциях
        float tremorAmp = noiseAmount.getFloat();
        if (tremorAmp > 0f && distance < 20f) {
            if (tremorRefresh-- <= 0) {
                tremorRefresh = 3 + RNG.nextInt(5);
                tremorTargetYaw = (float) RNG.nextGaussian() * 0.08f;
                tremorTargetPitch = (float) RNG.nextGaussian() * 0.05f;
            }
            float tremorFactor = 1f - (float) Math.exp(-6f * dt);
            tremorYaw += (tremorTargetYaw - tremorYaw) * tremorFactor;
            tremorPitch += (tremorTargetPitch - tremorPitch) * tremorFactor;

            smoothYaw += tremorYaw * tremorAmp;
            smoothPitch += tremorPitch * tremorAmp;
        }

        // overshoot на быстрых поворотах
        float speed = (float) Math.hypot(currentYawVel, currentPitchVel);
        if (speed > 180f && overshootTicks == 0 && RNG.nextFloat() < 0.012f) {
            overshootYaw = (RNG.nextFloat() - 0.5f) * 1.5f;
            overshootPitch = (RNG.nextFloat() - 0.5f) * 1.0f;
            overshootTicks = 2 + RNG.nextInt(3);
        }
        if (overshootTicks > 0) {
            overshootTicks--;
            smoothYaw += overshootYaw;
            smoothPitch += overshootPitch;
            overshootYaw *= 0.75f;
            overshootPitch *= 0.75f;
        }

        // GCD-snap — только если ротация уже близко к цели
        if (gcdFix.get() && distance < 10f) {
            float gcd = gcd(player);
            smoothYaw = Math.round(smoothYaw / gcd) * gcd;
            smoothPitch = Math.round(smoothPitch / gcd) * gcd;
        }

        smoothPitch = MathHelper.clamp(smoothPitch, -90f, 90f);

        // Отправляем rotation
        CombatProfile profile = CombatController.profile();
        if (profile.silentRotation()) {
            RotationManager.setAndSend(smoothYaw, smoothPitch, 4);
        } else {
            player.setYaw(smoothYaw);
            player.setPitch(smoothPitch);
        }
    }

    private static float gcd(net.minecraft.client.network.ClientPlayerEntity player) {
        var mc = BfbMod.getClient();
        double sens = mc.options.getMouseSensitivity().getValue();
        float f = (float) (sens * 0.6 + 0.2);
        return Math.max(1.0e-5f, f * f * f * 1.2f);
    }

    private Vec3d getAimPoint(Entity e) {
        if (aimAtCenter.get()) return e.getBoundingBox().getCenter();
        var box = e.getBoundingBox();
        double y = box.minY + (box.maxY - box.minY) * 0.62;
        return new Vec3d(e.getX(), y, e.getZ());
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

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}