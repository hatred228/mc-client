package com.example.bfb.hud;

import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class HudWidget {

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected String widgetName;
    protected boolean enabled = true;
    protected float x, y;
    protected float width, height;
    protected boolean customized = false;
    protected final HudAnimation visibility = new HudAnimation(1.0);

    private boolean dragging;
    private float dragOx, dragOy;
    protected DrawContext ctx;
    protected float scale = 1f;

    public HudWidget(String name, float x, float y) {
        this.widgetName = name;
        this.x = x;
        this.y = y;
    }

    public String name() { return widgetName; }
    public String getName() { return widgetName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public void toggle() { enabled = !enabled; }

    public float left() { return x; }
    public float top() { return y; }
    public float widgetWidth() { return width; }
    public float widgetHeight() { return height; }
    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    public int getWidth() { return (int) width; }
    public int getHeight() { return (int) height; }

    public void setPosition(float x, float y) {
        this.x = x; this.y = y; this.customized = true;
    }
    public void setPosition(int x, int y) {
        this.x = x; this.y = y; this.customized = true;
    }

    protected void place(DrawContext context, float w, float h) {
        float vw = context.getScaledWindowWidth();
        float vh = context.getScaledWindowHeight();
        x = Math.max(0, Math.min(x, vw - w));
        y = Math.max(0, Math.min(y, vh - h));
        width = w;
        height = h;
    }

    /** anchor: 0 = left/top, 1 = right/bottom, 0.5 = center. */
    protected void anchor(DrawContext context, float w, float h, float anchorX, float anchorY) {
        float vw = context.getScaledWindowWidth();
        float vh = context.getScaledWindowHeight();
        if (!customized) {
            x = (vw - w) * anchorX;
            y = (vh - h) * anchorY;
        }
        x = Math.max(0, Math.min(x, vw - w));
        y = Math.max(0, Math.min(y, vh - h));
        width = w;
        height = h;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public boolean menuHit(double mx, double my) {
        float size = 12 * scale;
        return mx >= x + width - size && mx <= x + width && my >= y && my <= y + size;
    }

    public HudCompactStyle.Options options() { return null; }

    public void beginDrag(double mx, double my) {
        dragging = true;
        customized = true;
        dragOx = (float) (mx - x);
        dragOy = (float) (my - y);
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        x = (float) (mx - dragOx);
        y = (float) (my - dragOy);
    }

    public void stopDrag() { dragging = false; }
    public boolean isDragging() { return dragging; }

    public void render(DrawContext ctx, UiDrawList ui, float scale) {
        if (!enabled) return;
        this.ctx = ctx;
        this.scale = scale;
        draw(ui, scale);
    }

    public void render(DrawContext ctx, float tickDelta) {}
    protected void draw(UiDrawList ui, float scale) {}

    protected float s(float value) { return value * scale; }

    protected float textWidth(String text, float sizePx) {
        if (text == null || text.isEmpty()) return 0;
        return HudCompactStyle.textWidth(text, sizePx) * scale;
    }

    protected float textWidth(String text) { return textWidth(text, 7f); }

    protected String fit(String text, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (textWidth(text, 7f) <= maxWidth) return text;
        String dots = "..";
        float dotsW = textWidth(dots, 7f);
        float limit = maxWidth - dotsW;
        int end = text.length();
        while (end > 0 && textWidth(text.substring(0, end), 7f) > limit) end--;
        return end == 0 ? "" : text.substring(0, end) + dots;
    }

    public boolean isMouseOver(double mx, double my) { return contains(mx, my); }
    public void startDrag(int mx, int my) { beginDrag(mx, my); }
    public void update() {}

    protected MinecraftClient getClient() { return mc; }
    protected int ix() { return (int) x; }
    protected int iy() { return (int) y; }
    protected int iw() { return (int) width; }
    protected int ih() { return (int) height; }
}