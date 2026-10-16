package com.example.bfb.mixin;

import com.example.bfb.modules.ChatUtilsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientReportBlockMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void bfb$blockReportScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) return;
        ChatUtilsModule utils = ChatUtilsModule.get();
        if (utils != null && utils.shouldBlockReportScreen(screen)) {
            ci.cancel();
        }
    }
}