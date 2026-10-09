package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoJumpDelayModule;
import com.example.bfb.modules.VelocityModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Shadow private int jumpingCooldown;
    private static Vec3d bfb$before;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bfb$jumpDelay(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        NoJumpDelayModule module = ModuleManager.get(NoJumpDelayModule.class);
        if (module != null && module.isEnabled()) jumpingCooldown = 0;
    }

    @Inject(method = "takeKnockback", at = @At("HEAD"))
    private void bfb$before(double strength, double x, double z, CallbackInfo ci) {
        bfb$before = null;
        if ((Object) this != MinecraftClient.getInstance().player) return;
        VelocityModule velocity = ModuleManager.get(VelocityModule.class);
        if (velocity == null || !velocity.isEnabled()) return;
        bfb$before = ((LivingEntity) (Object) this).getVelocity();
    }

    @Inject(method = "takeKnockback", at = @At("RETURN"))
    private void bfb$after(double strength, double x, double z, CallbackInfo ci) {
        if (bfb$before == null) return;
        VelocityModule velocity = ModuleManager.get(VelocityModule.class);
        if (velocity == null) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        Vec3d now = entity.getVelocity();
        double[] out = velocity.smooth(now.x - bfb$before.x, now.y - bfb$before.y, now.z - bfb$before.z);
        entity.setVelocity(
                bfb$before.x + out[0],
                bfb$before.y + out[1],
                bfb$before.z + out[2]);
        bfb$before = null;
    }
}