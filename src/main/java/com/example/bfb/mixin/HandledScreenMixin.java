package com.example.bfb.mixin;

import com.example.bfb.ShulkerPreview;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Shadow protected Slot focusedSlot;

    @Inject(method = "drawMouseoverTooltip", at = @At("TAIL"))
    private void bfb$shulker(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        ShulkerPreview.draw(context, focusedSlot, mouseX, mouseY);
    }
}
