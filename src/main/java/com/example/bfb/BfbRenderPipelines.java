package com.example.bfb;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

public final class BfbRenderPipelines {

    // === ЛИНИИ СКВОЗЬ СТЕНЫ (для outline) ===
    public static final RenderPipeline LINES_THROUGH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of(BfbMod.MOD_ID, "pipeline/lines_through"))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    // === ЗАЛИВКА СКВОЗЬ СТЕНЫ (для Chams flat, ESP fill) ===
    public static final RenderPipeline QUADS_THROUGH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(BfbMod.MOD_ID, "pipeline/quads_through"))
                    .withCull(false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());

    // === ГРАДИЕНТНАЯ ЗАЛИВКА СКВОЗЬ СТЕНЫ (для ESP gradient fill) ===
    public static final RenderPipeline ESP_FILLED_GRADIENT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(BfbMod.MOD_ID, "pipeline/esp_filled_gradient"))
                    .withCull(false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());

    // === CHAMS МОДЕЛЬ (сквозь стены, текстурированная) ===
    public static final RenderPipeline CHAMS_TEXTURED = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.of(BfbMod.MOD_ID, "pipeline/chams_textured"))
                    .withCull(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    // === CHAMS ЦВЕТНАЯ ЗАЛИВКА ===
    public static final RenderPipeline CHAMS_COLOR = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(BfbMod.MOD_ID, "pipeline/chams_color"))
                    .withCull(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    private BfbRenderPipelines() {}

    public static void init() {}
}