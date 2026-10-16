package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoSlowdownModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NoSlowdown — восстановление скорости при использовании item.
 *
 * В 1.21.11 slowdown от использования item применяется в tickMovement.
 * Перехватываем и компенсируем.
 */
@Mixin(ClientPlayerEntity.class)
public class NoSlowdownMixin {

    private Vec3d bfb$beforeUse = null;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bfb$captureBefore(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (self != MinecraftClient.getInstance().player) return;

        NoSlowdownModule mod = ModuleManager.get(NoSlowdownModule.class);
        if (mod == null || !mod.isEnabled()) return;
        if (!self.isUsingItem()) { bfb$beforeUse = null; return; }

        // сохраняем velocity ДО применения slowdown
        bfb$beforeUse = self.getVelocity();
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void bfb$restoreAfter(CallbackInfo ci) {
        if (bfb$beforeUse == null) return;

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (self != MinecraftClient.getInstance().player) { bfb$beforeUse = null; return; }

        NoSlowdownModule mod = ModuleManager.get(NoSlowdownModule.class);
        if (mod == null || !mod.isEnabled()) { bfb$beforeUse = null; return; }
        if (!self.isUsingItem()) { bfb$beforeUse = null; return; }

        // получаем текущую скорость (после vanilla slowdown)
        Vec3d current = self.getVelocity();

        // horizontal была уменьшена на 0.2
        // восстанавливаем пропорционально effectiveSpeed
        float targetMult = mod.effectiveSpeed() / 0.2f;
        double currentX = current.x;
        double currentZ = current.z;
        double currentLen = Math.hypot(currentX, currentZ);

        // если мы движемся — восстанавливаем
        if (currentLen > 0.001) {
            double beforeLen = Math.hypot(bfb$beforeUse.x, bfb$beforeUse.z);
            // применяем коэффициент от оригинала
            double newLen = Math.min(beforeLen, currentLen * targetMult);
            double scale = newLen / currentLen;
            self.setVelocity(currentX * scale, current.y, currentZ * scale);
        } else if (Math.hypot(bfb$beforeUse.x, bfb$beforeUse.z) > 0.001) {
            // vanilla обнулила — восстанавливаем
            double scale = targetMult;
            self.setVelocity(bfb$beforeUse.x * scale, current.y, bfb$beforeUse.z * scale);
        }

        bfb$beforeUse = null;
    }
}