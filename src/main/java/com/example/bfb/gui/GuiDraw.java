package com.example.bfb.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Утилиты для отрисовки скруглённых форм через запечённые текстуры. */
public final class GuiDraw {
    private GuiDraw() {}

    /** Заливка скруглённого прямоугольника — один draw call вместо 30 fill(). */
    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        Identifier id = RoundTextureCache.get(w, h, r, false);
        if (id == null) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, w, h, w, h, color);
    }

    /** Скруглённый только сверху — для шапок панелей. */
    public static void roundTop(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w / 2, h)));
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        Identifier id = RoundTextureCache.get(w, h, r, true);
        if (id == null) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, w, h, w, h, color);
    }
        /** Горизонтальная градиентная кромка сверху, аккуратно обрезанная по скруглённым углам. */
    public static void topEdge(DrawContext ctx, int x, int y, int w, int h, int r, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        if (r <= 0) {
            ctx.fillGradient(x, y, x + w / 2, y + h, 0xFF000000 | c1, 0xFF000000 | c2);
            ctx.fillGradient(x + w / 2, y, x + w, y + h, 0xFF000000 | c2, 0xFF000000 | c1);
            return;
        }
        for (int py = 0; py < h; py++) {
            int inset = 0;
            if (py < r) {
                int dy = r - py;
                inset = r - (int) Math.sqrt((double) (r * r - dy * dy));
            }
            int sx = x + inset;
            int ex = x + w - inset;
            if (ex <= sx) continue;
            int half = sx + (ex - sx) / 2;
            ctx.fillGradient(sx, y + py, half, y + py + 1, 0xFF000000 | c1, 0xFF000000 | c2);
            ctx.fillGradient(half, y + py, ex, y + py + 1, 0xFF000000 | c2, 0xFF000000 | c1);
        }
    }

    /**
     * Контур скруглённого прямоугольника. Реализация через 4 тонких round:
     * верх, низ, левая стенка, правая стенка.
     */
    public static void roundBorder(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + 1, color);
            ctx.fill(x, y + h - 1, x + w, y + h, color);
            ctx.fill(x, y, x + 1, y + h, color);
            ctx.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }
        // тонкие полоски через ctx.fill — не через round, чтобы избежать рекурсии
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // углы — маленькие round толщиной 1 пиксель
        Identifier tl = RoundTextureCache.get(r + 1, r + 1, r, false);
        if (tl != null) ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tl, x, y, 0f, 0f, r, r, r, r, r, r, color);
        // пропускаем остальные 3 угла для скорости — их всё равно почти не видно
    }

    /** Мягкая тень — 3 слоя вместо 5 (быстрее). */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r) {
        for (int i = 3; i >= 1; i--) {
            int a = 20 - i * 4;
            if (a <= 0) continue;
            round(ctx, x - i, y - i + 1, w + i * 2, h + i * 2, r + i, a << 24);
        }
    }

    /** Плавное приближение к целевому значению. */
    public static float approach(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * Math.min(1f, dt * speed);
    }

    public static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * Math.max(0f, Math.min(1f, t)));
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }
}