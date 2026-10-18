package com.example.bfb.render.nvg;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

/**
 * Обёртка над NanoVG API.
 *
 * Все методы требуют активного frame (beginFrame/endFrame).
 * Все цвета — int ARGB.
 */
public final class NvgRenderer {

    private static final NVGColor COLOR = NVGColor.calloc();

    private NvgRenderer() {}

    // ================================
    // FRAME
    // ================================

    public static void beginFrame(int screenW, int screenH, float pixelRatio) {
        if (!NvgContext.ready()) return;
        NanoVG.nvgBeginFrame(NvgContext.handle(), screenW, screenH, pixelRatio);
    }

    public static void endFrame() {
        if (!NvgContext.ready()) return;
        NanoVG.nvgEndFrame(NvgContext.handle());
    }

    // ================================
    // SHAPES
    // ================================

    public static void fillRect(float x, float y, float w, float h, int argb) {
        if (!NvgContext.ready()) return;
        long vg = NvgContext.handle();
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x, y, w, h);
        fill(vg, argb);
    }

    public static void fillRoundedRect(float x, float y, float w, float h, float radius, int argb) {
        if (!NvgContext.ready()) return;
        long vg = NvgContext.handle();
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
        fill(vg, argb);
    }

    public static void fillCircle(float cx, float cy, float radius, int argb) {
        if (!NvgContext.ready()) return;
        long vg = NvgContext.handle();
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, cx, cy, radius);
        fill(vg, argb);
    }

    public static void strokeRoundedRect(float x, float y, float w, float h, float radius,
                                          float thickness, int argb) {
        if (!NvgContext.ready()) return;
        long vg = NvgContext.handle();
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
        NanoVG.nvgStrokeWidth(vg, thickness);
        stroke(vg, argb);
    }

    private static void fill(long vg, int argb) {
        applyColor(argb);
        NanoVG.nvgFillColor(vg, COLOR);
        NanoVG.nvgFill(vg);
    }

    private static void stroke(long vg, int argb) {
        applyColor(argb);
        NanoVG.nvgStrokeColor(vg, COLOR);
        NanoVG.nvgStroke(vg);
    }

    // ================================
    // TEXT
    // ================================

    public static void text(String font, float size, float x, float y, int argb, String value) {
        if (!NvgContext.ready() || value == null || value.isEmpty()) return;
        long vg = NvgContext.handle();
        NanoVG.nvgFontFace(vg, font);
        NanoVG.nvgFontSize(vg, size);
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        applyColor(argb);
        NanoVG.nvgFillColor(vg, COLOR);
        NanoVG.nvgText(vg, x, y, value);
    }

    // ================================
    // HELPERS
    // ================================

    private static void applyColor(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        COLOR.r(r).g(g).b(b).a(a);
    }
}