package com.example.bfb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Запечённые круги: solid, radialFade, outline. */
public final class CircleTextureCache {
    private static final Map<Long, Identifier> CACHE = new HashMap<>();

    private CircleTextureCache() {}

    /** Залитый круг белого цвета — окрашивать через tint. */
    public static Identifier solid(int d) {
        return get("solid", d, 0, 0);
    }

    /** Радиальный градиент — alpha 255 в центре, 0 у края. RGB белый. */
    public static Identifier radialFade(int d) {
        return get("fade", d, 0, 0);
    }

    /** Кольцо-контур толщиной t пикселей. */
    public static Identifier outline(int d, int t) {
        if (t < 1) t = 1;
        return get("ring", d, t, 0);
    }

    private static Identifier get(String type, int size, int param, int unused) {
        if (size <= 2) size = 3;
        long key = ((long) size << 24) | ((long) param << 8) | type.hashCode() & 0xFF;
        Identifier id = CACHE.get(key);
        if (id != null) return id;
        id = bake(type, size, param);
        CACHE.put(key, id);
        return id;
    }

    private static Identifier bake(String type, int size, int param) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        double cx = size / 2.0;
        double cy = size / 2.0;
        double outerR = size / 2.0 - 0.5;

        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double dx = px + 0.5 - cx;
                double dy = py + 0.5 - cy;
                double r = Math.sqrt(dx * dx + dy * dy);

                int argb = 0;

                switch (type) {
                    case "solid" -> {
                        if (r > outerR + 1) continue;
                        double a = 1.0;
                        if (r > outerR) a = 1.0 - (r - outerR);
                        a = Math.max(0, Math.min(1, a));
                        argb = ((int) (255 * a) << 24) | 0xFFFFFF;
                    }
                    case "fade" -> {
                        if (r > outerR + 1) continue;
                        double t = Math.max(0, Math.min(1, r / outerR));
                        // smooth falloff
                        double a = 1.0 - t * t;
                        a = Math.max(0, Math.min(1, a));
                        argb = ((int) (255 * a) << 24) | 0xFFFFFF;
                    }
                    case "ring" -> {
                        double innerR = outerR - param;
                        if (r > outerR + 1 || r < innerR - 1) continue;
                        double a = 1.0;
                        if (r > outerR) a = Math.min(a, 1.0 - (r - outerR));
                        if (r < innerR) a = Math.min(a, 1.0 - (innerR - r));
                        a = Math.max(0, Math.min(1, a));
                        argb = ((int) (255 * a) << 24) | 0xFFFFFF;
                    }
                }

                if ((argb >>> 24) > 0) img.setColorArgb(px, py, argb);
            }
        }

        Identifier id = Identifier.of("undetected",
                "circle/" + type + "_" + size + "_" + param);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bfb-circle", img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }
}