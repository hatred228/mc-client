package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NeuroModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class NeuroMovementMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bfb$neuroMovement(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        NeuroModule neuro = ModuleManager.get(NeuroModule.class);
        if (neuro == null || !neuro.isPlaying()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        self.forwardSpeed = neuro.overrideForward;
        self.sidewaysSpeed = neuro.overrideSideways;

        if (neuro.overrideJump && self.isOnGround()) {
            self.jump();
        }
        self.setSprinting(neuro.overrideSprint);
    }
}