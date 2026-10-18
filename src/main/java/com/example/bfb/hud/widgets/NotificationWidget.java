package com.example.bfb.hud.widgets;

import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.hud.Notifications;
import com.example.bfb.modules.VisualsModule;
import com.example.bfb.render.ui.UiDrawList;

public class NotificationWidget extends HudWidget {

    public NotificationWidget() {
        super("Notifications", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        Notifications.tick();
        var list = Notifications.active();
        if (list.isEmpty()) { width = height = 0; return; }

        VisualsModule vm = VisualsModule.get();
        if (vm != null && !vm.notifications.bool("Enabled")) { width = height = 0; return; }

        String position = vm != null ? vm.notifications.mode("Position") : "TopRight";
        String anim = vm != null ? vm.notifications.mode("Animation") : "Slide";
        float animSpeed = vm != null ? vm.notifications.num("Anim speed") : 1f;
        float gapBase = vm != null ? vm.notifications.num("Stack gap") : 3f;
        float widthPx = vm != null ? vm.notifications.num("Width") : 170f;
        boolean accentBar = vm == null || vm.notifications.bool("Accent bar");
        boolean slideFromSide = vm == null || vm.notifications.bool("Slide from side");

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        float rowH = s(14f);
        float gap = s(gapBase);
        float w = s(widthPx);

        boolean rightSide = position.endsWith("Right");
        boolean bottomSide = position.startsWith("Bottom");

        float baseX = rightSide ? (sw - w - s(4f)) : s(4f);
        float startY = bottomSide ? (sh - s(4f) - list.size() * (rowH + gap)) : s(4f);

        float ry = startY;
        for (var e : list) {
            float a = e.alpha();
            if (a <= 0.01f) { ry += rowH + gap; continue; }
            int alpha = Math.round(a * 235);

            float offsetX = 0f;
            float offsetY = 0f;
            float localScale = 1f;
            float localAlpha = 1f;

            switch (anim) {
                case "Slide" -> {
                    float in = Math.min(1f, (1f - a) * 6f * animSpeed);
                    float slide = (1f - Math.min(1f, in + (1f - a) * 4f * animSpeed)) * 30f;
                    if (slideFromSide) {
                        offsetX = rightSide ? s(slide) : -s(slide);
                    } else {
                        offsetY = bottomSide ? -s(slide) : s(slide);
                    }
                    localAlpha = a;
                }
                case "Fade" -> localAlpha = a;
                case "Pop" -> {
                    float in = Math.min(1f, (1f - a) * 8f * animSpeed);
                    localScale = 0.6f + 0.4f * in;
                    localAlpha = Math.min(1f, in * 1.4f);
                }
                case "None" -> localAlpha = a > 0.5f ? 1f : a * 2f;
            }

            int baseAlpha = (int) (alpha * localAlpha);
            if (baseAlpha <= 3) { ry += rowH + gap; continue; }

            float rowX = baseX + offsetX;
            float rowY = ry + offsetY;
            float rowW = w * localScale;

            ui.roundedRect(rowX, rowY, rowW, rowH * localScale, s(3f),
                    HudTheme.withAlpha(HudTheme.background(), baseAlpha));

            if (accentBar) {
                float barW = s(2.5f) * localScale;
                ui.rect(rowX, rowY, barW, rowH * localScale,
                        HudTheme.withAlpha(e.color, baseAlpha));
            }

            float textX = rowX + (accentBar ? s(8f) : s(6f));
            ui.text(textX, rowY + s(3.5f), 7f,
                    HudTheme.withAlpha(0xFFF4F6FF, baseAlpha), e.text);

            ry += rowH + gap;
        }

        width = w;
        height = ry - startY;
    }
}