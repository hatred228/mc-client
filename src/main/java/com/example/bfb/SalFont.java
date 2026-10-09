package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class SalFont {
    private static final int ATLAS = 1024;
    private static final int SIZE = 8;
    private static final Identifier ID = Identifier.of("libbase", "font/sal");
    private static final Map<Character, Glyph> GLYPHS = new HashMap<>();
    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static Font font;
    private static int penX = 1;
    private static int penY = 1;
    private static int rowHeight = 1;
    private static int lineHeight = SIZE;
    private static int scale = 1;
    private static boolean ready;

    private SalFont() {
    }

    public static int width(String text) {
        ensure();
        int width = 0;
        for (int i = 0; i < text.length(); i++) width += glyph(text.charAt(i)).advance;
        return width;
    }

    public static int height() {
        ensure();
        return lineHeight;
    }

    public static String fit(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (width(text) <= maxWidth) return text;
        String dots = "..";
        int limit = maxWidth - width(dots);
        if (limit <= 0) return "";
        int end = text.length();
        while (end > 0 && width(text.substring(0, end)) > limit) end--;
        return end == 0 ? "" : text.substring(0, end) + dots;
    }

    public static void draw(DrawContext context, String text, int x, int y, int color) {
        ensure();
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyph(text.charAt(i));
            if (glyph.width > 0 && glyph.height > 0 && glyph.drawWidth > 0 && glyph.drawHeight > 0) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, ID, cursor, y, glyph.u, glyph.v,
                        glyph.drawWidth, glyph.drawHeight, glyph.width, glyph.height, ATLAS, ATLAS, color);
            }
            cursor += glyph.advance;
        }
    }

    private static void ensure() {
        int nextScale = scale();
        if (ready && nextScale == scale) return;
        scale = nextScale;
        int pixels = SIZE * scale;
        font = loadFont(pixels);
        if (image == null) {
            image = new NativeImage(NativeImage.Format.RGBA, ATLAS, ATLAS, false);
            texture = new NativeImageBackedTexture(() -> "bfb-font", image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(ID, texture);
        } else {
            image.fillRect(0, 0, ATLAS, ATLAS, 0);
            texture.upload();
        }
        GLYPHS.clear();
        penX = 1;
        penY = 1;
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metricsGraphics = measure.createGraphics();
        prepare(metricsGraphics);
        var metrics = metricsGraphics.getFontMetrics();
        rowHeight = metrics.getHeight() + scale;
        lineHeight = Math.max(SIZE, (metrics.getHeight() + scale - 1) / scale);
        metricsGraphics.dispose();
        ready = true;
    }

    private static Font loadFont(int pixels) {
        try {
            InputStream is = SalFont.class.getResourceAsStream("/assets/undetected/fonts/Rubik-Regular.ttf");
            if (is != null) {
                Font base = Font.createFont(Font.TRUETYPE_FONT, is);
                is.close();
                return base.deriveFont(Font.PLAIN, (float) pixels);
            }
        } catch (Exception e) {
            System.err.println("[BFB] Failed to load Rubik font: " + e.getMessage());
        }
        Font fallback = new Font("Segoe UI", Font.PLAIN, pixels);
        if (!fallback.getFamily().toLowerCase().contains("segoe")) {
            fallback = new Font("Verdana", Font.PLAIN, pixels);
        }
        return fallback.deriveFont(Font.PLAIN, (float) pixels);
    }

    private static Glyph glyph(char character) {
        Glyph existing = GLYPHS.get(character);
        if (existing != null) return existing;
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metricsGraphics = measure.createGraphics();
        prepare(metricsGraphics);
        var metrics = metricsGraphics.getFontMetrics();
        int width = Math.max(scale, metrics.charWidth(character));
        int height = Math.max(scale, metrics.getHeight());
        int ascent = metrics.getAscent();
        metricsGraphics.dispose();
        BufferedImage scratch = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scratch.createGraphics();
        prepare(graphics);
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(Color.WHITE);
        graphics.drawString(String.valueOf(character), 1, ascent + 1);
        graphics.dispose();
        int pad = 1;
        int boxW = width + pad * 2;
        int boxH = height + pad;
        if (penX + boxW >= ATLAS) {
            penX = 1;
            penY += rowHeight;
        }
        if (penY + boxH < ATLAS) {
            for (int py = 0; py < boxH; py++) {
                for (int px = 0; px < boxW; px++) {
                    int argb = scratch.getRGB(px, py);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha < 24) argb = 0;
                    else if (alpha > 220) argb = 0xFFFFFFFF;
                    image.setColorArgb(penX + px, penY + py, argb);
                }
            }
            texture.upload();
        }
        int drawWidth = Math.max(1, (boxW + scale - 1) / scale);
        int drawHeight = Math.max(1, (boxH + scale - 1) / scale);
        int advance = Math.max(drawWidth, (metrics.charWidth(character) + scale - 1) / scale);
        Glyph glyph = new Glyph(penX, penY, boxW, boxH, drawWidth, drawHeight, advance);
        penX += boxW + scale;
        GLYPHS.put(character, glyph);
        return glyph;
    }

    private static void prepare(Graphics2D graphics) {
        graphics.setTransform(new AffineTransform());
        graphics.setFont(font);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    private static int scale() {
        int factor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (factor < 1) return 1;
        return Math.min(factor, 4);
    }

    private record Glyph(int u, int v, int width, int height, int drawWidth, int drawHeight, int advance) {
    }
}