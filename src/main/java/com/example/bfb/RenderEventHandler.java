package com.example.bfb;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public final class RenderEventHandler {
    private RenderEventHandler() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (PanicManager.hideVisuals()) return;
            MatrixStack matrices = context.matrices();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices == null || consumers == null) return;
            var camera = client.gameRenderer.getCamera().getCameraPos();
            for (CheatModule module : ModuleManager.getModules()) {
                if (module.isEnabled()) module.onWorldRender(matrices, consumers, camera);
            }
        });
    }
}