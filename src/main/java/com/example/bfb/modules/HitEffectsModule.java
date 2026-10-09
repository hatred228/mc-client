package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.ModuleManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

/** Эффекты при убийстве: вспышка частиц и звук (как в Celestial). Вызывается из AttackSoundMixin. */
public class HitEffectsModule extends CheatModule {
    public final ModeSetting sound = add(new ModeSetting("Sound", "Pling", "Pling", "Fart", "Moan", "Thunder"));
    public final NumberSetting particles = add(new NumberSetting("Particles", 40, 0, 200, 5));
    public final BooleanSetting onHit = add(new BooleanSetting("On Hit", true));

    private Entity lastTarget;
    private boolean rewarded;

    public HitEffectsModule() {
        super("HitEffects", "Particles and sound on kill", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    /** Хук из AttackSoundMixin: вызывается при каждом ударе по сущности. */
    public static void onAttack(Entity target) {
        HitEffectsModule mod = ModuleManager.get(HitEffectsModule.class);
        if (mod == null || !mod.isEnabled() || target == null) return;
        var client = BfbMod.getClient();
        if (mod.onHit.get() && client.player != null) {
            mod.burst(target, mod.particles.getInt() / 4);
        }
        mod.lastTarget = target;
        mod.rewarded = false;
    }

    @Override
    public void onTick() {
        if (lastTarget == null || rewarded) return;
        var client = BfbMod.getClient();
        if (client.world == null) {
            lastTarget = null;
            return;
        }
        Entity target = client.world.getEntityById(lastTarget.getId());
        boolean dead = target == null || target instanceof LivingEntity living && !living.isAlive();
        if (!dead) return;
        rewarded = true;
        lastTarget = null;
        if (client.player != null) {
            burstAt(client.player.getX(), client.player.getEyeY(), client.player.getZ(), particles.getInt());
            playKillSound();
        }
    }

    private void burst(Entity entity, int count) {
        burstAt(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ(), count);
    }

    private void burstAt(double x, double y, double z, int count) {
        var client = BfbMod.getClient();
        if (client.world == null || count <= 0) return;
        for (int i = 0; i < count; i++) {
            double dx = (Math.random() - 0.5) * 1.2;
            double dy = Math.random() * 1.2;
            double dz = (Math.random() - 0.5) * 1.2;
            client.particleManager.addParticle(ParticleTypes.FIREWORK, x, y, z, dx, dy + 0.3, dz);
            if (i % 3 == 0) client.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, dx * 0.6, dy, dz * 0.6);
        }
    }

    private void playKillSound() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;
        switch (sound.get()) {
            case "Fart" -> {
                var mod = ModuleManager.get(HitSoundModule.class);
                if (mod != null) mod.onHit();
            }
            case "Moan" -> player.playSound(net.minecraft.sound.SoundEvent.of(
                    net.minecraft.util.Identifier.of("libbase", "moan")), 1f, 1f);
            case "Thunder" -> player.playSound(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
            default -> player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 1f);
        }
    }
}
