package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.HitBoxesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EntityMixin — HitBoxes.
 *
 * ВАЖНО: setVelocityClient инжект УДАЛЁН.
 * Сглаживание velocity делает ClientPlayNetworkHandlerMixin.onEntityVelocityUpdate.
 * Двойное сглаживание давало неконсистентный knockback.
 */
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
}