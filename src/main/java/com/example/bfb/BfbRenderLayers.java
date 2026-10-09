package com.example.bfb;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class BfbRenderLayers {

    public static final RenderLayer LINE_LAYER = RenderLayer.of("bfb_lines",
            RenderSetup.builder(BfbRenderPipelines.LINES_THROUGH).translucent().expectedBufferSize(1024).build());

    public static final RenderLayer QUAD_LAYER = RenderLayer.of("bfb_quads",
            RenderSetup.builder(BfbRenderPipelines.QUADS_THROUGH).translucent().expectedBufferSize(1024).build());

    // НОВЫЙ СЛОЙ ДЛЯ ГРАДИЕНТНОЙ ЗАЛИВКИ ESP
    public static final RenderLayer ESP_GRADIENT_LAYER = RenderLayer.of("bfb_esp_gradient",
            RenderSetup.builder(BfbRenderPipelines.ESP_FILLED_GRADIENT).translucent().expectedBufferSize(2048).build());

    public static final RenderLayer CHAMS_COLOR_LAYER = RenderLayer.of("bfb_chams_color",
            RenderSetup.builder(BfbRenderPipelines.CHAMS_COLOR).translucent().expectedBufferSize(4096).build());

    private static final Map<Identifier, RenderLayer> CHAM_TEXTURE_LAYERS = new HashMap<>();

    private BfbRenderLayers() {}

    public static RenderLayer chams(Identifier texture) {
        if (texture == null) return CHAMS_COLOR_LAYER;
        return CHAM_TEXTURE_LAYERS.computeIfAbsent(texture, id -> RenderLayer.of(
                "bfb_chams_" + Integer.toUnsignedString(id.hashCode()),
                RenderSetup.builder(BfbRenderPipelines.CHAMS_TEXTURED)
                        .texture("Sampler0", id)
                        .useOverlay().useLightmap().translucent().expectedBufferSize(2048).build()));
    }

    public static RenderLayer chamsColor() { return CHAMS_COLOR_LAYER; }

    public static void init() {}
}