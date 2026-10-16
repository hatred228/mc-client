package com.example.bfb.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Примитивы GUI. Всё рисуется через ctx.fill / HudDraw.round —
 * пиксель-перфект на любом guiScale, без запечённых текстур.
 */
public final class Draw {
    private Draw() {}

    // ============================================================
    // RECT
    // ============================================================

    public static void rect(DrawContext ctx, float x, float y, float w, float h, float r, int argb) {
        if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(w), ih = Math.round(h);
        int ir = Math.max(0, Math.round(r));
        if (ir <= 0) {
            ctx.fill(ix, iy, ix + iw, iy + ih, argb);
        } else {
            HudDraw.round(ctx, ix, iy, iw, ih, ir, argb);
        }
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int argb) {
        if (radius <= 0 || (argb >>> 24) == 0) return;
        int r = Math.round(radius);
        int icx = Math.round(cx), icy = Math.round(cy);
        for (int y = -r; y <= r; y++) {
            int dx = (int) Math.sqrt((double) r * r - (double) y * y);
            ctx.fill(icx - dx, icy + y, icx + dx + 1, icy + y + 1, argb);
        }
    }

    // ============================================================
    // BORDER
    // ============================================================

    public static void border(DrawContext ctx, float x, float y, float w, float h, float r,
                              float thickness, int argb) {
        if (w <= 0 || h <= 0 || thickness <= 0 || (argb >>> 24) == 0) return;
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(w), ih = Math.round(h);
        int it = Math.max(1, Math.round(thickness));
        int ir = Math.max(0, Math.round(r));

        ctx.fill(ix + ir, iy, ix + iw - ir, iy + it, argb);
        ctx.fill(ix + ir, iy + ih - it, ix + iw - ir, iy + ih, argb);
        ctx.fill(ix, iy + ir, ix + it, iy + ih - ir, argb);
        ctx.fill(ix + iw - it, iy + ir, ix + iw, iy + ih - ir, argb);

        if (ir > 0) {
            arc(ctx, ix + ir, iy + ir, ir, it, argb, 0);
            arc(ctx, ix + iw - ir, iy + ir, ir, it, argb, 1);
            arc(ctx, ix + iw - ir, iy + ih - ir, ir, it, argb, 2);
            arc(ctx, ix + ir, iy + ih - ir, ir, it, argb, 3);
        }
    }

    private static void arc(DrawContext ctx, int cx, int cy, int r, int t, int argb, int corner) {
        for (int py = 0; py < r; py++) {
            double dy = r - py - 0.5;
            if (dy < 0) continue;
            int dxO = (int) Math.sqrt(Math.max(0, (double) r * r - dy * dy));
            int innerR = r - t;
            int dxI = innerR <= 0 ? 0 : (int) Math.sqrt(Math.max(0, (double) innerR * innerR - dy * dy));
            if (dxO <= dxI) continue;
            int y = (corner == 0 || corner == 1) ? cy - r + py : cy + py;
            switch (corner) {
                case 0 -> ctx.fill(cx - dxO, y, cx - dxI, y + 1, argb);
                case 1 -> ctx.fill(cx + dxI, y, cx + dxO, y + 1, argb);
                case 2 -> ctx.fill(cx + dxI, y, cx + dxO, y + 1, argb);
                case 3 -> ctx.fill(cx - dxO, y, cx - dxI, y + 1, argb);
            }
        }
    }

    // ============================================================
    // SHADOW / GLOW — сглаженные, для них можно текстурой
    // ============================================================

    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r,
                              float spread, int argb) {
        int spreadI = Math.max(1, Math.round(spread));
        int baseA = (argb >>> 24) & 0xFF;
        int rgb = argb & 0xFFFFFF;
        for (int i = spreadI; i >= 1; i--) {
            int a = baseA / (i * 3 + 4);
            if (a <= 0) continue;
            float alpha = a / 255f;
            int col = (Math.round(alpha * 255) << 24) | rgb;
            HudDraw.round(ctx,
                    Math.round(x) - i, Math.round(y) - i,
                    Math.round(w) + i * 2, Math.round(h) + i * 2,
                    Math.round(r) + i, col);
        }
    }

    // ============================================================
    // GRADIENTS — в виде fill-полос, пиксель-перфект
    // ============================================================

    public static void vGradient(DrawContext ctx, float x, float y, float w, float h, float r,
                                 int top, int bottom) {
        int ih = Math.round(h);
        if (ih <= 0) return;
        for (int o = 0; o < ih; o++) {
            float t = ih <= 1 ? 0f : o / (float) (ih - 1);
            rect(ctx, x, y + o, w, 1f, o == 0 || o == ih - 1 ? r : 0f, lerp(top, bottom, t));
        }
    }

    public static void hGradient(DrawContext ctx, float x, float y, float w, float h, float r,
                                 int left, int right) {
        int iw = Math.round(w);
        if (iw <= 0) return;
        for (int o = 0; o < iw; o++) {
            float t = iw <= 1 ? 0f : o / (float) (iw - 1);
            rect(ctx, x + o, y, 1f, h, o == 0 || o == iw - 1 ? r : 0f, lerp(left, right, t));
        }
    }

    // ============================================================
    // ЦВЕТА
    // ============================================================

    public static int alpha(int argb, int a) {
        return (argb & 0xFFFFFF) | (Math.max(0, Math.min(255, a)) << 24);
    }

    public static int alpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (argb & 0xFFFFFF) | (a << 24);
    }

    public static int lerp(int a, int b, float t) {
        t = t < 0f ? 0f : t > 1f ? 1f : t;
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    public static int hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255f) << 16) | ((int) (g * 255f) << 8) | (int) (b * 255f);
    }

    public static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0f;
        if (d > 0.0001f) {
            if (max == r) h = ((g - b) / d) / 6f;
            else if (max == g) h = (2f + (b - r) / d) / 6f;
            else h = (4f + (r - g) / d) / 6f;
        }
        return new float[]{(h % 1f + 1f) % 1f, max <= 0f ? 0f : d / max, max};
    }

    // ============================================================
    // АНИМАЦИИ
    // ============================================================

    public static float approach(float cur, float target, float dt, float speed) {
        float t = 1f - (float) Math.exp(-speed * dt);
        return cur + (target - cur) * t;
    }

    public static float easeOutCubic(float t) {
        t = t < 0f ? 0f : t > 1f ? 1f : t;
        float f = 1f - t;
        return 1f - f * f * f;
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : v > max ? max : v;
    }
}