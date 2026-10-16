package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoJumpDelayModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntityMixin — NoJumpDelay.
 *
 * ВАЖНО: takeKnockback хуки УДАЛЕНЫ.
 * Сглаживание velocity делает ClientPlayNetworkHandlerMixin.onEntityVelocityUpdate.
 * Двойное сглаживание давало неконсистентный knockback.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Shadow private int jumpingCooldown;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bfb$jumpDelay(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        NoJumpDelayModule module = ModuleManager.get(NoJumpDelayModule.class);
        if (module != null && module.isEnabled()) jumpingCooldown = 0;
    }
}