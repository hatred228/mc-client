package com.example.bfb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Кэш запечённых текстур скруглённых прямоугольников.
 * Рисует один draw call вместо 30 fill() — критично для FPS.
 */
public final class RoundTextureCache {
    private static final Map<Long, Identifier> CACHE = new HashMap<>();
    private static final List<Long> ORDER = new ArrayList<>();
    private static final int MAX = 96;

    private RoundTextureCache() {}

    public static Identifier get(int w, int h, int r, boolean topOnly) {
        if (w <= 0 || h <= 0) return null;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        long key = ((long) w << 40) | ((long) h << 20) | ((long) r << 1) | (topOnly ? 1L : 0L);
        Identifier id = CACHE.get(key);
        if (id != null) return id;
        id = build(w, h, r, topOnly);
        CACHE.put(key, id);
        ORDER.add(key);
        if (ORDER.size() > MAX) {
            long old = ORDER.remove(0);
            CACHE.remove(old);
            // текстуры оставляем в памяти — destroy с риском падения на разных билдах
            // 96 текстур по 128x64 RGBA = ~3 МБ VRAM, не критично
        }
        return id;
    }

    private static Identifier build(int w, int h, int r, boolean topOnly) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        int white = 0xFFFFFFFF;
        int r2 = r * r;

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                boolean inside;
                if (px >= r && px < w - r) {
                    inside = true;
                } else if (py >= r && py < h - r) {
                    inside = true;
                } else {
                    int cx = px < r ? r : w - r - 1;
                    int cy = py < r ? r : h - r - 1;
                    boolean bottom = py >= h - r;
                    if (topOnly && bottom) {
                        inside = true; // нижние углы не скругляем при topOnly
                    } else {
                        int dx = px - cx;
                        int dy = py - cy;
                        inside = dx * dx + dy * dy <= r2;
                    }
                }
                if (inside) img.setColorArgb(px, py, white);
            }
        }

        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bfb-round", img);
        Identifier id = Identifier.of("libbase", "round/"
                + Integer.toHexString(w) + "_"
                + Integer.toHexString(h) + "_"
                + Integer.toHexString(r) + "_"
                + (topOnly ? "t" : "a"));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }
}