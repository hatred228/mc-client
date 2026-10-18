package com.example.bfb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Кэш запечённых фигур со сглаживанием (SDF-based).
 *
 * Каждая фигура — белая текстура с alpha-покрытием, которую тонируем
 * нужным цветом при отрисовке. Один draw call на фигуру, края сглажены.
 */
public final class ShapeCache {
    private static final int MAX = 220;
    private static final Map<String, Identifier> CACHE = new LinkedHashMap<>();
    private static int counter;

    private ShapeCache() {}

    /** Заливка скруглённого прямоугольника со сглаженными краями. */
    public static Identifier rounded(int w, int h, float r) {
        if (w <= 0 || h <= 0) return null;
        r = clampRadius(w, h, r);
        return cached("f:" + w + ":" + h + ":" + fmt(r), w, h, r, 0f, 0f, Kind.FILL);
    }

    /** Контур скруглённого прямоугольника, толщина внутрь. */
    public static Identifier outline(int w, int h, float r, float thickness) {
        if (w <= 0 || h <= 0) return null;
        r = clampRadius(w, h, r);
        thickness = Math.max(0.5f, thickness);
        return cached("o:" + w + ":" + h + ":" + fmt(r) + ":" + fmt(thickness), w, h, r, thickness, 0f, Kind.OUTLINE);
    }

    /**
     * Мягкое свечение / тень вокруг скруглённого прямоугольника.
     * Текстура больше фигуры на spread с каждой стороны.
     */
    public static Identifier glow(int w, int h, float r, float spread) {
        if (w <= 0 || h <= 0) return null;
        spread = Math.max(1f, spread);
        int tw = w + (int) (spread * 2);
        int th = h + (int) (spread * 2);
        r = clampRadius(w, h, r);
        return cached("g:" + w + ":" + h + ":" + fmt(r) + ":" + fmt(spread), tw, th, r, 0f, spread, Kind.GLOW);
    }

    private enum Kind { FILL, OUTLINE, GLOW }

    private static Identifier cached(String key, int tw, int th, float r, float thickness, float spread, Kind kind) {
        Identifier id = CACHE.get(key);
        if (id != null) return id;
        try {
            id = build(tw, th, r, thickness, spread, kind);
        } catch (Throwable t) {
            return null;
        }
        CACHE.put(key, id);
        if (CACHE.size() > MAX) {
            var it = CACHE.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return id;
    }

    private static Identifier build(int tw, int th, float r, float thickness, float spread, Kind kind) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, tw, th, false);

        float rw = tw - spread * 2f;
        float rh = th - spread * 2f;
        float cx = spread + rw / 2f;
        float cy = spread + rh / 2f;
        float ex = Math.max(0f, rw / 2f - r);
        float ey = Math.max(0f, rh / 2f - r);

        for (int py = 0; py < th; py++) {
            float sy = py + 0.5f;
            for (int px = 0; px < tw; px++) {
                float sx = px + 0.5f;
                float dx = Math.max(Math.abs(sx - cx) - ex, 0f);
                float dy = Math.max(Math.abs(sy - cy) - ey, 0f);
                float d = (float) Math.sqrt(dx * dx + dy * dy) - r;

                float a;
                switch (kind) {
                    case FILL -> a = clamp01(0.5f - d);
                    case OUTLINE -> a = clamp01(0.5f - d) * clamp01(0.5f + d + thickness);
                    default -> {
                        float t = clamp01(1f - d / spread);
                        a = t * t * (3f - 2f * t);
                        a *= a;
                    }
                }
                if (a <= 0.003f) continue;
                int alpha = (int) (a * 255f + 0.5f);
                if (alpha > 255) alpha = 255;
                img.setColorArgb(px, py, (alpha << 24) | 0xFFFFFF);
            }
        }

        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bfb-shape", img);
        Identifier id = Identifier.of("undetected", "shape/s" + (counter++));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }

    private static float clampRadius(int w, int h, float r) {
        return Math.max(0f, Math.min(r, Math.min(w, h) / 2f));
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static String fmt(float v) {
        return Integer.toString((int) (v * 10f));
    }
}
