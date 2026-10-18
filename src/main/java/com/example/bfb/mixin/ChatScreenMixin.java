package com.example.bfb.mixin;

import com.example.bfb.hud.HudIntegration;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Перехват кликов в ChatScreen для HUD drag & drop.
 *
 * mouseClicked работает через ChatScreen.
 * mouseReleased отсутствует в ChatScreen — обрабатываем release
 * через glfwGetMouseButton в WidgetManager.render().
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bfb$hudClick(Click click, boolean doubled,
                               CallbackInfoReturnable<Boolean> cir) {
        HudIntegration hud = HudIntegration.get();
        if (hud == null || !hud.enabled()) return;
        if (hud.mouseButton(click.x(), click.y(), click.button(), 1)) {
            cir.setReturnValue(true);
        }
    }
}