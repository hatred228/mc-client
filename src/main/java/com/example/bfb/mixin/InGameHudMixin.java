package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.CustomVisualsModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Пробует отменить ванильный crosshair когда CustomVisuals.customCrosshair включён.
 * require = 0 — не падать если метод не найден в текущей версии yarn.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void bfb$hideCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomVisualsModule mod = ModuleManager.get(CustomVisualsModule.class);
        if (mod != null && mod.isEnabled() && mod.customCrosshair.get()) {
            System.out.println("[BFB] hiding vanilla crosshair via renderCrosshair");
            ci.cancel();
        }
    }
}