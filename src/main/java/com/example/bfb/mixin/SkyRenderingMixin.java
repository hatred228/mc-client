package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.SkyBoxModule;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class SkyRenderingMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void bfb$customSky(net.minecraft.client.util.ObjectAllocator allocator,
                                RenderTickCounter tickCounter,
                                boolean renderBlockOutline,
                                Camera camera,
                                Matrix4f positionMatrix,
                                Matrix4f basicProjectionMatrix,
                                Matrix4f projectionMatrix,
                                GpuBufferSlice fogBuffer,
                                Vector4f fogColor,
                                boolean renderSky,
                                CallbackInfo ci) {
        SkyBoxModule mod = ModuleManager.get(SkyBoxModule.class);
        if (mod == null || !mod.isEnabled()) return;
        if (!renderSky) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float tickDelta = tickCounter.getTickProgress(false);

        // === РАБОЧИЙ порядок: positionMatrix → projectionMatrix ===
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        matrices.multiplyPositionMatrix(projectionMatrix);

        VertexConsumerProvider.Immediate consumers =
                client.getBufferBuilders().getEntityVertexConsumers();

        SkyBoxModule.renderSky(matrices, consumers, tickDelta);

        consumers.draw();
    }
}