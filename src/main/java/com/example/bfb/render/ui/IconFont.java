package com.example.bfb.render.ui;

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

/**
 * Рендерер иконок из lucide.ttf.
 *
 * Атлас 1024x1024. Каждая иконка — клетка 32x32.
 * При рендере region 32x32 масштабируется в квадрат size x size.
 * Чётко на размерах <= 32. На больших — bilinear upscale (визуально ок).
 */
public final class IconFont {

    private static final int ATLAS_SIZE = 1024;
    private static final int CELL = 32;
    private static final Identifier ID = Identifier.of("undetected", "icon/lucide");

    private static final Map<Integer, Glyph> GLYPHS = new HashMap<>();
    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static Font font;
    private static int penX = 2;
    private static int penY = 2;
    private static boolean ready;
    private static boolean failed;

    private IconFont() {}

    public static void draw(DrawContext ctx, int codePoint, float x, float y,
                            float size, int color) {
        if (size <= 0 || color == 0) return;
        ensure();
        if (failed || font == null) {
            int xi = Math.round(x);
            int yi = Math.round(y);
            int si = Math.round(size);
            ctx.fill(xi + si / 4, yi + si / 4, xi + 3 * si / 4, yi + 3 * si / 4, color);
            return;
        }
        Glyph glyph = glyph(codePoint);
        if (glyph == null) return;

        int ix = Math.round(x);
        int iy = Math.round(y);
        int is = Math.round(size);

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ID,
                ix, iy,
                (float) glyph.u, (float) glyph.v,
                is, is,
                ATLAS_SIZE, ATLAS_SIZE,
                CELL, CELL,
                color);
    }

    /** Прогрев: загрузить шрифт и первые глифы при старте клиента. */
    public static void preload() {
        ensure();
        if (failed) {
            System.err.println("[BFB] IconFont preload FAILED — lucide.ttf not loaded");
            return;
        }
        for (int cp : new int[]{Icons.SWORDS, Icons.USER, Icons.SETTINGS, Icons.SEARCH}) {
            glyph(cp);
        }
        System.out.println("[BFB] IconFont preload OK, glyphs cached: " + GLYPHS.size());
    }

    private static void ensure() {
        if (ready) return;
        ready = true;

        font = loadFont();
        if (font == null) {
            failed = true;
            return;
        }

        image = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        texture = new NativeImageBackedTexture(() -> "bfb-icon", image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ID, texture);
    }

    private static Font loadFont() {
        try {
            InputStream is = IconFont.class.getResourceAsStream(
                    "/assets/undetected/fonts/lucide.ttf");
            if (is == null) {
                System.err.println("[BFB] lucide.ttf not found in resources");
                return null;
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, is);
            is.close();
            return base.deriveFont(Font.PLAIN, (float) (CELL - 4));
        } catch (Exception e) {
            System.err.println("[BFB] Failed to load lucide.ttf: " + e.getMessage());
            return null;
        }
    }

    private static Glyph glyph(int codePoint) {
        Glyph existing = GLYPHS.get(codePoint);
        if (existing != null) return existing;
        if (font == null) return null;

        if (penX + CELL >= ATLAS_SIZE) {
            penX = 2;
            penY += CELL + 2;
        }
        if (penY + CELL >= ATLAS_SIZE) {
            System.err.println("[BFB] Icon atlas full at codepoint " + codePoint);
            return null;
        }

        String str = new String(Character.toChars(codePoint));

        BufferedImage scratch = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scratch.createGraphics();
        prepare(g);
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.WHITE);

        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(str);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int textX = (CELL - textW) / 2;
        int textY = (CELL - ascent + descent) / 2 + ascent;

        g.drawString(str, textX, textY);
        g.dispose();

        for (int py = 0; py < CELL; py++) {
            for (int px = 0; px < CELL; px++) {
                int argb = scratch.getRGB(px, py);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < 24) continue;
                if (alpha > 220) argb = 0xFFFFFFFF;
                image.setColorArgb(penX + px, penY + py, argb);
            }
        }
        texture.upload();

        Glyph glyph = new Glyph(penX, penY);
        penX += CELL + 2;
        GLYPHS.put(codePoint, glyph);
        return glyph;
    }

    private static void prepare(Graphics2D g) {
        g.setTransform(new AffineTransform());
        g.setFont(font);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    private record Glyph(int u, int v) {}
}