package com.example.bfb.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashScreenMixin {

    @Shadow private float progress;

    @Inject(method = "render", at = @At("HEAD"))
    private void bfb$blackStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        context.fill(0, 0, width, height, 0xFF000000);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bfb$blackEndAndText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        context.fill(0, 0, width, height, 0xFF000000);

        String text = "injecting";
        int percent = Math.max(1, Math.min(100, (int) (progress * 100)));
        String percentText = percent + "%";

        int textWidth = client.textRenderer.getWidth(text);
        int percentWidth = client.textRenderer.getWidth(percentText);
        int y = height / 2 - 20;

        context.drawText(client.textRenderer, text, (width - textWidth) / 2, y, 0xFFFFFFFF, true);
        context.drawText(client.textRenderer, percentText, (width - percentWidth) / 2, y + 20, 0xFFFFFFFF, true);
    }
}