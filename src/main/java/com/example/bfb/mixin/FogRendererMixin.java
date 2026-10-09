package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoRenderModule;
import com.example.bfb.modules.WorldVisualsModule;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @ModifyVariable(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float bfb$fogStart(float value) {
        return start(value);
    }

    @ModifyVariable(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float bfb$fogEnd(float value) {
        return end(value);
    }

    @ModifyVariable(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float bfb$renderStart(float value) {
        return start(value);
    }

    @ModifyVariable(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float bfb$renderEnd(float value) {
        return end(value);
    }

    @ModifyVariable(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private float bfb$skyEnd(float value) {
        return end(value);
    }

    @ModifyVariable(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private float bfb$cloudEnd(float value) {
        return end(value);
    }

    private static float start(float value) {
        if (noFog()) return 1.0e7f;
        WorldVisualsModule module = module();
        if (module == null) return value;
        if (value <= 16f) return value;
        if (module.hideFog()) return 1.0e7f;
        if (module.heavyFog()) return Math.min(value, 8f);
        return value;
    }

    private static float end(float value) {
        if (noFog()) return 1.0e7f;
        WorldVisualsModule module = module();
        if (module == null || value <= 16f) return value;
        if (module.hideFog()) return 1.0e7f;
        if (module.heavyFog()) return Math.min(value, 70f);
        return value;
    }

    private static boolean noFog() {
        NoRenderModule module = ModuleManager.get(NoRenderModule.class);
        return module != null && module.isEnabled() && module.fog.get();
    }

    private static WorldVisualsModule module() {
        WorldVisualsModule module = ModuleManager.get(WorldVisualsModule.class);
        return module != null && module.isEnabled() ? module : null;
    }
}
