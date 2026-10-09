package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.CameraModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void bfb$hurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        CameraModule camera = ModuleManager.get(CameraModule.class);
        if (camera != null && camera.isEnabled() && camera.hurt.get()) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bfb$bob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        CameraModule camera = ModuleManager.get(CameraModule.class);
        if (camera != null && camera.isEnabled() && camera.bob.get()) ci.cancel();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bfb$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        CameraModule module = ModuleManager.get(CameraModule.class);
        if (module == null || !module.isEnabled() || !module.zoomHeld()) return;
        cir.setReturnValue(Math.max(10f, cir.getReturnValue() / module.zoom.getFloat()));
    }
}