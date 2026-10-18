package com.example.bfb.hud;

import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ListHudWidget<T> extends HudWidget {

    private final HudCompactStyle.Options options;
    private final int headerIcon;
    private final float anchorX, anchorY;
    private final Map<Object, Row<T>> rows = new LinkedHashMap<>();

    private float animatedHeight;
    private long previousFrame;

    protected ListHudWidget(String name, int headerIcon, float anchorX, float anchorY,
                            HudCompactStyle.Options options) {
        super(name, 0, 0);
        this.options = options;
        this.headerIcon = headerIcon;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    @Override public final HudCompactStyle.Options options() { return options; }

    protected abstract List<T> entries();
    protected abstract Object key(T entry);
    protected abstract String left(T entry);
    protected abstract String right(T entry);
    protected String level(T entry) { return ""; }
    protected String valueOption() { return "Duration"; }
    protected boolean nativeIcon(T entry) { return false; }
    protected void drawNativeIcon(DrawContext ctx, T entry, float iconX, float iconY,
                                   float iconSize, int alpha) {}

    @Override
    protected final void draw(UiDrawList ui, float scale) {
        var player = mc.player;
        if (player == null) { rows.clear(); width = height = 0; return; }

        long now = System.nanoTime();
        float dt = previousFrame == 0 ? 1f / 60f
                : Math.min(.05f, (now - previousFrame) / 1_000_000_000f);
        previousFrame = now;

        for (Row<T> row : rows.values()) row.active = false;

        List<T> latest = entries();
        for (T entry : latest) {
            Row<T> row = rows.computeIfAbsent(key(entry), k -> new Row<>());
            row.entry = entry;
            row.active = true;
        }

        boolean preview = mc.currentScreen instanceof ChatScreen;
        boolean visible = !latest.isEmpty() || (preview && options.show("Header"));
        visibility.run(visible ? 1 : 0, 220);
        visibility.update();

        for (Row<T> row : rows.values()) {
            row.animation.run(row.active ? 1 : 0, row.active ? 180 : 240);
            row.animation.update();
        }
        rows.values().removeIf(row -> !row.active && row.animation.value() < .01f);

        float appear = visibility.value();
        if (!preview && latest.isEmpty() && appear < 0.02f) { width = height = 0; return; }
        if (appear < .01f) { width = height = 0; return; }

        float padding = s(HudCompactStyle.PADDING);
        float headerH = options.header(scale);
        float body = 0;
        for (Row<T> row : rows.values()) body += s(HudCompactStyle.ROW) * row.animation.value();

        float bodyPadding = HudCompactStyle.listPadding(options.show("Header"), scale);
        float desiredW = s(HudCompactStyle.MIN_WIDTH);
        float desiredH = headerH + (rows.isEmpty() ? s(1) : bodyPadding * 2) + body;

        if (animatedHeight == 0) animatedHeight = desiredH;
        animatedHeight = HudCompactStyle.approach(animatedHeight, desiredH, dt, 18);

        float w = desiredW;
        float h = Math.max(desiredH, animatedHeight);

        anchor(ctx, w, h, anchorX, anchorY);

        float drawY = y + s(2) * (1 - appear);
        int alpha = Math.round(appear * 255);

        HudCompactStyle.panel(ui, x, drawY, w, h, scale, alpha);
        if (options.show("Header")) {
            HudCompactStyle.header(ui, name(), headerIcon, x, drawY, w, scale, alpha);
        }

        float rowY = HudCompactStyle.listBodyTop(drawY, h, headerH, body);
        boolean showIcon = options.show("Icon");
        boolean showName = options.show("Name");
        boolean showLevel = options.show("Level");
        boolean showValue = options.show(valueOption());

        final float fontPx = 7f;
        final float levelPx = 7f;
        final float fontH = 7f;

        for (Row<T> row : rows.values()) {
            float reveal = row.animation.value();
            if (reveal <= .01f) continue;

            float cy = rowY + s(HudCompactStyle.ROW * .5f);
            float tx = x + padding + s(1.5f) * (1 - reveal);
            int rowAlpha = Math.round(alpha * reveal);

            if (showIcon) {
                float iconSize = s(7f);
                float iconX = tx;
                float iconY = cy - iconSize * 0.5f;

                if (nativeIcon(row.entry)) {
                    drawNativeIcon(ctx, row.entry, iconX, iconY, iconSize, rowAlpha);
                } else {
                    ui.circle(iconX + iconSize * 0.5f, cy, iconSize * 0.35f,
                            HudTheme.withAlpha(HudTheme.accent(), rowAlpha));
                }
                tx += iconSize + s(3f);
            }

            String value = showValue ? right(row.entry) : "";
            float valueW = textWidth(value, fontPx);

            String lvl = showLevel ? level(row.entry) : "";
            float lvlW = lvl.isEmpty() ? 0 : textWidth(lvl, levelPx) + s(2f);

            String labelText = left(row.entry);
            float nameMaxW = w - padding * 2 - (tx - x - padding) - valueW - lvlW - s(5);
            String label = showName ? fit(labelText, nameMaxW) : "";

            float textY = cy - fontH * 0.5f;

            ui.text(tx, textY, fontPx,
                    HudTheme.withAlpha(HudTheme.text(), rowAlpha), label);

            if (!lvl.isEmpty()) {
                ui.text(tx + textWidth(label, fontPx) + s(2f),
                        textY, levelPx,
                        HudTheme.withAlpha(HudTheme.muted(), rowAlpha), lvl);
            }

            if (!value.isEmpty()) {
                ui.rightText(x + w - padding, textY, fontPx,
                        HudTheme.withAlpha(0xFFB0B5C0, rowAlpha), value, false);
            }

            rowY += s(HudCompactStyle.ROW) * reveal;
        }

        width = w;
        height = h;
    }

    private static final class Row<T> {
        final HudAnimation animation = new HudAnimation();
        T entry;
        boolean active;
    }
}