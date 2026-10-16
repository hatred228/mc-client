package com.example.bfb.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Скруглённые прямоугольники для HUD.
 * Построчный рендер углов через fill — без drawTexture и NativeImage.
 * Предвычисленный DX_CACHE для радиусов 1..16 — экономит sqrt() в hot path.
 */
public final class HudDraw {

    private static final int MAX_CACHED_R = 16;
    private static final int[][] DX_CACHE = new int[MAX_CACHED_R + 1][];

    static {
        for (int r = 1; r <= MAX_CACHED_R; r++) {
            int[] dx = new int[r];
            for (int py = 0; py < r; py++) {
                double dy = r - py - 0.5;
                double sq = (double) r * r - dy * dy;
                dx[py] = sq < 0 ? -1 : (int) Math.sqrt(sq);
            }
            DX_CACHE[r] = dx;
        }
    }

    private HudDraw() {}

    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        if (r > Math.min(w, h) / 2) r = Math.min(w, h) / 2;

        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        int[] dxs = dxs(r);
        for (int py = 0; py < r; py++) {
            int dx = dxs[py];
            if (dx <= 0) continue;
            ctx.fill(x + r - dx, y + py, x + r, y + py + 1, color);
            ctx.fill(x + w - r, y + py, x + w - r + dx, y + py + 1, color);
            ctx.fill(x + r - dx, y + h - py - 1, x + r, y + h - py, color);
            ctx.fill(x + w - r, y + h - py - 1, x + w - r + dx, y + h - py, color);
        }
    }

    public static void roundGradient(DrawContext ctx, int x, int y, int w, int h, int r,
                                     int topColor, int bottomColor) {
        if (w <= 0 || h <= 0) return;
        if (r <= 0) { ctx.fillGradient(x, y, x + w, y + h, topColor, bottomColor); return; }
        if (r > Math.min(w, h) / 2) r = Math.min(w, h) / 2;

        ctx.fillGradient(x + r, y, x + w - r, y + h, topColor, bottomColor);
        ctx.fillGradient(x, y + r, x + r, y + h - r, topColor, bottomColor);
        ctx.fillGradient(x + w - r, y + r, x + w, y + h - r, topColor, bottomColor);

        int[] dxs = dxs(r);
        int totalH = Math.max(1, h);
        for (int py = 0; py < r; py++) {
            int dx = dxs[py];
            if (dx <= 0) continue;

            int cTop = lerpColor(topColor, bottomColor, (float) py / totalH);
            int cBot = lerpColor(topColor, bottomColor, (float) (h - py - 1) / totalH);

            ctx.fill(x + r - dx, y + py, x + r, y + py + 1, cTop);
            ctx.fill(x + w - r, y + py, x + w - r + dx, y + py + 1, cTop);
            ctx.fill(x + r - dx, y + h - py - 1, x + r, y + h - py, cBot);
            ctx.fill(x + w - r, y + h - py - 1, x + w - r + dx, y + h - py, cBot);
        }
    }

    private static int[] dxs(int r) {
        if (r <= MAX_CACHED_R) return DX_CACHE[r];
        int[] dxs = new int[r];
        for (int py = 0; py < r; py++) {
            double dy = r - py - 0.5;
            double sq = (double) r * r - dy * dy;
            dxs[py] = sq < 0 ? -1 : (int) Math.sqrt(sq);
        }
        return dxs;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (((int) (aa + (ba - aa) * t)) << 24)
             | (((int) (ar + (br - ar) * t)) << 16)
             | (((int) (ag + (bg - ag) * t)) << 8)
             | ((int) (ab + (bb - ab) * t));
    }
}