package com.example.bfb;

import net.minecraft.client.gui.DrawContext;

public final class HudStyle {
    private HudStyle() {}

    public static void panel(DrawContext ctx, int x, int y, int w, int h,
                             int radius, int edgeW,
                             int edgeC1, int edgeC2, int bgColor, boolean shadow) {
        // фон градиент
        int bgTop = bgColor & 0xFFFFFF;
        int bgBot = darken(bgTop, 0.5f);
        int bgA = (bgColor >>> 24);
        ctx.fillGradient(x, y, x + w, y + h, (bgA << 24) | bgTop, (bgA << 24) | bgBot);
        // края
        if (edgeW <= 0) return;
        ctx.fillGradient(x, y, x + w, y + edgeW, 0xFF000000 | edgeC1, 0xFF000000 | edgeC2);
        ctx.fillGradient(x, y + h - edgeW, x + w, y + h, 0xFF000000 | edgeC2, 0xFF000000 | edgeC1);
        ctx.fill(x, y + edgeW, x + edgeW, y + h - edgeW, 0xFF000000 | edgeC1);
        ctx.fill(x + w - edgeW, y + edgeW, x + w, y + h - edgeW, 0xFF000000 | edgeC2);
    }

    public static void header(DrawContext ctx, int x, int y, int w, int headerH,
                              int c1, int c2, String title, int textColor) {
        ctx.fillGradient(x, y, x + w, y + headerH, 0xFF000000 | c1, 0xFF000000 | c2);
        int tw = SalFont.width(title);
        SalFont.draw(ctx, title, x + (w - tw) / 2, y + (headerH - SalFont.height()) / 2,
                0xFF000000 | (textColor & 0xFFFFFF));
    }

    /** Плоский залитый круг. */
    public static void circle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int dx = (int) Math.sqrt((double) r * r - (double) y * y);
            ctx.fill(cx - dx, cy + y, cx + dx + 1, cy + y + 1, color);
        }
    }

    /** Круг с радиальным градиентом — 6 слоёв, дёшево и красиво. */
    public static void circleGradient(DrawContext ctx, int cx, int cy, int r, int inner, int outer) {
        int layers = 6;
        for (int i = layers - 1; i >= 0; i--) {
            int rr = r * (i + 1) / layers;
            float t = i / (float) (layers - 1);
            int c = lerpColor(outer, inner, t);
            int a = 0xFF;
            circle(ctx, cx, cy, rr, (a << 24) | c);
        }
    }

        /** Гладкое градиентное кольцо — концентрические окружности. */
    public static void gradientRing(DrawContext ctx, int cx, int cy, int r, int thickness,
                                    int c1, int c2, int segments) {
        int half = Math.max(1, thickness / 2);
        // 3 концентрических кольца — каждое толщиной 1px
        for (int layer = -half; layer <= half; layer++) {
            int rr = r + layer;
            if (rr < 1) continue;
            // для каждого пикселя на окружности рисуем точку 2x2 чтобы не было дырок
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

    public static int lerpColor(int a, int b, float t) {
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
}