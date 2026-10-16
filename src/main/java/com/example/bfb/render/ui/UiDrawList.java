package com.example.bfb.render.ui;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UiDrawList {

    private final List<UiCommand> commands = new ArrayList<>();
    private int scissorDepth = 0;
    private boolean sealed = false;
    private boolean backdropQueued = false;
    private float opacity = 1f;

    public float opacity() { return opacity; }

    public UiDrawList opacity(float value) {
        checkMutable();
        if (!Float.isFinite(value)) value = 1f;
        opacity = Math.max(0f, Math.min(1f, value));
        return this;
    }

    public UiDrawList rect(float x, float y, float w, float h, int color) {
        return roundedRect(x, y, w, h, 0, color);
    }

    public UiDrawList roundedRect(float x, float y, float w, float h, float r, int color) {
        checkMutable();
        if (w <= 0 || h <= 0) return this;
        commands.add(new UiCommand.RoundedRect(x, y, w, h, clampRadius(r, w, h), tint(color)));
        return this;
    }

    public UiDrawList gradientRect(float x, float y, float w, float h, float r,
                                   int topColor, int bottomColor) {
        checkMutable();
        if (w <= 0 || h <= 0) return this;
        commands.add(new UiCommand.GradientRect(x, y, w, h, clampRadius(r, w, h),
                tint(topColor), tint(bottomColor)));
        return this;
    }

    public UiDrawList outline(float x, float y, float w, float h, float r,
                              float thickness, int color) {
        checkMutable();
        if (w <= 0 || h <= 0 || thickness <= 0) return this;
        commands.add(new UiCommand.Outline(x, y, w, h, clampRadius(r, w, h),
                thickness, tint(color)));
        return this;
    }

    public UiDrawList circle(float cx, float cy, float radius, int color) {
        checkMutable();
        if (radius <= 0) return this;
        commands.add(new UiCommand.Circle(cx, cy, radius, tint(color)));
        return this;
    }

    public UiDrawList circleGradient(float cx, float cy, float radius,
                                     int innerColor, int outerColor) {
        checkMutable();
        if (radius <= 0) return this;
        commands.add(new UiCommand.CircleGradient(cx, cy, radius,
                tint(innerColor), tint(outerColor)));
        return this;
    }

    public UiDrawList ring(float cx, float cy, float radius, float thickness,
                           float progress, int trackColor, int progressColor) {
        checkMutable();
        if (radius <= 0 || thickness <= 0) return this;
        commands.add(new UiCommand.Ring(cx, cy, radius, thickness,
                Math.max(0f, Math.min(1f, progress)),
                tint(trackColor), tint(progressColor)));
        return this;
    }

    public UiDrawList line(float x1, float y1, float x2, float y2,
                           float thickness, int color) {
        checkMutable();
        if (thickness <= 0) return this;
        commands.add(new UiCommand.Line(x1, y1, x2, y2, thickness, tint(color)));
        return this;
    }

    public UiDrawList triangle(float x1, float y1, float x2, float y2,
                               float x3, float y3, int color) {
        checkMutable();
        commands.add(new UiCommand.Triangle(x1, y1, x2, y2, x3, y3, tint(color)));
        return this;
    }

    // ================================
    // TEXT
    // ================================

    public UiDrawList text(float x, float y, float size, int color, String value) {
        return text(x, y, size, color, value, UiCommand.Align.LEFT, false);
    }

    public UiDrawList strongText(float x, float y, float size, int color, String value) {
        return text(x, y, size, color, value, UiCommand.Align.LEFT, true);
    }

    public UiDrawList centeredText(float x, float y, float size, int color,
                                   String value, boolean strong) {
        return text(x, y, size, color, value, UiCommand.Align.CENTER, strong);
    }

    public UiDrawList rightText(float x, float y, float size, int color,
                                String value, boolean strong) {
        return text(x, y, size, color, value, UiCommand.Align.RIGHT, strong);
    }

    public UiDrawList text(float x, float y, float size, int color, String value,
                           UiCommand.Align align, boolean strong) {
        checkMutable();
        if (value == null || value.isEmpty() || size <= 0) return this;
        commands.add(new UiCommand.Text(x, y, size, tint(color), value, align, strong));
        return this;
    }

    // ================================
    // ICON
    // ================================

    public UiDrawList icon(float x, float y, float size, int color, int codePoint) {
        checkMutable();
        if (size <= 0 || codePoint <= 0) return this;
        commands.add(new UiCommand.Icon(x, y, size, tint(color), codePoint));
        return this;
    }

    // ================================
    // IMAGE
    // ================================

    public UiDrawList image(float x, float y, float w, float h, float r,
                            Identifier id, int color) {
        return image(x, y, w, h, r, id, 0f, 0f, 1f, 1f, color);
    }

    public UiDrawList image(float x, float y, float w, float h, float r,
                            Identifier id,
                            float u0, float v0, float u1, float v1, int color) {
        checkMutable();
        if (w <= 0 || h <= 0 || id == null) return this;
        commands.add(new UiCommand.Image(x, y, w, h, clampRadius(r, w, h),
                id, u0, v0, u1, v1, tint(color)));
        return this;
    }

    // ================================
    // SCISSOR
    // ================================

    public UiDrawList pushScissor(float x, float y, float w, float h) {
        checkMutable();
        commands.add(new UiCommand.PushScissor(x, y, Math.max(0, w), Math.max(0, h)));
        scissorDepth++;
        return this;
    }

    public UiDrawList popScissor() {
        checkMutable();
        if (scissorDepth == 0) return this;
        commands.add(new UiCommand.PopScissor());
        scissorDepth--;
        return this;
    }

    // ================================
    // BACKDROP
    // ================================

    public UiDrawList backdropBlur(float x, float y, float w, float h, float r,
                                   float strength, int color) {
        checkMutable();
        if (w <= 0 || h <= 0 || backdropQueued) return this;
        commands.add(new UiCommand.BackdropBlur(x, y, w, h, clampRadius(r, w, h),
                Math.max(0f, strength), tint(color)));
        backdropQueued = true;
        return this;
    }

    // ================================
    // LIFECYCLE
    // ================================

    public List<UiCommand> seal() {
        while (scissorDepth > 0) {
            commands.add(new UiCommand.PopScissor());
            scissorDepth--;
        }
        sealed = true;
        return Collections.unmodifiableList(commands);
    }

    public boolean isEmpty() { return commands.isEmpty(); }
    public int size() { return commands.size(); }

    public void reset() {
        commands.clear();
        scissorDepth = 0;
        sealed = false;
        backdropQueued = false;
        opacity = 1f;
    }

    // ================================
    // INTERNAL
    // ================================

    private void checkMutable() {
        if (sealed) throw new IllegalStateException("UiDrawList already sealed");
    }

    private int tint(int color) {
        if (opacity >= 1f) return color;
        int a = (color >>> 24) & 0xFF;
        int newA = Math.round(a * opacity);
        return (newA << 24) | (color & 0x00FFFFFF);
    }

    private static float clampRadius(float r, float w, float h) {
        if (!Float.isFinite(r) || r <= 0) return 0;
        float max = Math.min(w, h) * 0.5f;
        return Math.min(r, max);
    }
}