package com.example.bfb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Запекает "дуги" круга: набор текстур, где индекс 0 = пусто, индекс N = полный круг.
 * Используется для кругового прогресса Cooldowns, CrossbowCharge и т.д.
 */
public final class RingTextureCache {
    public static final int ARC_STEPS = 48;
    private static final Map<Long, Identifier[]> CACHE = new HashMap<>();

    private RingTextureCache() {}

    public static Identifier[] getArcs(int size, int thickness) {
        if (size <= 4) size = 5;
        if (thickness < 1) thickness = 1;
        if (thickness > size / 2) thickness = size / 2;
        long key = ((long) size << 16) | thickness;
        Identifier[] arr = CACHE.get(key);
        if (arr != null) return arr;

        arr = new Identifier[ARC_STEPS + 1];
        for (int i = 0; i <= ARC_STEPS; i++) {
            arr[i] = bake(size, thickness, i);
        }
        CACHE.put(key, arr);
        return arr;
    }

    private static Identifier bake(int size, int thickness, int arcIndex) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        double cx = size / 2.0;
        double cy = size / 2.0;
        double outerR = size / 2.0 - 0.5;
        double innerR = outerR - thickness;
        double startAngle = -Math.PI / 2; // начинаем сверху
        double endRel = 2 * Math.PI * arcIndex / ARC_STEPS;

        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double dx = px + 0.5 - cx;
                double dy = py + 0.5 - cy;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r < innerR - 1 || r > outerR + 1) continue;

                // проверяем угол
                if (arcIndex < ARC_STEPS) {
                    double angle = Math.atan2(dy, dx);
                    double rel = angle - startAngle;
                    while (rel < 0) rel += 2 * Math.PI;
                    while (rel >= 2 * Math.PI) rel -= 2 * Math.PI;
                    if (rel > endRel) continue;
                }

                // anti-alias по внешнему и внутреннему краю
                double alpha = 1.0;
                if (r > outerR) alpha = Math.min(alpha, 1.0 - (r - outerR));
                if (r < innerR) alpha = Math.min(alpha, 1.0 - (innerR - r));
                // anti-alias по торцу дуги (плавный кончик)
                if (arcIndex > 0 && arcIndex < ARC_STEPS) {
                    double angle = Math.atan2(dy, dx);
                    double rel = angle - startAngle;
                    while (rel < 0) rel += 2 * Math.PI;
                    while (rel >= 2 * Math.PI) rel -= 2 * Math.PI;
                    double edgeDist = endRel - rel;
                    if (edgeDist < 0.15) alpha = Math.min(alpha, edgeDist / 0.15);
                    if (rel < 0.05) alpha = Math.min(alpha, rel / 0.05);
                }
                alpha = Math.max(0, Math.min(1, alpha));
                int a = (int) (255 * alpha);
                if (a <= 0) continue;

                img.setColorArgb(px, py, (a << 24) | 0xFFFFFF);
            }
        }

        Identifier id = Identifier.of("undetected",
                "ring/" + size + "_" + thickness + "_" + arcIndex);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bfb-ring", img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }
}