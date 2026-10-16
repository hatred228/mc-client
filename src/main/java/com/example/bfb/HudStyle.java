package com.example.bfb;

import com.example.bfb.gui.HudDraw;
import net.minecraft.client.gui.DrawContext;

public final class HudStyle {
    private HudStyle() {}

    // ============================================================
    // PREMIUM PANEL
    // ============================================================

    public static void panel(DrawContext ctx, int x, int y, int w, int h,
                             int radius, int edgeW,
                             int edgeC1, int edgeC2, int bgColor, boolean shadow) {
        if (w <= 0 || h <= 0) return;
        if (radius < 0) radius = 0;
        if (radius > Math.min(w, h) / 2) radius = Math.min(w, h) / 2;

        // === 1. Мягкая тень ===
        if (shadow) {
            for (int i = 4; i >= 1; i--) {
                int a = 8 + i * 4;
                int pad = i;
                int rr = radius + i;
                HudDraw.round(ctx, x - pad, y - pad + 1, w + pad * 2, h + pad * 2,
                        rr, (a << 24));
            }
        }

        // === 2. Фон — градиентное скруглённое ===
        int bgA = (bgColor >>> 24);
        int bgRgb = bgColor & 0xFFFFFF;
        int topColor = (bgA << 24) | bgRgb;
        int botColor = ((int) (bgA * 0.7f) << 24) | darken(bgRgb, 0.55f);
        HudDraw.roundGradient(ctx, x, y, w, h, radius, topColor, botColor);

        // === 3. Внутренний highlight сверху ===
        if (w > radius * 2 + 4) {
            ctx.fill(x + radius, y + 1, x + w - radius, y + 2, 0x45FFFFFF);
        }

        // === 4. Внутренний shadow снизу ===
        if (w > radius * 2 + 4) {
            ctx.fill(x + radius, y + h - 2, x + w - radius, y + h - 1, 0x50000000);
        }

        // === 5. Accent кромка ===
        if (edgeW > 0) {
            if (w > radius * 2 + 2) {
                ctx.fillGradient(x + radius, y, x + w - radius, y + edgeW,
                        0xFF000000 | edgeC1, 0xFF000000 | edgeC2);
                ctx.fillGradient(x + radius, y + h - edgeW, x + w - radius, y + h,
                        0xFF000000 | edgeC2, 0xFF000000 | edgeC1);
            }
        }
    }

    public static void panelGlow(DrawContext ctx, int x, int y, int w, int h,
                                 int radius, int edgeW,
                                 int edgeC1, int edgeC2, int bgColor,
                                 boolean shadow, int glowColor, int glowStrength) {
        if (glowStrength > 0) {
            glowBorder(ctx, x, y, w, h, radius, glowColor, glowStrength);
        }
        panel(ctx, x, y, w, h, radius, edgeW, edgeC1, edgeC2, bgColor, shadow);
    }

    // ============================================================
    // PILL (watermark, toast)
    // ============================================================

    public static void pill(DrawContext ctx, int x, int y, int w, int h,
                            int bgColor, int glowColor, int glowStrength) {
        if (w <= 0 || h <= 0) return;
        int radius = h / 2;

        if (glowStrength > 0) {
            glowBorder(ctx, x, y, w, h, radius, glowColor, glowStrength);
        }

        int bgA = (bgColor >>> 24);
        int bgRgb = bgColor & 0xFFFFFF;
        HudDraw.round(ctx, x, y, w, h, radius, (bgA << 24) | bgRgb);

        if (w > radius * 2 + 4) {
            ctx.fill(x + radius, y + 1, x + w - radius, y + 2, 0x55FFFFFF);
            ctx.fill(x + radius, y + h - 2, x + w - radius, y + h - 1, 0x50000000);
        }
    }

    // ============================================================
    // HEADER
    // ============================================================

    public static void header(DrawContext ctx, int x, int y, int w, int headerH,
                              int c1, int c2, String title, int textColor) {
        if (w <= 0 || headerH <= 0) return;

        ctx.fillGradient(x, y, x + w, y + headerH, 0xFF000000 | c1, 0xFF000000 | c2);
        ctx.fill(x + 1, y, x + w - 1, y + 1, 0x45FFFFFF);
        ctx.fill(x, y + headerH - 1, x + w, y + headerH, 0x80000000);

        int tw = SalFont.width(title);
        int textY = y + (headerH - SalFont.height()) / 2;
        SalFont.draw(ctx, title, x + (w - tw) / 2 + 1, textY + 1, 0x40000000);
        SalFont.draw(ctx, title, x + (w - tw) / 2, textY,
                0xFF000000 | (textColor & 0xFFFFFF));
    }

    // ============================================================
    // GLOW BORDER
    // ============================================================

    public static void glowBorder(DrawContext ctx, int x, int y, int w, int h, int r,
                                  int glowColor, int strength) {
        if (strength <= 0) return;
        int g = glowColor & 0xFFFFFF;
        for (int i = 5; i >= 1; i--) {
            int a = strength / (i * 3 + 4);
            if (a <= 0) continue;
            int pad = i;
            HudDraw.round(ctx, x - pad, y - pad, w + pad * 2, h + pad * 2,
                    r + pad, (a << 24) | g);
        }
    }

    // ============================================================
    // CIRCLES
    // ============================================================

    public static void circle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int dx = (int) Math.sqrt((double) r * r - (double) y * y);
            ctx.fill(cx - dx, cy + y, cx + dx + 1, cy + y + 1, color);
        }
    }

    public static void circleGradient(DrawContext ctx, int cx, int cy, int r, int inner, int outer) {
        int layers = 6;
        for (int i = layers - 1; i >= 0; i--) {
            int rr = r * (i + 1) / layers;
            float t = i / (float) (layers - 1);
            int c = lerpColor(outer, inner, t);
            circle(ctx, cx, cy, rr, (0xFF << 24) | c);
        }
    }

    public static void gradientRing(DrawContext ctx, int cx, int cy, int r, int thickness,
                                    int c1, int c2, int segments) {
        int half = Math.max(1, thickness / 2);
        for (int layer = -half; layer <= half; layer++) {
            int rr = r + layer;
            if (rr < 1) continue;
            int steps = Math.max(64, segments * 2);
            for (int i = 0; i < steps; i++) {
                double a = Math.PI * 2 * i / steps;
                float t = i / (float) steps;
                int c = lerpColor(c1, c2, t);
                int px = cx + (int) Math.round(Math.cos(a) * rr);
                int py = cy + (int) Math.round(Math.sin(a) * rr);
                ctx.fill(px, py, px + 1, py + 1, 0xFF000000 | c);
            }
        }
    }

    public static void splitRing(DrawContext ctx, int cx, int cy, int r, int thickness,
                                 int c1, int c2, int segments) {
        int half = Math.max(1, thickness / 2);
        for (int layer = -half; layer <= half; layer++) {
            int rr = r + layer;
            if (rr < 1) continue;
            for (int i = 0; i < segments; i++) {
                double a1 = Math.PI * 2 * i / segments;
                int px = cx + (int) Math.round(Math.cos(a1) * rr);
                int py = cy + (int) Math.round(Math.sin(a1) * rr);
                int c = py < cy ? c1 : c2;
                ctx.fill(px, py, px + 1, py + 1, 0xFF000000 | c);
            }
        }
    }

    // ============================================================
    // ЦВЕТА
    // ============================================================

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (((int) (ar + (br - ar) * t)) << 16)
             | (((int) (ag + (bg - ag) * t)) << 8)
             | ((int) (ab + (bb - ab) * t));
    }

    public static int darken(int rgb, float factor) {
        return (((int) (((rgb >> 16) & 0xFF) * factor)) << 16)
             | (((int) (((rgb >> 8) & 0xFF) * factor)) << 8)
             | ((int) ((rgb & 0xFF) * factor));
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}