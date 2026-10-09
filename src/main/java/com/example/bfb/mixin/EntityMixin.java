package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.HitBoxesModule;
import com.example.bfb.modules.VelocityModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void bfb$hitbox(CallbackInfoReturnable<Float> cir) {
        HitBoxesModule boxes = ModuleManager.get(HitBoxesModule.class);
        if (boxes == null || !boxes.isEnabled()) return;
        if ((Object) this == MinecraftClient.getInstance().player) return;
        if (!((Object) this instanceof LivingEntity)) return;
        cir.setReturnValue(cir.getReturnValue() + boxes.expand.getFloat());
    }

    @Inject(method = "setVelocityClient", at = @At("HEAD"), cancellable = true)
    private void bfb$velocity(Vec3d velocity, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        VelocityModule module = ModuleManager.get(VelocityModule.class);
        if (module == null || !module.isEnabled()) return;
        double[] out = module.smooth(velocity.x, velocity.y, velocity.z);
        Entity self = (Entity) (Object) this;
        self.setVelocity(out[0], out[1], out[2]);
        ci.cancel();
    }
}