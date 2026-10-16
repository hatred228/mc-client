package com.example.bfb.mixin;

import com.example.bfb.modules.ChatUtilsModule;
import com.example.bfb.modules.ServerAlertModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void bfb$onChat(Text message, CallbackInfo ci) {
        String raw = message.getString();
        ServerAlertModule alert = ServerAlertModule.instance();
        if (alert != null && alert.isEnabled()) {
            alert.onChatMessage(raw);
        }
        ChatUtilsModule utils = ChatUtilsModule.get();
        if (utils != null && utils.shouldHide(raw)) {
            ci.cancel();
        }
    }

    /** NameProtect через пересборку Text с сохранением стиля. */
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text bfb$nameProtect(Text message) {
        ChatUtilsModule utils = ChatUtilsModule.get();
        if (utils == null || !utils.shouldProtectInChat()) return message;

        try {
            String raw = message.getString();
            String protectedText = utils.protectText(raw);
            if (raw.equals(protectedText)) return message;

            // пересобираем с корневым стилем
            MutableText rebuilt = Text.literal(protectedText);
            if (message.getStyle() != null) rebuilt.setStyle(message.getStyle());
            return rebuilt;
        } catch (Throwable t) {
            return message;
        }
    }
}