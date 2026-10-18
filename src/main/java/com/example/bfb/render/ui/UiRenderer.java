package com.example.bfb.render.ui;

import com.example.bfb.gui.GlowTextureCache;
import com.example.bfb.gui.HudDraw;
import com.example.bfb.gui.RingTextureCache;
import com.example.bfb.gui.UiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class UiRenderer {

    private static final Identifier LUCIDE_ID = Identifier.of("undetected", "lucide");
    private static final StyleSpriteSource LUCIDE_FONT = new StyleSpriteSource.Font(LUCIDE_ID);

    private UiRenderer() {}

    public static void submit(DrawContext ctx, UiDrawList list) {
        if (list == null || list.isEmpty()) return;
        List<UiCommand> commands = list.seal();
        for (UiCommand cmd : commands) {
            try { draw(ctx, cmd); } catch (Throwable ignored) {}
        }
    }

    private static void draw(DrawContext ctx, UiCommand cmd) {
        switch (cmd) {
            case UiCommand.Rect r -> ctx.fill(
                    Math.round(r.x()), Math.round(r.y()),
                    Math.round(r.x() + r.w()), Math.round(r.y() + r.h()),
                    r.color());

            case UiCommand.RoundedRect rr -> HudDraw.round(ctx,
                    Math.round(rr.x()), Math.round(rr.y()),
                    Math.round(rr.w()), Math.round(rr.h()),
                    Math.round(rr.r()), rr.color());

            case UiCommand.GradientRect gr -> HudDraw.roundGradient(ctx,
                    Math.round(gr.x()), Math.round(gr.y()),
                    Math.round(gr.w()), Math.round(gr.h()),
                    Math.round(gr.r()), gr.topColor(), gr.bottomColor());

            case UiCommand.Outline o -> drawOutline(ctx, o);

            case UiCommand.Circle c -> drawCircle(ctx, c);

            case UiCommand.CircleGradient cg -> drawCircleGradient(ctx, cg);

            case UiCommand.Ring ring -> drawRing(ctx, ring);

            case UiCommand.Line l -> drawLine(ctx, l);

            case UiCommand.Triangle t -> drawTriangle(ctx, t);

            case UiCommand.PushScissor s -> ctx.enableScissor(
                    Math.round(s.x()), Math.round(s.y()),
                    Math.round(s.x() + s.w()), Math.round(s.y() + s.h()));

            case UiCommand.PopScissor ignored -> ctx.disableScissor();

            case UiCommand.Text t -> drawText(ctx, t);

            case UiCommand.Icon icon -> drawIcon(ctx, icon);

            case UiCommand.Image img -> drawImage(ctx, img);

            case UiCommand.BackdropBlur blur -> HudDraw.round(ctx,
                    Math.round(blur.x()), Math.round(blur.y()),
                    Math.round(blur.w()), Math.round(blur.h()),
                    Math.round(blur.r()), blur.color());
        }
    }

    // ================================
    // TEXT — через UiFont (4x SS + downsample)
    // ================================

    private static void drawText(DrawContext ctx, UiCommand.Text t) {
        String value = t.value();
        if (value == null || value.isEmpty()) return;

        int color = t.color();
        float size = t.size() <= 0 ? 7f : t.size();
        UiFont font = UiFont.forSize(size);

        float textW = font.width(value);

        float drawX = switch (t.align()) {
            case CENTER -> t.x() - textW * 0.5f;
            case RIGHT -> t.x() - textW;
            default -> t.x();
        };

        font.draw(ctx, value, drawX, t.y(), color);
    }

    // ================================
    // ICON — Lucide через Minecraft TextRenderer
    // ================================

    private static void drawIcon(DrawContext ctx, UiCommand.Icon icon) {
        if (icon.size() <= 0 || icon.codePoint() <= 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        String value = new String(Character.toChars(icon.codePoint()));
        MutableText text = Text.literal(value).styled(s -> s.withFont(LUCIDE_FONT));

        int color = icon.color();
        int textW = mc.textRenderer.getWidth(text);

        float drawX = icon.x() + (icon.size() - textW) * 0.5f;
        float drawY = icon.y();

        ctx.drawText(mc.textRenderer, text,
                Math.round(drawX), Math.round(drawY), color, false);
    }

    // ================================
    // OUTLINE
    // ================================

    private static void drawOutline(DrawContext ctx, UiCommand.Outline o) {
        int x = Math.round(o.x());
        int y = Math.round(o.y());
        int w = Math.round(o.w());
        int h = Math.round(o.h());
        int r = Math.round(o.r());
        int t = Math.max(1, Math.round(o.thickness()));
        int color = o.color();

        if (r <= 0) {
            ctx.fill(x, y, x + w, y + t, color);
            ctx.fill(x, y + h - t, x + w, y + h, color);
            ctx.fill(x, y + t, x + t, y + h - t, color);
            ctx.fill(x + w - t, y + t, x + w, y + h - t, color);
            return;
        }
        if (r > Math.min(w, h) / 2) r = Math.min(w, h) / 2;
        ctx.fill(x + r, y, x + w - r, y + t, color);
        ctx.fill(x + r, y + h - t, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + t, y + h - r, color);
        ctx.fill(x + w - t, y + r, x + w, y + h - r, color);
    }

    // ================================
    // CIRCLE
    // ================================

    private static void drawCircle(DrawContext ctx, UiCommand.Circle c) {
        int cx = Math.round(c.cx());
        int cy = Math.round(c.cy());
        int r = Math.round(c.radius());
        int color = c.color();
        for (int y = -r; y <= r; y++) {
            int dx = (int) Math.sqrt((double) r * r - (double) y * y);
            ctx.fill(cx - dx, cy + y, cx + dx + 1, cy + y + 1, color);
        }
    }

    private static void drawCircleGradient(DrawContext ctx, UiCommand.CircleGradient cg) {
        int cx = Math.round(cg.cx());
        int cy = Math.round(cg.cy());
        int r = Math.round(cg.radius());
        int d = r * 2;
        if (d <= 0) return;
        Identifier tex = GlowTextureCache.get(d);
        if (tex == null) return;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex,
                cx - r, cy - r, 0f, 0f, d, d, d, d, d, d, cg.outerColor());
        int innerR = (int) (r * 0.75f);
        int d2 = innerR * 2;
        if (d2 > 0) {
            Identifier tex2 = GlowTextureCache.get(d2);
            if (tex2 != null) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex2,
                        cx - innerR, cy - innerR, 0f, 0f, d2, d2, d2, d2, d2, d2,
                        cg.innerColor());
            }
        }
    }

    // ================================
    // RING
    // ================================

    private static void drawRing(DrawContext ctx, UiCommand.Ring ring) {
        int cx = Math.round(ring.cx());
        int cy = Math.round(ring.cy());
        int r = Math.round(ring.radius());
        int t = Math.max(1, Math.round(ring.thickness()));
        float progress = Math.max(0f, Math.min(1f, ring.progress()));
        int size = r * 2 + 2;
        if (size <= 2) return;
        Identifier[] arcs = RingTextureCache.getArcs(size, t);
        if (arcs == null || arcs.length <= RingTextureCache.ARC_STEPS) return;
        int ax = cx - r - 1;
        int ay = cy - r - 1;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, arcs[RingTextureCache.ARC_STEPS],
                ax, ay, 0f, 0f, size, size, size, size, size, size, ring.trackColor());
        int idx = Math.round(progress * RingTextureCache.ARC_STEPS);
        if (idx > 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, arcs[idx],
                    ax, ay, 0f, 0f, size, size, size, size, size, size, ring.progressColor());
        }
    }

    // ================================
    // LINE / TRIANGLE / IMAGE
    // ================================

    private static void drawLine(DrawContext ctx, UiCommand.Line l) {
        float dx = l.x2() - l.x1();
        float dy = l.y2() - l.y1();
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.1f) return;
        int t = Math.max(1, Math.round(l.thickness()));
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(l.x1(), l.y1());
        matrices.rotate((float) Math.atan2(dy, dx));
        ctx.fill(0, -t / 2, (int) Math.ceil(length), t / 2 + 1, l.color());
        matrices.popMatrix();
    }

    private static void drawTriangle(DrawContext ctx, UiCommand.Triangle t) {
        int color = t.color();
        float minX = Math.min(t.x1(), Math.min(t.x2(), t.x3()));
        float maxX = Math.max(t.x1(), Math.max(t.x2(), t.x3()));
        float minY = Math.min(t.y1(), Math.min(t.y2(), t.y3()));
        float maxY = Math.max(t.y1(), Math.max(t.y2(), t.y3()));
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.ceil(maxY);
        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.ceil(maxX);

        for (int py = y0; py <= y1; py++) {
            float fy = py + 0.5f;
            int runStart = -1;
            for (int px = x0; px <= x1; px++) {
                float fx = px + 0.5f;
                if (pointInTriangle(fx, fy, t)) {
                    if (runStart < 0) runStart = px;
                } else {
                    if (runStart >= 0) {
                        ctx.fill(runStart, py, px, py + 1, color);
                        runStart = -1;
                    }
                }
            }
            if (runStart >= 0) ctx.fill(runStart, py, x1 + 1, py + 1, color);
        }
    }

    private static boolean pointInTriangle(float px, float py, UiCommand.Triangle t) {
        float d1 = sign(px, py, t.x1(), t.y1(), t.x2(), t.y2());
        float d2 = sign(px, py, t.x2(), t.y2(), t.x3(), t.y3());
        float d3 = sign(px, py, t.x3(), t.y3(), t.x1(), t.y1());
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static float sign(float px, float py, float x1, float y1, float x2, float y2) {
        return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
    }

    private static void drawImage(DrawContext ctx, UiCommand.Image img) {
        int x = Math.round(img.x());
        int y = Math.round(img.y());
        int w = Math.round(img.w());
        int h = Math.round(img.h());
        int regW = Math.max(1, Math.round((img.u1() - img.u0()) * w));
        int regH = Math.max(1, Math.round((img.v1() - img.v0()) * h));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, img.id(),
                x, y, img.u0() * w, img.v0() * h, w, h,
                w, h, regW, regH, img.color());
    }
}