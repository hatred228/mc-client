package com.example.bfb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class UiFont {

    private static final int ATLAS = 1024;

    public static final UiFont SIZE_6 = new UiFont("s6", "manrope-regular.ttf", 6);
    public static final UiFont SIZE_7 = new UiFont("s7", "manrope-regular.ttf", 7);
    public static final UiFont SIZE_8 = new UiFont("s8", "manrope-regular.ttf", 8);
    public static final UiFont SIZE_9 = new UiFont("s9", "manrope-regular.ttf", 9);
    public static final UiFont SIZE_10 = new UiFont("s10", "manrope-regular.ttf", 10);
    public static final UiFont SIZE_11 = new UiFont("s11", "manrope-regular.ttf", 11);

    public static UiFont forSize(float sizePx) {
        int s = Math.round(sizePx);
        return switch (s) {
            case 6 -> SIZE_6;
            case 7, 8 -> SIZE_7;
            case 9 -> SIZE_8;
            case 10 -> SIZE_9;
            case 11, 12 -> SIZE_10;
            default -> sizePx >= 13 ? SIZE_11 : SIZE_7;
        };
    }

    private final Identifier id;
    private final String resource;
    private final int displaySize;

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private NativeImage image;
    private NativeImageBackedTexture texture;
    private Font font;
    private int cachedScale = -1;
    private int commonAscent;
    private int commonHeight;
    private int penX = 1;
    private int penY = 1;
    private int rowHeight = 1;

    private UiFont(String key, String resource, int displaySize) {
        this.id = Identifier.of("undetected", "font/uifont_" + key);
        this.resource = resource;
        this.displaySize = displaySize;
    }

    public float width(String text) {
        if (text == null || text.isEmpty()) return 0f;
        ensure();
        float w = 0f;
        for (int i = 0; i < text.length(); i++) w += glyph(text.charAt(i)).advance;
        return w;
    }

    public float height() { return displaySize; }

    public void draw(DrawContext ctx, String text, float x, float y, int argb) {
        if (text == null || text.isEmpty() || (argb >>> 24) == 0) return;
        ensure();
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = glyph(text.charAt(i));
            if (g.w > 0 && g.h > 0) {
                int drawW = g.w / cachedScale;
                int drawH = g.h / cachedScale;
                if (drawW <= 0 || drawH <= 0) continue;
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id,
                        Math.round(cursor), Math.round(y),
                        g.u, g.v,
                        drawW, drawH,
                        g.w, g.h,
                        ATLAS, ATLAS,
                        argb);
            }
            cursor += g.advance;
        }
    }

    public String fit(String text, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (width(text) <= maxWidth) return text;
        float dots = width("...");
        if (dots >= maxWidth) return "";
        int end = text.length();
        while (end > 0 && width(text.substring(0, end)) + dots > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end).stripTrailing() + "...";
    }

    private void ensure() {
        int currentScale = Math.max(1, Math.min(4,
                MinecraftClient.getInstance().getWindow().getScaleFactor()));

        if (cachedScale == currentScale && texture != null) return;
        cachedScale = currentScale;

        int bakePx = displaySize * currentScale;
        font = loadFont(bakePx);

        // общие метрики — единый baseline для всех глифов
        Graphics2D mg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        mg.setFont(font);
        mg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        FontMetrics fm = mg.getFontMetrics();
        commonAscent = fm.getAscent();
        commonHeight = fm.getHeight();
        mg.dispose();

        if (image == null) {
            image = new NativeImage(NativeImage.Format.RGBA, ATLAS, ATLAS, false);
            texture = new NativeImageBackedTexture(() -> "bfb-uifont", image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        } else {
            image.fillRect(0, 0, ATLAS, ATLAS, 0);
        }

        glyphs.clear();
        penX = 1;
        penY = 1;
        rowHeight = 1;
        texture.upload();
    }

    private Font loadFont(int px) {
        try {
            InputStream is = UiFont.class.getResourceAsStream(
                    "/assets/undetected/font/" + resource);
            if (is != null) {
                Font base = Font.createFont(Font.TRUETYPE_FONT, is);
                is.close();
                return base.deriveFont(Font.PLAIN, (float) px);
            }
        } catch (Exception e) {
            System.err.println("[BFB] UiFont load failed: " + e.getMessage());
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, px);
    }

    private Glyph glyph(char c) {
        Glyph cached = glyphs.get(c);
        if (cached != null) return cached;

        Graphics2D mg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        mg.setFont(font);
        FontMetrics fm = mg.getFontMetrics();
        int cw = Math.max(1, fm.charWidth(c));
        mg.dispose();

        // padding до кратности cachedScale
        int rawBoxW = cw + 2;
        int rawBoxH = commonHeight + 2;
        int padRight = (cachedScale - (rawBoxW % cachedScale)) % cachedScale;
        int padBottom = (cachedScale - (rawBoxH % cachedScale)) % cachedScale;
        int boxW = rawBoxW + padRight;
        int boxH = rawBoxH + padBottom;

        BufferedImage scratch = new BufferedImage(boxW, boxH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scratch.createGraphics();
        prepare(g);
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(c), 1, commonAscent + 1);
        g.dispose();

        if (penX + boxW >= ATLAS) {
            penX = 1;
            penY += rowHeight + 1;
            rowHeight = 0;
        }
        if (penY + boxH >= ATLAS) {
            System.err.println("[BFB] UiFont atlas full");
            return new Glyph(0, 0, 0, 0, displaySize * 0.55f);
        }

        for (int py = 0; py < boxH; py++) {
            for (int px = 0; px < boxW; px++) {
                int argb = scratch.getRGB(px, py);
                int a = (argb >>> 24) & 0xFF;
                if (a <= 0) continue;
                image.setColorArgb(penX + px, penY + py, (a << 24) | 0xFFFFFF);
            }
        }
        texture.upload();

        float advance = (cw + 1) / (float) cachedScale;

        Glyph result = new Glyph(penX, penY, boxW, boxH, advance);
        penX += boxW + 1;
        rowHeight = Math.max(rowHeight, boxH);
        glyphs.put(c, result);
        return result;
    }

    private void prepare(Graphics2D g) {
        g.setTransform(new AffineTransform());
        g.setFont(font);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    private record Glyph(int u, int v, int w, int h, float advance) {}
}