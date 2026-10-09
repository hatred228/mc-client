package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoRenderModule;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void bfb$fire(MatrixStack matrices, VertexConsumerProvider consumers, Sprite sprite, CallbackInfo ci) {
        NoRenderModule module = ModuleManager.get(NoRenderModule.class);
        if (module != null && module.isEnabled() && module.fire.get()) ci.cancel();
    }
}
