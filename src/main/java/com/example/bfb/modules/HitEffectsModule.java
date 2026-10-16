package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.ModuleManager;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HitEffectsModule extends CheatModule {

    public final ModeSetting particleStyle = add(new ModeSetting("Particles", "Firework",
            "Firework", "Crit", "Flame", "Snow", "Hearts", "Totem", "Souls", "Witch", "None"));

    public final ModeSetting soundStyle = add(new ModeSetting("Kill sound", "Pling",
            "Pling", "Bell", "Thunder", "Fart", "Moan", "None"));

    public final NumberSetting hitParticles = add(new NumberSetting("Hit particles", 12, 0, 60, 1));
    public final NumberSetting killParticles = add(new NumberSetting("Kill particles", 60, 0, 200, 5));
    public final BooleanSetting onHit = add(new BooleanSetting("On hit", true));
    public final BooleanSetting onKill = add(new BooleanSetting("On kill", true));
    public final BooleanSetting hitRing = add(new BooleanSetting("Hit ring", false));
    public final BooleanSetting killRing = add(new BooleanSetting("Kill ring", true));
    public final BooleanSetting killRingExpand = add(new BooleanSetting("Ring expand", true));
    public final ColorSetting ringColor = add(new ColorSetting("Ring color", 0xFFFF5A6A));
    public final NumberSetting ringSize = add(new NumberSetting("Ring size", 0.5, 0.1, 2.0, 0.05));
    public final NumberSetting ringLife = add(new NumberSetting("Ring life (ms)", 700, 200, 2000, 50));

    private static final Random RNG = new Random();
    private static final List<Ring> RINGS = new ArrayList<>();
    private Entity lastTarget;
    private boolean rewarded;

    public HitEffectsModule() {
        super("HitEffects", "Particles and sound on hit/kill", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void onAttack(Entity target) {
        HitEffectsModule mod = ModuleManager.get(HitEffectsModule.class);
        if (mod == null || !mod.isEnabled() || target == null) return;
        var client = BfbMod.getClient();
        if (client.player == null) return;

        if (mod.onHit.get()) {
            mod.burst(target, mod.hitParticles.getInt(), false);
            if (mod.hitRing.get()) {
                mod.spawnRing(target, false);
            }
        }
        mod.lastTarget = target;
        mod.rewarded = false;
    }

    @Override
    public void onTick() {
        if (lastTarget == null || rewarded) return;
        var client = BfbMod.getClient();
        if (client.world == null) { lastTarget = null; return; }
        Entity target = client.world.getEntityById(lastTarget.getId());
        boolean dead = target == null || (target instanceof LivingEntity le && !le.isAlive());
        if (!dead) return;

        rewarded = true;
        if (onKill.get() && lastTarget != null) {
            burst(lastTarget, killParticles.getInt(), true);
            if (killRing.get()) spawnRing(lastTarget, true);
        }
        playKillSound();
        lastTarget = null;

        long now = System.currentTimeMillis();
        RINGS.removeIf(r -> now - r.born > ringLife.getInt());
    }

    private void spawnRing(Entity target, boolean kill) {
        RINGS.add(new Ring(target.getX(), target.getY() + target.getHeight() * 0.5,
                target.getZ(), System.currentTimeMillis(), kill));
    }

    private void burst(Entity entity, int count, boolean kill) {
        if (count <= 0) return;
        var client = BfbMod.getClient();
        if (client.world == null) return;
        ParticleEffect type = pickParticle();
        if (type == null) return;

        double cx = entity.getX();
        double cy = entity.getY() + entity.getHeight() * 0.5;
        double cz = entity.getZ();
        double speedScale = kill ? 1.8 : 1.0;

        for (int i = 0; i < count; i++) {
            double dx = (RNG.nextDouble() - 0.5) * 1.2 * speedScale;
            double dy = RNG.nextDouble() * 1.0 * speedScale;
            double dz = (RNG.nextDouble() - 0.5) * 1.2 * speedScale;
            client.particleManager.addParticle(type, cx, cy, cz, dx, dy + 0.2, dz);
        }
        // дополнительный слой — другой тип частиц
        if (kill) {
            ParticleEffect layer2 = secondaryParticle();
            if (layer2 != null) {
                int n2 = count / 2;
                for (int i = 0; i < n2; i++) {
                    double dx = (RNG.nextDouble() - 0.5) * 2.0;
                    double dy = RNG.nextDouble() * 1.5;
                    double dz = (RNG.nextDouble() - 0.5) * 2.0;
                    client.particleManager.addParticle(layer2, cx, cy, cz, dx, dy, dz);
                }
            }
        }
    }

    private ParticleEffect pickParticle() {
        return switch (particleStyle.get()) {
            case "Firework" -> ParticleTypes.FIREWORK;
            case "Crit" -> ParticleTypes.CRIT;
            case "Flame" -> ParticleTypes.FLAME;
            case "Snow" -> ParticleTypes.SNOWFLAKE;
            case "Hearts" -> ParticleTypes.HEART;
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "Souls" -> ParticleTypes.SOUL;
            case "Witch" -> ParticleTypes.WITCH;
            default -> null;
        };
    }

    private ParticleEffect secondaryParticle() {
        return switch (particleStyle.get()) {
            case "Firework" -> ParticleTypes.END_ROD;
            case "Crit" -> ParticleTypes.ENCHANTED_HIT;
            case "Flame" -> ParticleTypes.LAVA;
            case "Snow" -> ParticleTypes.ITEM_SNOWBALL;
            case "Hearts" -> ParticleTypes.DAMAGE_INDICATOR;
            case "Totem" -> ParticleTypes.END_ROD;
            case "Souls" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "Witch" -> ParticleTypes.WITCH;
            default -> null;
        };
    }

    private void playKillSound() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;
        switch (soundStyle.get()) {
            case "Bell" -> player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1f, 1.2f);
            case "Thunder" -> player.playSound(net.minecraft.sound.SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
            case "Fart" -> player.playSound(net.minecraft.sound.SoundEvent.of(
                    net.minecraft.util.Identifier.of("undetected", "fart")), 1f, 1f);
            case "Moan" -> player.playSound(net.minecraft.sound.SoundEvent.of(
                    net.minecraft.util.Identifier.of("undetected", "moan")), 1f, 1f);
            case "None" -> {}
            default -> player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 1f);
        }
    }

    // ─── RENDER ──────────────────────────────────────────────

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        long now = System.currentTimeMillis();
        RINGS.removeIf(r -> now - r.born > ringLife.getInt());
        int lifeMs = ringLife.getInt();
        int rgb = ringColor.getRgb() & 0xFFFFFF;

        for (Ring r : RINGS) {
            float t = (now - r.born) / (float) lifeMs;
            if (t > 1f) continue;

            double base = ringSize.get();
            double size = killRingExpand.get() ? base * (1.0 + t * 2.0) : base;
            int alpha = (int) ((1f - t) * 220);

            int segments = 48;
            double y = r.y;
            for (int i = 0; i < segments; i++) {
                double a1 = Math.PI * 2 * i / segments;
                double a2 = Math.PI * 2 * (i + 1) / segments;
                Vec3d p1 = new Vec3d(r.x + Math.cos(a1) * size, y, r.z + Math.sin(a1) * size);
                Vec3d p2 = new Vec3d(r.x + Math.cos(a2) * size, y, r.z + Math.sin(a2) * size);
                RenderUtil.tracer(matrices, consumers, p1, p2, camera, (alpha << 24) | rgb, 2f);
            }

            // второй круг для kill
            if (r.kill) {
                double size2 = size * 0.7;
                int a2 = (int) ((1f - t) * 150);
                for (int i = 0; i < segments; i++) {
                    double a1 = Math.PI * 2 * i / segments + t * 4;
                    double a12 = Math.PI * 2 * (i + 1) / segments + t * 4;
                    Vec3d p1 = new Vec3d(r.x + Math.cos(a1) * size2, y + 0.05, r.z + Math.sin(a1) * size2);
                    Vec3d p2 = new Vec3d(r.x + Math.cos(a12) * size2, y + 0.05, r.z + Math.sin(a12) * size2);
                    RenderUtil.tracer(matrices, consumers, p1, p2, camera, (a2 << 24) | 0xFFFFFF, 1.5f);
                }
            }
        }
    }

    private record Ring(double x, double y, double z, long born, boolean kill) {}
}