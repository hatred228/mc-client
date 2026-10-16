package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoSlowdownModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LivingEntityMovementMixin — NoSlowdown.
 *
 * Vanilla применяет множитель 0.2 к скорости когда игрок использует item
 * (еда, лук, щит). Модуль возвращает нормальную скорость.
 *
 * ВАЖНО: НЕ отдаём скорость если игрок стоит — иначе палится как "движение без input".
 */
@Mixin(LivingEntity.class)
public class LivingEntityMovementMixin {

    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    private void bfb$noSlowdown(CallbackInfoReturnable<Float> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        NoSlowdownModule mod = ModuleManager.get(NoSlowdownModule.class);
        if (mod == null || !mod.isEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isUsingItem()) return;

        // === если игрок не двигается — не отдаём скорость ===
        if (!mod.checkOnlyMoving((net.minecraft.client.network.ClientPlayerEntity) self)) return;

        float original = cir.getReturnValue();
        float restored = mod.effectiveSpeed() / 0.2f;

        // кап на 1.0 — не даём превышать нормальную скорость
        float result = Math.min(original * restored, 1.0f);

        cir.setReturnValue(result);
    }
}