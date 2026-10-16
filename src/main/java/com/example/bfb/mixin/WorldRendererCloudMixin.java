package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.SkyBoxModule;
import com.example.bfb.modules.WorldVisualsModule;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRendererCloudMixin {

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getCloudRenderModeValue()Lnet/minecraft/client/option/CloudRenderMode;"),
            require = 0)
    private CloudRenderMode bfb$hideClouds(GameOptions options) {
        CloudRenderMode original = options.getCloudRenderModeValue();

        // 1. SkyBox включён и его "Hide clouds" активен → убираем облака
        SkyBoxModule sky = ModuleManager.get(SkyBoxModule.class);
        if (sky != null && sky.isEnabled() && sky.hideClouds.get()) {
            return CloudRenderMode.OFF;
        }

        // 2. WorldVisuals "Hide clouds" → убираем
        WorldVisualsModule wv = ModuleManager.get(WorldVisualsModule.class);
        if (wv != null && wv.isEnabled() && wv.hideClouds.get()) {
            return CloudRenderMode.OFF;
        }

        return original;
    }
}