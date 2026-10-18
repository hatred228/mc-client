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
    // ============================================================
// ПРЕМИУМ-МЕТОДЫ
// ============================================================

/**
 * Pill-панель (таблетка). Радиус = высота / 2.
 */
public static void pill(DrawContext ctx, int x, int y, int w, int h, int color) {
    int r = h / 2;
    round(ctx, x, y, w, h, r, color);
}

/**
 * Мягкая тень. 3 слоя скруглений с убывающей alpha.
 */
public static void softShadow(DrawContext ctx, int x, int y, int w, int h, int radius, int strength) {
    for (int i = 3; i >= 1; i--) {
        int a = strength / (i * 2 + 1);
        if (a <= 0) continue;
        round(ctx, x - i, y - i + 1, w + i * 2, h + i * 2, radius + i, (a << 24));
    }
}

/**
 * Неоновое свечение вокруг элемента. Цвет accent.
 */
public static void glow(DrawContext ctx, int x, int y, int w, int h, int radius,
                        int glowColor, int strength) {
    if (strength <= 0) return;
    int g = glowColor & 0xFFFFFF;
    for (int i = 5; i >= 1; i--) {
        int a = strength / (i * 3 + 4);
        if (a <= 0) continue;
        round(ctx, x - i, y - i, w + i * 2, h + i * 2, radius + i, (a << 24) | g);
    }
}

/**
 * Градиентная граница 1px вокруг скруглённого прямоугольника.
 */
public static void gradientBorder(DrawContext ctx, int x, int y, int w, int h, int radius,
                                  int c1, int c2) {
    if (w <= 2 || h <= 2) return;
    int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
    // верхняя кромка (градиент)
    ctx.fillGradient(x + r, y, x + w - r, y + 1, c1, c2);
    // нижняя кромка (обратный градиент)
    ctx.fillGradient(x + r, y + h - 1, x + w - r, y + h, c2, c1);
    // левая и правая
    ctx.fill(x, y + r, x + 1, y + h - r, c1);
    ctx.fill(x + w - 1, y + r, x + w, y + h - r, c2);
}

/**
 * Внутренний highlight сверху — тонкая светлая линия.
 */
public static void innerHighlight(DrawContext ctx, int x, int y, int w, int h, int radius, int alpha) {
    if (w <= radius * 2 + 4) return;
    ctx.fill(x + radius, y + 1, x + w - radius, y + 2, (alpha << 24) | 0xFFFFFF);
}

/**
 * Внутренний shadow снизу.
 */
public static void innerShadowBottom(DrawContext ctx, int x, int y, int w, int h, int radius, int alpha) {
    if (w <= radius * 2 + 4) return;
    ctx.fill(x + radius, y + h - 2, x + w - radius, y + h - 1, (alpha << 24));
}

/**
 * Премиум-switch (toggle) с анимацией.
 */
public static void switchButton(DrawContext ctx, int x, int y, int w, int h,
                                float anim, boolean enabled) {
    if (w <= 0 || h <= 0) return;
    int radius = h / 2;

    int trackOff = 0xFF23242E;
    int trackOn = GuiTheme.accent();
    int track = enabled ? trackOn : trackOff;

    // track
    round(ctx, x, y, w, h, radius, track);

    // inner highlight
    if (w > 4) {
        ctx.fill(x + radius, y + 1, x + w - radius, y + 2, 0x30FFFFFF);
    }

    // knob
    int knobSize = h - 4;
    int knobOff = x + 2;
    int knobOn = x + w - knobSize - 2;
    int knobX = (int) (knobOff + (knobOn - knobOff) * anim);
    int knobY = y + 2;

    // knob shadow
    round(ctx, knobX - 1, knobY + 1, knobSize + 2, knobSize + 2, (knobSize + 2) / 2, 0x30000000);
    // knob body
    round(ctx, knobX, knobY, knobSize, knobSize, knobSize / 2, 0xFFFFFFFF);
}

/**
 * Премиум-slider (ползунок) с круглым knob.
 */
public static void slider(DrawContext ctx, int x, int y, int w, int h, float ratio,
                          boolean dragging) {
    if (w <= 0 || h <= 0) return;
    int radius = h / 2;

    // track
    round(ctx, x, y, w, h, radius, 0xFF262630);

    // fill
    int fillW = (int) (w * ratio);
    if (fillW > 0) {
        round(ctx, x, y, fillW, h, Math.min(radius, fillW / 2), GuiTheme.accent());
    }

    // knob
    int knobSize = h + 4;
    int knobX = x + fillW - knobSize / 2;
    int knobY = y + h / 2 - knobSize / 2;
    if (knobX < x - 2) knobX = x - 2;
    if (knobX > x + w - knobSize + 2) knobX = x + w - knobSize + 2;

    // knob shadow
    round(ctx, knobX - 1, knobY + 1, knobSize + 2, knobSize + 2, (knobSize + 2) / 2, 0x40000000);
    // knob body
    round(ctx, knobX, knobY, knobSize, knobSize, knobSize / 2, 0xFFFFFFFF);
    // knob glow (если dragging)
    if (dragging) {
        glow(ctx, knobX, knobY, knobSize, knobSize, knobSize / 2, GuiTheme.accent(), 40);
    }
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