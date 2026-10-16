package com.example.bfb.hud;

import com.example.bfb.BfbFont;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class NotificationManager {
    private static final List<Notification> active = new ArrayList<>();
    private static final Deque<Notification> queue = new ArrayDeque<>();
    private static final int MAX_VISIBLE = 4;

    public static void push(String title, String text, Type type) {
        queue.add(new Notification(title, text, type));
        flush();
    }

    private static void flush() {
        while (active.size() < MAX_VISIBLE && !queue.isEmpty()) {
            active.add(queue.poll());
        }
    }

    public static void render(DrawContext ctx, int screenW, int screenH) {
        active.removeIf(n -> n.isDone());
        flush();

        int ry = screenH - 10;
        for (int i = active.size() - 1; i >= 0; i--) {
            Notification n = active.get(i);
            n.render(ctx, screenW, ry);
            ry -= n.getHeight() + 4;
        }
    }

    public enum Type {
        INFO(0xFF5599FF),
        SUCCESS(0xFF55FF99),
        WARNING(0xFFFFDD55),
        ERROR(0xFFFF5555);

        final int color;
        Type(int c) { this.color = c; }
    }

    private static class Notification {
        private final String title;
        private final String text;
        private final Type type;
        private final long created;
        private static final long LIFE = 3500L;
        private static final long FADE = 400L;
        private static final int W = 180;
        private static final int H = 32;

        Notification(String title, String text, Type type) {
            this.title = title;
            this.text = text;
            this.type = type;
            this.created = System.currentTimeMillis();
        }

        boolean isDone() { return System.currentTimeMillis() - created > LIFE; }
        int getHeight() { return H; }

        void render(DrawContext ctx, int screenW, int bottomY) {
            long age = System.currentTimeMillis() - created;
            float alpha = 1f;
            if (age < FADE) alpha = age / (float) FADE;
            else if (age > LIFE - FADE) alpha = (LIFE - age) / (float) FADE;
            alpha = Math.max(0f, Math.min(1f, alpha));

            int a = (int)(alpha * 0xCC);
            int x = screenW - W - 6;
            int y = bottomY - H;

            ctx.fill(x, y, x + W, y + H, (a << 24) | 0x0A0A14);
            ctx.fill(x, y, x + 3, y + H, (0xFF << 24) | (type.color & 0xFFFFFF));

            int ta = (int)(alpha * 255);
            BfbFont.draw(ctx, title, x + 7, y + 4, (ta << 24) | (type.color & 0xFFFFFF), true);
            BfbFont.draw(ctx, text, x + 7, y + 16, (ta << 24) | 0xCCCCCC, false);

            // progress bar
            float progress = 1f - (float) age / LIFE;
            int barW = (int)((W - 6) * progress);
            ctx.fill(x + 3, y + H - 2, x + 3 + barW, y + H, (ta << 24) | (type.color & 0xFFFFFF));
        }
    }
}
