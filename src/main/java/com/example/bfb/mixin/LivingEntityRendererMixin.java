package com.example.bfb.mixin;

import com.example.bfb.modules.NametagsModule;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void bfb$names(LivingEntity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        NametagsModule nametags = NametagsModule.instance;
        if (nametags != null && nametags.hideVanilla(entity)) cir.setReturnValue(false);
    }
}
