package com.example.bfb.mixin;

import com.example.bfb.modules.PlayerModelModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Inject(method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void bfb$hideDoll(DrawContext context, int x1, int y1, int x2, int y2, int size, float f, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        if (!PlayerModelModule.replacesPlayer() || entity != MinecraftClient.getInstance().player) return;
        PlayerModelModule.remember(x1, y1, x2 - x1, y2 - y1);
        ci.cancel();
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void bfb$portrait(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        PlayerModelModule.drawPending(context);
    }
}
