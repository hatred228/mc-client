package com.example.bfb.mixin;

import com.example.bfb.modules.VisualsModule;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Скрывает vanilla nametag над мобами ТОЛЬКО если наш Nametags widget активен.
 *
 * Раньше тут был баг: проверялось hud.find("Nametags").isEnabled(),
 * что всегда true, потому что это HUD widget, а не BooleanSetting.
 *
 * Теперь проверяем VisualsModule.nametags.bool("Enabled") — реальную настройку.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void bfb$hideVanilla(LivingEntity entity, double squaredDistance,
                                  CallbackInfoReturnable<Boolean> cir) {
        VisualsModule vm = VisualsModule.get();
        if (vm == null) return;

        // скрываем ванильный только если наш nametag включён
        if (!vm.nametags.bool("Enabled")) return;

        cir.setReturnValue(false);
    }
}