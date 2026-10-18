package com.example.bfb.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает ParticleS2CPacket от сервера и блокирует NO_RENDER по ID.
 * Работает до того как vanilla создаст партикл.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerParticleMixin {

    @Inject(method = "onParticle", at = @At("HEAD"), cancellable = true)
    private void bfb$blockNoRenderParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        try {
            var parameters = packet.getParameters();
            if (parameters == null) return;

            var type = parameters.getType();
            if (type == null) return;

            var id = Registries.PARTICLE_TYPE.getId(type);
            if (id != null) {
                String path = id.getPath().toLowerCase();
                if (path.contains("no_render") || path.contains("norender")) {
                    ci.cancel();
                }
            }
        } catch (Throwable ignored) {}
    }
}