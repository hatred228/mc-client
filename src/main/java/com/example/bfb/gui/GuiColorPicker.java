package com.example.bfb.gui;

import com.example.bfb.setting.ColorSetting;
import net.minecraft.client.gui.DrawContext;

public final class GuiColorPicker {

    public static final float SV_W = 130f;
    public static final float SV_H = 90f;
    public static final float HUE_H = 10f;
    public static final float GAP = 6f;
    public static final float TOTAL_W = SV_W;
    public static final float TOTAL_H = SV_H + GAP + HUE_H + GAP + 14f;

    private GuiColorPicker() {}

    // ============================================================
    // RENDER
    // ============================================================

    public static void render(DrawContext ctx, ColorSetting color, float x, float y, float hue) {
        // Фон и рамка
        Draw.rect(ctx, x - 6, y - 6, TOTAL_W + 12, TOTAL_H + 12, 5f, 0xF00E0E16);
        Draw.border(ctx, x - 6, y - 6, TOTAL_W + 12, TOTAL_H + 12, 5f, 1f, 0x30FFFFFF);

        // ─── SV квадрат: 130 вертикальных колонок ───
        // каждая колонка: S постоянная, V меняется от 1 (верх) до 0 (низ)
        // ось X = S (0=белый слева, 1=чистый hue справа)
        // ось Y = V (1=яркий сверху, 0=чёрный снизу)
        int cols = (int) SV_W;
        for (int i = 0; i < cols; i++) {
            float s = (cols <= 1) ? 0f : i / (float) (cols - 1);
            int topColor = hsvToRgb(hue, s, 1f);        // V=1
            int bottomColor = hsvToRgb(hue, s, 0f);     // V=0 (чёрный)
            ctx.fillGradient(
                    Math.round(x) + i, Math.round(y),
                    Math.round(x) + i + 1, Math.round(y + SV_H),
                    topColor, bottomColor);
        }

        // ─── Шарик на SV ───
        float[] hsv = rgbToHsv(color.getRgb());
        float sPos = hsv[1];
        float vPos = hsv[2];
        float ballX = x + sPos * SV_W;
        float ballY = y + (1f - vPos) * SV_H;

        circle(ctx, ballX, ballY, 5f, 0x80000000);
        circle(ctx, ballX, ballY, 4f, 0xFFFFFFFF);
        circle(ctx, ballX, ballY, 2.5f, 0xFF000000 | (color.getRgb() & 0xFFFFFF));

        // ─── Hue bar — тоже колонками ───
        float hueY = y + SV_H + GAP;
        int hueCols = (int) TOTAL_W;
        for (int i = 0; i < hueCols; i++) {
            float h = (hueCols <= 1) ? 0f : i / (float) (hueCols - 1);
            int c = hsvToRgb(h, 1f, 1f);
            ctx.fill(Math.round(x) + i, Math.round(hueY),
                    Math.round(x) + i + 1, Math.round(hueY + HUE_H), c);
        }
        Draw.border(ctx, x, hueY, TOTAL_W, HUE_H, 2f, 1f, 0x40FFFFFF);

        // knob hue
        float knobX = x + hue * TOTAL_W;
        ctx.fill(Math.round(knobX - 1f), Math.round(hueY - 2f),
                Math.round(knobX + 1f), Math.round(hueY + HUE_H + 2f), 0xFF000000);
        ctx.fill(Math.round(knobX - 0.5f), Math.round(hueY - 2f),
                Math.round(knobX + 0.5f), Math.round(hueY + HUE_H + 2f), 0xFFFFFFFF);

        // ─── Hex превью ───
        float infoY = hueY + HUE_H + GAP;
        ctx.fill(Math.round(x), Math.round(infoY),
                Math.round(x + 16), Math.round(infoY + 14), 0xFF000000 | color.getRgb());
        Draw.border(ctx, x, infoY, 16f, 14f, 2f, 1f, 0x40FFFFFF);

        String hex = String.format("#%06X", color.getRgb() & 0xFFFFFF);
        UiFont.forSize(7).draw(ctx, hex, x + 22f, infoY + 3.5f, 0xFFF4F6FF);
    }

    // ============================================================
    // INPUT
    // ============================================================

    public static boolean mouseClick(ColorSetting color, double mx, double my,
                                     float x, float y, float hue, int button) {
        if (button != 0) return false;

        // SV
        if (mx >= x && mx <= x + SV_W && my >= y && my <= y + SV_H) {
            float s = (float) ((mx - x) / SV_W);
            float v = 1f - (float) ((my - y) / SV_H);
            setSV(color, s, v, hue);
            return true;
        }

        // Hue
        float hueY = y + SV_H + GAP;
        if (mx >= x && mx <= x + SV_W && my >= hueY && my <= hueY + HUE_H) {
            float h = (float) ((mx - x) / SV_W);
            float[] hsv = rgbToHsv(color.getRgb());
            color.setRgb(hsvToRgb(h, Math.max(0.01f, hsv[1]), Math.max(0.01f, hsv[2])) & 0xFFFFFF);
            com.example.bfb.BfbConfig.save();
            return true;
        }

        return false;
    }

    public static boolean mouseDrag(ColorSetting color, double mx, double my,
                                    float x, float y, float hue) {
        if (mx >= x && mx <= x + SV_W && my >= y && my <= y + SV_H) {
            float s = (float) ((mx - x) / SV_W);
            float v = 1f - (float) ((my - y) / SV_H);
            setSV(color, s, v, hue);
            return true;
        }
        float hueY = y + SV_H + GAP;
        if (mx >= x && mx <= x + SV_W && my >= hueY && my <= hueY + HUE_H) {
            float h = (float) ((mx - x) / SV_W);
            float[] hsv = rgbToHsv(color.getRgb());
            color.setRgb(hsvToRgb(h, Math.max(0.01f, hsv[1]), Math.max(0.01f, hsv[2])) & 0xFFFFFF);
            return true;
        }
        return false;
    }

    // ============================================================
    // UTILS
    // ============================================================

    private static void setSV(ColorSetting color, float s, float v, float hue) {
        s = Math.max(0f, Math.min(1f, s));
        v = Math.max(0f, Math.min(1f, v));
        color.setRgb(hsvToRgb(hue, s, v) & 0xFFFFFF);
    }

    private static void circle(DrawContext ctx, float cx, float cy, float r, int color) {
        int ir = Math.round(r);
        int icx = Math.round(cx), icy = Math.round(cy);
        for (int y = -ir; y <= ir; y++) {
            int dx = (int) Math.sqrt((double) ir * ir - (double) y * y);
            ctx.fill(icx - dx, icy + y, icx + dx + 1, icy + y + 1, color);
        }
    }

    public static int hsvToRgb(float h, float s, float v) {
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

    public static float[] rgbToHsv(int rgb) {
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
}