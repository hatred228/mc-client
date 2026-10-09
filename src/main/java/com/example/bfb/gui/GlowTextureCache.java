package com.example.bfb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Радиальное свечение — белый центр, плавно в 0 к краю. Для AmbientGlow и др. */
public final class GlowTextureCache {
    private static final Map<Integer, Identifier> CACHE = new HashMap<>();

    private GlowTextureCache() {}

    public static Identifier get(int diameter) {
        if (diameter <= 4) diameter = 5;
        Identifier id = CACHE.get(diameter);
        if (id != null) return id;
        id = bake(diameter);
        CACHE.put(diameter, id);
        return id;
    }

    private static Identifier bake(int size) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        double cx = size / 2.0;
        double cy = size / 2.0;
        double r = size / 2.0;

        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double dx = px + 0.5 - cx;
                double dy = py + 0.5 - cy;
                double d = Math.sqrt(dx * dx + dy * dy) / r;
                if (d > 1) continue;
                // квадратичное затухание: центр 1.0, край 0
                double a = 1.0 - d * d;
                a = a * a; // ещё резче
                int ai = (int) (255 * Math.max(0, Math.min(1, a)));
                if (ai <= 0) continue;
                img.setColorArgb(px, py, (ai << 24) | 0xFFFFFF);
            }
        }

        Identifier id = Identifier.of("libbase", "glow/" + size);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bfb-glow", img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }
}