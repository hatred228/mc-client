package com.example.bfb.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/**
 * Экстренная защита: удаляем NO_RENDER партиклы из менеджера перед tick(),
 * если они там уже есть. Ловит случай когда партикл проскочил мимо addParticle.
 *
 * Определяем NO_RENDER по ID регистра через рефлексию на партикле.
 */
@Mixin(ParticleManager.class)
public class ParticleTickGuardMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void bfb$purgeNoRender(CallbackInfo ci) {
        try {
            ParticleManager self = (ParticleManager) (Object) this;

            for (Field field : ParticleManager.class.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object raw = field.get(self);
                if (!(raw instanceof Map<?, ?> map)) continue;

                for (Object value : new ArrayList<>(map.values())) {
                    if (!(value instanceof Iterable<?> iterable)) continue;
                    try {
                        java.util.Iterator<?> it = iterable.iterator();
                        while (it.hasNext()) {
                            Object next = it.next();
                            if (!(next instanceof Particle p)) continue;
                            if (bfb$isNoRenderParticle(p)) {
                                it.remove();
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /** Проверка через ID типа партикла. */
    private static boolean bfb$isNoRenderParticle(Particle p) {
        try {
            // Ищем поле "type" типа ParticleType или getType() метод
            Class<?> cls = p.getClass();
            while (cls != null && cls != Object.class) {
                for (Field f : cls.getDeclaredFields()) {
                    if (!f.getType().getName().contains("ParticleType")) continue;
                    f.setAccessible(true);
                    Object type = f.get(p);
                    if (type == null) continue;
                    var id = Registries.PARTICLE_TYPE.getId((net.minecraft.particle.ParticleType<?>) type);
                    if (id != null) {
                        String path = id.getPath().toLowerCase();
                        if (path.contains("no_render") || path.contains("norender")) {
                            return true;
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
        } catch (Throwable ignored) {}
        return false;
    }
}