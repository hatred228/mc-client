package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.WorldVisualsModule;
import net.minecraft.client.render.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int bfb$cloudColor(int color) {
        WorldVisualsModule module = ModuleManager.get(WorldVisualsModule.class);
        if (module == null || !module.isEnabled() || !module.colorClouds.get()) return color;
        return module.clouds.argb(0xFF);
    }
}
