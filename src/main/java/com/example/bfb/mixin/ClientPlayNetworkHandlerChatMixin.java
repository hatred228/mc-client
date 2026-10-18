package com.example.bfb.mixin;

import com.example.bfb.modules.ChatUtilsModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerChatMixin {

    /** AntiReport — блок исходящих /report, содержащих наш ник. */
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void bfb$blockChat(String message, CallbackInfo ci) {
        ChatUtilsModule utils = ChatUtilsModule.get();
        if (utils != null && utils.shouldBlockOutgoing(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void bfb$blockCommand(String command, CallbackInfo ci) {
        ChatUtilsModule utils = ChatUtilsModule.get();
        if (utils != null && utils.shouldBlockOutgoing("/" + command)) {
            ci.cancel();
        }
    }

    /** Suffix — добавляет текст к исходящему сообщению. */
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String bfb$chatSuffix(String message) {
        ChatUtilsModule utils = ChatUtilsModule.get();
        if (utils == null || !utils.isEnabled()) return message;
        return utils.applySuffix(message);
    }
}