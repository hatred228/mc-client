package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoRenderModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Защита от NO_RENDER партиклов сервера + NoRender модуль.
 *
 * Ванильный баг 1.21.11: серверные партиклы типа "no_render" падают с NPE
 * при tick потому что spriteProvider = null.
 *
 * Блокируем по ID регистра: "no_render" / "norender".
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void bfb$particle(ParticleEffect effect, double x, double y, double z,
                              double vx, double vy, double vz,
                              CallbackInfoReturnable<Particle> cir) {
        if (shouldBlock(effect)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V",
            at = @At("HEAD"), cancellable = true)
    private void bfb$emitter(Entity entity, ParticleEffect effect, CallbackInfo ci) {
        if (shouldBlock(effect)) ci.cancel();
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V",
            at = @At("HEAD"), cancellable = true)
    private void bfb$emitterTicks(Entity entity, ParticleEffect effect, int maxAge, CallbackInfo ci) {
        if (shouldBlock(effect)) ci.cancel();
    }

    private static boolean shouldBlock(ParticleEffect effect) {
        if (effect == null) return false;

        // === блокируем ванильный баг по ID ===
        try {
            ParticleType<?> type = effect.getType();
            if (type == null) return false;

            var id = Registries.PARTICLE_TYPE.getId(type);
            if (id != null) {
                String path = id.getPath().toLowerCase();
                if (path.contains("no_render") || path.contains("norender")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        // === обычный NoRender модуль ===
        NoRenderModule mod = ModuleManager.get(NoRenderModule.class);
        return mod != null && mod.hideParticle(effect);
    }
}