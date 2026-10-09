package com.example.bfb.mixin;

import com.example.bfb.modules.NametagsModule;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void bfb$names(PlayerLikeEntity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        NametagsModule nametags = NametagsModule.instance;
        if (nametags != null && nametags.hideVanilla(entity)) cir.setReturnValue(false);
    }
}