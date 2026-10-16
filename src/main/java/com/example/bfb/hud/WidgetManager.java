package com.example.bfb.hud;

import com.example.bfb.render.ui.UiDrawList;
import com.example.bfb.render.ui.UiRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class WidgetManager {

    private final List<HudWidget> widgets = new ArrayList<>();
    private final HudCompactStyle.Menu menu = new HudCompactStyle.Menu();

    private float scale = 1f;
    private float viewportWidth, viewportHeight;
    private long previousFrame;
    private HudWidget dragging;
    private HudWidget menuOwner;
    private float userScale = 1f;

    public WidgetManager() {}

    public void register(HudWidget widget) { widgets.add(widget); }

    public void setUserScale(float value) {
        this.userScale = Math.max(0.5f, Math.min(2f, value));
    }

    public List<HudWidget> widgets() { return widgets; }

    // ============================================================
    // RENDER
    // ============================================================

    public void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.nanoTime();
        float dt = previousFrame == 0 ? 1f / 60f
                : Math.min(.05f, (now - previousFrame) / 1_000_000_000f);
        previousFrame = now;

        viewportWidth = ctx.getScaledWindowWidth();
        viewportHeight = ctx.getScaledWindowHeight();

        float targetScale = HudCompactStyle.referenceScale(
                viewportWidth, viewportHeight, userScale);
        scale = HudCompactStyle.approach(scale, targetScale, dt, 14);

        boolean inChat = mc.currentScreen instanceof ChatScreen;
        if (!inChat) {
            stopDragging();
            if (menu.isOpen()) {
                menu.close();
                menuOwner = null;
            }
        }

        // ОБНОВЛЕНИЕ DRAG — тянем виджет за курсором каждый кадр
        if (dragging != null && mc.getWindow() != null) {
            long handle = mc.getWindow().getHandle();
            // проверка отжатия ЛКМ
            boolean pressed = GLFW.glfwGetMouseButton(
                    handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (!pressed) {
                stopDragging();
            } else {
                double mx = mc.mouse.getX() * viewportWidth / mc.getWindow().getWidth();
                double my = mc.mouse.getY() * viewportHeight / mc.getWindow().getHeight();
                dragging.drag(mx, my);
            }
        }

        UiDrawList ui = new UiDrawList();

        for (HudWidget widget : widgets) {
            widget.render(ctx, ui, scale);
        }

        if (menu.isOpen()) {
            menu.render(ui);
        }

        UiRenderer.submit(ctx, ui);
    }

    // ============================================================
    // INPUT
    // ============================================================

    public boolean mouseButton(double mx, double my, int button, int action) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (button == 0 && action == 0 && dragging != null) {
            stopDragging();
            return true;
        }

        if (!(mc.currentScreen instanceof ChatScreen)) return false;

        if (menu.isOpen()) {
            if (menu.mouseClick(mx, my, button, action)) return true;
        }

        for (int i = widgets.size() - 1; i >= 0; i--) {
            HudWidget widget = widgets.get(i);
            if (!widget.contains(mx, my)) continue;

            if (widget.options() != null
                    && (button == 1 || (button == 0 && widget.menuHit(mx, my)))) {
                stopDragging();
                menuOwner = widget;
                menu.show(widget.options(),
                        widget.left() + widget.widgetWidth() - 12 * scale,
                        widget.top(),
                        scale,
                        viewportWidth,
                        viewportHeight);
                return true;
            }

            if (button == 0) {
                stopDragging();
                dragging = widget;
                widget.beginDrag(mx, my);
                return true;
            }
        }

        return false;
    }

    private void stopDragging() {
        if (dragging != null) dragging.stopDrag();
        dragging = null;
    }

    public void close() {
        stopDragging();
        if (menu.isOpen()) menu.close();
        menuOwner = null;
    }

    public float scale() { return scale; }
}