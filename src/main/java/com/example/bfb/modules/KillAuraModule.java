package com.example.bfb.modules;

import com.example.bfb.AimController;
import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.FriendManager;
import com.example.bfb.HudStyle;
import com.example.bfb.RenderUtil;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAuraModule extends CheatModule {
    public static Entity target;

    public final ModeSetting mode = add(new ModeSetting("Mode", "Smooth", "Rage", "Smooth", "Legit"));
    public final ModeSetting targetMode = add(new ModeSetting("Target", "Nearest",
            "Nearest", "Health", "Angle", "Crosshair"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 4.5, 0.1));
    public final NumberSetting maxTargets = add(new NumberSetting("Max targets", 1, 1, 5, 1));
    public final NumberSetting attackAngle = add(new NumberSetting("Attack angle", 60.0, 10.0, 180.0, 5.0));
    public final BooleanSetting silentAim = add(new BooleanSetting("Silent aim", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    // === visuals ===
    public final BooleanSetting ring = add(new BooleanSetting("Target ring", true));
    public final BooleanSetting ringRotate = add(new BooleanSetting("Ring rotate", true));
    public final BooleanSetting hitFlash = add(new BooleanSetting("Hit flash", true));
    public final BooleanSetting hitPredict = add(new BooleanSetting("Hit predict", false));
    public final ColorSetting color1 = add(new ColorSetting("Ring color 1", 0x9C5CFF));
    public final ColorSetting color2 = add(new ColorSetting("Ring color 2", 0xFF5CA8));
    public final ColorSetting flashColor = add(new ColorSetting("Flash color", 0xFF3B30));

    private final Random RNG = new Random();
    private int attackCooldown;
    private static final List<HitMarker> HITS = new ArrayList<>();

    public KillAuraModule() {
        super("KillAura", "Attacks targets with silent aim and visuals", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        target = null;
        attackCooldown = 0;
        HITS.clear();
        AimController.reset();
        RotationManager.release();
    }

    @Override
    public void onTick() {
        if (attackCooldown > 0) attackCooldown--;

        long now = System.currentTimeMillis();
        HITS.removeIf(h -> now - h.time > 400);

        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        target = null;
        if (player == null || world == null || client.currentScreen != null) {
            AimController.clearTarget();
            RotationManager.release();
            return;
        }

        boolean rage = mode.is("Rage");
        double reach = range.get();

        // сбор кандидатов — FOV считается от silent yaw если он активен
        List<Entity> candidates = new ArrayList<>();
        for (Entity e : com.example.bfb.EntityCache.living()) {
            if (!CombatUtil.accept(e, player, players.get(), mobs.get(), animals.get())) continue;
            if (FriendManager.isFriend(e)) continue;
            if (e.squaredDistanceTo(player) > reach * reach) continue;
            if (!throughWalls.get() && !CombatUtil.canSee(player, e)) continue;
            candidates.add(e);
        }
        if (candidates.isEmpty()) {
            AimController.clearTarget();
            RotationManager.release();
            return;
        }

        switch (targetMode.get()) {
            case "Health" -> candidates.sort(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()));
            case "Angle" -> candidates.sort(Comparator.comparingDouble(e -> CombatUtil.fullAngle(player, e)));
            case "Crosshair" -> candidates.sort(Comparator.comparingDouble(e -> crosshairScore(player, e)));
            default -> candidates.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
        }

        int max = maxTargets.getInt();
        int hit = 0;
        Entity primary = candidates.get(0);
        target = primary;

        // === наводка ===
        if (silentAim.get() && !rage && primary instanceof LivingEntity living) {
            Vec3d aimPoint = getAimPoint(living);
            float[] ideal = calculateRotations(player.getEyePos(), aimPoint);
            // цель — silent, камера не двигается
            float speed01 = mode.is("Smooth") ? 0.55f : 0.75f;
            AimController.setTarget(ideal[0], ideal[1], speed01, 0.05f, 0);
        } else if (rage && primary != null) {
            Vec3d aimPoint = getAimPoint(primary);
            float[] ideal = calculateRotations(player.getEyePos(), aimPoint);
            RotationManager.set(ideal[0], ideal[1]);
        }

        for (Entity best : candidates) {
            if (hit >= max) break;

            float angleToTarget = CombatUtil.fullAngle(player, best);
            boolean inAngle = rage || silentAim.get() || angleToTarget <= attackAngle.getFloat();

            if (attackCooldown == 0
                    && player.getAttackCooldownProgress(0f) >= 0.92f
                    && player.distanceTo(best) <= reach + 0.4
                    && inAngle) {
                CombatUtil.attack(best);
                if (hitFlash.get() && best instanceof LivingEntity living) {
                    HITS.add(new HitMarker(living, now));
                }
                attackCooldown = 1 + RNG.nextInt(2);
                hit++;
            }
        }
    }

    private static Vec3d getAimPoint(Entity e) {
        var box = e.getBoundingBox();
        double y = box.minY + (box.maxY - box.minY) * 0.62;
        return new Vec3d(e.getX(), y, e.getZ());
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

    private static double crosshairScore(net.minecraft.client.network.ClientPlayerEntity player, Entity e) {
        Vec3d look = player.getRotationVec(1f);
        Vec3d to = e.getBoundingBox().getCenter().subtract(player.getEyePos());
        double len = to.length();
        if (len < 1.0e-4) return 0;
        return 1.0 - look.dotProduct(to.multiply(1.0 / len));
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.player == null) return;
        float tick = client.getRenderTickCounter().getTickProgress(false);
        long time = System.currentTimeMillis();

        if (ring.get() && target instanceof LivingEntity entity && entity.isAlive()) {
            Vec3d pos = entity.getLerpedPos(tick);
            double r = entity.getWidth() * 0.95;
            int segments = 72;
            float angleOffset = ringRotate.get() ? (time / 800f) : 0f;

            for (int i = 0; i < segments; i++) {
                double a1 = Math.PI * 2 * i / segments;
                double a2 = Math.PI * 2 * (i + 1) / segments;

                float t1 = i / (float) segments;
                int c1 = HudStyle.lerpColor(color1.getRgb() & 0xFFFFFF, color2.getRgb() & 0xFFFFFF, t1);

                float pulse = 0.5f + 0.5f * (float) Math.sin(time / 400.0 + i * 0.3);
                int alpha = (int) (180 + 75 * pulse);

                double y = pos.y + 0.05 + Math.sin(a1 * 4 + time / 300.0 + angleOffset) * 0.06;

                Vec3d p1 = new Vec3d(pos.x + Math.cos(a1) * r, y, pos.z + Math.sin(a1) * r);
                Vec3d p2 = new Vec3d(pos.x + Math.cos(a2) * r, y, pos.z + Math.sin(a2) * r);
                RenderUtil.bone(matrices, consumers, camera, p1, p2, (alpha << 24) | c1, 2.5f);
            }

            if (hitPredict.get()) {
                Vec3d center = entity.getBoundingBox().getCenter();
                double ring2 = entity.getWidth() * 0.35;
                int pseg = 20;
                for (int i = 0; i < pseg; i++) {
                    double a1 = Math.PI * 2 * i / pseg;
                    double a2 = Math.PI * 2 * (i + 1) / pseg;
                    RenderUtil.bone(matrices, consumers, camera,
                            center.add(Math.cos(a1) * ring2, 0, Math.sin(a1) * ring2),
                            center.add(Math.cos(a2) * ring2, 0, Math.sin(a2) * ring2),
                            (200 << 24) | 0xFFFFFF, 1.4f);
                }
            }
        }

        for (HitMarker hit : HITS) {
            if (hit.entity.isRemoved()) continue;
            float t = (System.currentTimeMillis() - hit.time) / 400f;
            if (t > 1f) continue;
            int alpha = (int) ((1f - t) * 255);
            Vec3d pos = hit.entity.getLerpedPos(tick);
            double r = hit.entity.getWidth() * (1.0 + t * 0.6);
            int seg = 24;
            int fc = flashColor.getRgb() & 0xFFFFFF;
            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg;
                double a2 = Math.PI * 2 * (i + 1) / seg;
                double y = pos.y + 0.05 + hit.entity.getHeight() * t * 0.5;
                RenderUtil.bone(matrices, consumers, camera,
                        new Vec3d(pos.x + Math.cos(a1) * r, y, pos.z + Math.sin(a1) * r),
                        new Vec3d(pos.x + Math.cos(a2) * r, y, pos.z + Math.sin(a2) * r),
                        (alpha << 24) | fc, 3f);
            }
        }
    }

    private static final class HitMarker {
        final LivingEntity entity;
        final long time;
        HitMarker(LivingEntity e, long t) { entity = e; time = t; }
    }
}