package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class BfbFont {
    private BfbFont() {}

    public static void draw(DrawContext ctx, String text, int x, int y, int color, boolean shadow) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (shadow) {
            ctx.drawTextWithShadow(tr, Text.literal(text), x, y, color);
        } else {
            ctx.drawText(tr, Text.literal(text), x, y, color, false);
        }
    }

    public static void drawCentered(DrawContext ctx, String text, int cx, int y, int color, boolean shadow) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(text);
        draw(ctx, text, cx - w / 2, y, color, shadow);
    }

    public static void drawRight(DrawContext ctx, String text, int rx, int y, int color, boolean shadow) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(text);
        draw(ctx, text, rx - w, y, color, shadow);
    }

    public static int getWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }

    public static int getHeight() {
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    /** Draw with gradient from colorLeft to colorRight */
    public static void drawGradient(DrawContext ctx, String text, int x, int y, int colorLeft, int colorRight) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int totalWidth = tr.getWidth(text);
        if (totalWidth == 0) return;
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int chW = tr.getWidth(ch);
            float t = totalWidth > 0 ? (float) (cx - x) / totalWidth : 0f;
            int color = RenderUtil.lerpArgb(colorLeft, colorRight, t);
            ctx.drawText(tr, Text.literal(ch), cx, y, color, false);
            cx += chW;
        }
    }

    /** Rainbow per-character draw */
    public static void drawRainbow(DrawContext ctx, String text, int x, int y, float speed, float spread) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float baseHue = (System.currentTimeMillis() % (long)(8000 / speed)) / (8000f / speed);
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float hue = (baseHue + i * spread) % 1f;
            int color = java.awt.Color.HSBtoRGB(hue, 0.8f, 1f) | 0xFF000000;
            ctx.drawText(tr, Text.literal(ch), cx, y, color, false);
            cx += tr.getWidth(ch);
        }
    }
}
