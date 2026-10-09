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

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void bfb$hideCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomVisualsModule mod = ModuleManager.get(CustomVisualsModule.class);
        if (mod != null && mod.isEnabled() && mod.customCrosshair.get()) ci.cancel();
    }
}