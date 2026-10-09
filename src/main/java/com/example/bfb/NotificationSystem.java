package com.example.bfb;

import com.example.bfb.gui.GuiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Тосты в стиле Meteor/Celestial — всплывают справа сверху при toggle модуля. */
public final class NotificationSystem {
    private static final List<Notification> LIST = new ArrayList<>();
    private static final long DEFAULT_DURATION = 2400L;

    private NotificationSystem() {}

    public static void push(String text, int color) {
        push(text, color, DEFAULT_DURATION);
    }

    public static void push(String text, int color, long durationMs) {
        for (Notification n : LIST) {
            if (n.text.equals(text) && !n.dying()) {
                n.startMs = System.currentTimeMillis();
                n.color = color;
                return;
            }
        }
        LIST.add(new Notification(text, color, System.currentTimeMillis(), durationMs));
        while (LIST.size() > 8) LIST.remove(0);
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (LIST.isEmpty()) return;
        long now = System.currentTimeMillis();
        int screenW = context.getScaledWindowWidth();
        int baseY = 32;
        int rowH = SalFont.height() + 9;

        LIST.removeIf(n -> now - n.startMs > n.durationMs + 500);

        for (int i = 0; i < LIST.size(); i++) {
            Notification n = LIST.get(i);
            long age = now - n.startMs;
            float in = Math.min(1f, age / 240f);
            float out = 1f;
            long left = n.durationMs - age;
            if (left < 350) out = Math.max(0f, left / 350f);

            float alpha = Math.min(in, out);
            if (alpha <= 0.01f) continue;

            String text = n.text;
            int tw = SalFont.width(text) + 20;
            int th = rowH;

            int slide = (int) ((1f - in) * 40);
            int x = screenW - tw - 6 + slide;
            int y = baseY + i * (rowH + 3);

            int bgAlpha = (int) (0xE8 * alpha);
            int txtAlpha = (int) (0xFF * alpha);
            int acAlpha = (int) (0xFF * alpha);

            GuiDraw.shadow(context, x, y, tw, th, 4);
            GuiDraw.round(context, x, y, tw, th, 4, (bgAlpha << 24) | 0x0C0C14);
            GuiDraw.round(context, x, y, 3, th, 3, (acAlpha << 24) | (n.color & 0xFFFFFF));
            SalFont.draw(context, text, x + 9, y + (th - SalFont.height()) / 2, (txtAlpha << 24) | 0xF4F6FF);
        }
    }

    private static class Notification {
        String text;
        int color;
        long startMs;
        long durationMs;

        Notification(String text, int color, long startMs, long durationMs) {
            this.text = text;
            this.color = color;
            this.startMs = startMs;
            this.durationMs = durationMs;
        }

        boolean dying() {
            return System.currentTimeMillis() - startMs > durationMs - 400;
        }
    }
}