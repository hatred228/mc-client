package com.example.bfb.mixin;

import com.example.bfb.AimController;
import com.example.bfb.ModuleManager;
import com.example.bfb.modules.FastPlaceModule;
import com.example.bfb.modules.FreecamModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow private int itemUseCooldown;

    private static long bfb$lastFrameNanos = 0L;

    @Inject(method = "render", at = @At("HEAD"))
    private void bfb$aimFrame(boolean tick, CallbackInfo ci) {
        AimController.updateFrame();

        FreecamModule freecam = ModuleManager.get(FreecamModule.class);
        if (freecam != null && freecam.isEnabled()) {
            long now = System.nanoTime();
            float deltaSeconds = bfb$lastFrameNanos == 0L ? (1f / 60f) : (now - bfb$lastFrameNanos) / 1.0e9f;
            bfb$lastFrameNanos = now;
            if (deltaSeconds <= 0f) deltaSeconds = 1f / 60f;
            if (deltaSeconds > 0.1f) deltaSeconds = 0.1f;
            freecam.onFrame(deltaSeconds);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bfb$fastPlace(CallbackInfo ci) {
        FastPlaceModule fastPlace = FastPlaceModule.instance;
        if (fastPlace == null || !fastPlace.isEnabled()) return;
        int min = Math.max(2, fastPlace.delay.getInt());
        if (itemUseCooldown > min) {
            itemUseCooldown = min;
        }
    }
}