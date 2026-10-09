package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoRenderModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void bfb$particle(ParticleEffect effect, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (hide(effect)) cir.setReturnValue(null);
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V", at = @At("HEAD"), cancellable = true)
    private void bfb$emitter(Entity entity, ParticleEffect effect, CallbackInfo ci) {
        if (hide(effect)) ci.cancel();
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V", at = @At("HEAD"), cancellable = true)
    private void bfb$emitterTicks(Entity entity, ParticleEffect effect, int maxAge, CallbackInfo ci) {
        if (hide(effect)) ci.cancel();
    }

    private static boolean hide(ParticleEffect effect) {
        NoRenderModule module = ModuleManager.get(NoRenderModule.class);
        return module != null && module.hideParticle(effect);
    }
}
