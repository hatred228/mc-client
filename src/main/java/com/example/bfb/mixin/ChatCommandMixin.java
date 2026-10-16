package com.example.bfb.mixin;

import com.example.bfb.modules.NeuroModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatCommandMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void bfb$neuroCommand(String message, CallbackInfo ci) {
        if (message == null) return;
        if (message.startsWith(".ai ")) {
            NeuroModule.handleCommand(message.substring(4));
            ci.cancel();
        }
    }
}