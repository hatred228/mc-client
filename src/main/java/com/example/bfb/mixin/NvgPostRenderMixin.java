package com.example.bfb.mixin;

import com.example.bfb.render.nvg.NvgPostRender;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class NvgPostRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void bfb$nvgPostRender(boolean tick, CallbackInfo ci) {
        try {
            NvgPostRender.render((MinecraftClient) (Object) this);
        } catch (Throwable t) {
            System.err.println("[BFB] NvgPostRender failed: " + t.getMessage());
            t.printStackTrace();
        }
    }
}