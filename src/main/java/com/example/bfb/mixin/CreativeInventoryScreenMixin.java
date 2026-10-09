package com.example.bfb.mixin;

import com.example.bfb.modules.PlayerModelModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {
    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void bfb$portrait(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        PlayerModelModule.drawPending(context);
    }
}
