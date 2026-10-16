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

public final class HudFont {

    private static final int ATLAS = 2048;
    private static final int BASE_SIZE = 9;
    private static final int OVERSAMPLE = 4;
    private static final int BAKED_SIZE = BASE_SIZE * OVERSAMPLE;   // 36
    private static final int PAD = 2;

    private static final Identifier ID = Identifier.of("undetected", "font/hud");

    private static final Map<Character, Glyph> GLYPHS = new HashMap<>();
    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static Font baseFont;
    private static boolean ready;
    private static boolean failed;

    private static int penX = 2;
    private static int penY = 2;
    private static int rowHeight = 1;

    private HudFont() {}

    public static void draw(DrawContext ctx, String text, float x, float y,
                            float sizePx, int color) {
        if (text == null || text.isEmpty() || sizePx <= 0) return;
        ensure();
        if (failed || baseFont == null) return;

        float k = sizePx / (float) BAKED_SIZE;
        // Округлить начальную позицию для стабильности
        float cursor = Math.round(x);
        float drawY = Math.round(y);

        for (int i = 0; i < text.length(); i++) {
            Glyph g = glyph(text.charAt(i));
            if (g == null || g.w <= 0 || g.h <= 0) {
                cursor += Math.round(4 * sizePx / BASE_SIZE);
                continue;
            }
            int dw = Math.max(1, Math.round(g.w * k));
            int dh = Math.max(1, Math.round(g.h * k));
            float gx = cursor - PAD * k;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ID,
                    (int)gx, (int)drawY,
                    g.u, g.v,
                    dw, dh,
                    g.w, g.h,
                    ATLAS, ATLAS,
                    color);
            cursor += g.advance * k;
        }
    }

    public static float width(String text, float sizePx) {
        if (text == null || text.isEmpty()) return 0;
        ensure();
        if (failed || baseFont == null) return text.length() * sizePx * 0.55f;
        float k = sizePx / (float) BAKED_SIZE;
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = glyph(text.charAt(i));
            w += (g == null ? 5f : g.advance) * k;
        }
        return w;
    }

    private static void ensure() {
        if (ready) return;
        ready = true;

        baseFont = loadFont(BAKED_SIZE);
        if (baseFont == null) { failed = true; return; }

        image = new NativeImage(NativeImage.Format.RGBA, ATLAS, ATLAS, false);
        texture = new NativeImageBackedTexture(() -> "bfb-hud-font", image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ID, texture);

        applyBilinearFilter(texture);
    }

    /** Пробуем все возможные пути включения билинейного фильтра. */
    private static void applyBilinearFilter(NativeImageBackedTexture tex) {
        // 1) методы прямо на NativeImageBackedTexture / AbstractTexture
        String[] direct = {"setFilter", "setTextureFilter", "setLinearFilter", "method_4624"};
        Class<?> cls = tex.getClass();
        while (cls != null) {
            for (String name : direct) {
                try {
                    var m = cls.getDeclaredMethod(name, boolean.class, boolean.class);
                    m.setAccessible(true);
                    m.invoke(tex, true, false);
                    System.out.println("[BFB] HudFont: bilinear via " + cls.getSimpleName() + "." + name);
                    return;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable e) {
                    System.err.println("[BFB] HudFont invoke failed: " + e.getMessage());
                }
            }
            cls = cls.getSuperclass();
        }

        // 2) через GpuTexture
        Object gpu = null;
        for (String getter : new String[]{"getGlTexture", "getTexture", "getGpuTexture"}) {
            try {
                var m = tex.getClass().getMethod(getter);
                gpu = m.invoke(tex);
                if (gpu != null) break;
            } catch (Throwable ignored) {}
        }
        if (gpu != null) {
            System.out.println("[BFB] HudFont: GpuTexture = " + gpu.getClass().getName());
            for (String name : new String[]{"setTextureFilter", "setFilter", "setSampler"}) {
                try {
                    var m = gpu.getClass().getMethod(name, boolean.class, boolean.class);
                    m.invoke(gpu, true, false);
                    System.out.println("[BFB] HudFont: bilinear via GpuTexture." + name);
                    return;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable e) {
                    System.err.println("[BFB] HudFont gpu invoke failed: " + e.getMessage());
                }
            }

            // 3) через sampler record — поищем метод setSampler(Sampler) или поле
            for (var m : gpu.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("sampler")) {
                    System.out.println("[BFB] HudFont: sampler method found — " + m.getName());
                }
            }
        }

        System.err.println("[BFB] HudFont: bilinear filter not found — text may be pixelated");
    }

    private static Font loadFont(int px) {
        try {
            InputStream is = HudFont.class.getResourceAsStream(
                    "/assets/undetected/font/manrope-regular.ttf");
            if (is == null) {
                System.err.println("[BFB] HudFont: manrope-regular.ttf not found");
                return null;
            }
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            is.close();
            return f.deriveFont(Font.PLAIN, (float) px);
        } catch (Exception e) {
            System.err.println("[BFB] HudFont load failed: " + e.getMessage());
            return null;
        }
    }

    private static Glyph glyph(char c) {
        Glyph g = GLYPHS.get(c);
        if (g != null) return g;

        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = measure.createGraphics();
        prepare(mg);
        FontMetrics fm = mg.getFontMetrics();
        int cw = fm.charWidth(c);
        int h = fm.getHeight();
        int asc = fm.getAscent();
        mg.dispose();

        if (cw <= 0) {
            Glyph empty = new Glyph(0, 0, 0, 0, fm.charWidth(' '));
            GLYPHS.put(c, empty);
            return empty;
        }

        int boxW = cw + PAD * 2;
        int boxH = h + PAD * 2;

        if (penX + boxW >= ATLAS) {
            penX = 2;
            penY += rowHeight + 2;
            rowHeight = 0;
        }
        if (penY + boxH >= ATLAS) {
            System.err.println("[BFB] HudFont atlas full at '" + c + "'");
            return null;
        }

        BufferedImage scratch = new BufferedImage(boxW, boxH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scratch.createGraphics();
        prepare(g2);
        g2.setComposite(AlphaComposite.Src);
        g2.setColor(Color.WHITE);
        g2.drawString(String.valueOf(c), PAD, asc + PAD);
        g2.dispose();

        for (int py = 0; py < boxH; py++) {
            for (int px = 0; px < boxW; px++) {
                int argb = scratch.getRGB(px, py);
                image.setColorArgb(penX + px, penY + py, argb);
            }
        }
        texture.upload();

        Glyph result = new Glyph(penX, penY, boxW, boxH, cw);
        penX += boxW + 2;
        rowHeight = Math.max(rowHeight, boxH);
        GLYPHS.put(c, result);
        return result;
    }

    private static void prepare(Graphics2D g) {
        g.setTransform(new AffineTransform());
        g.setFont(baseFont);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    private record Glyph(int u, int v, int w, int h, int advance) {}
}