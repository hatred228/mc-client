package com.example.bfb.hud;

import java.util.ArrayList;
import java.util.List;

/** Тост-уведомления: toggle модулей, warning'и, события сервера. */
public final class Notifications {

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final long LIFETIME = 3000L;
    private static final long FADE = 400L;

    public static final class Entry {
        public final String text;
        public final int color;
        private final long created;

        Entry(String text, int color) {
            this.text = text;
            this.color = color;
            this.created = System.currentTimeMillis();
        }

        public float alpha() {
            long age = System.currentTimeMillis() - created;
            if (age < FADE) return age / (float) FADE;
            if (age > LIFETIME - FADE) {
                return Math.max(0f, (LIFETIME - age) / (float) FADE);
            }
            return 1f;
        }

        boolean done() { return System.currentTimeMillis() - created > LIFETIME; }
    }

    private Notifications() {}

    public static void push(String text, int color) {
        if (text == null || text.isEmpty()) return;
        ENTRIES.add(new Entry(text, color));
        while (ENTRIES.size() > 6) ENTRIES.remove(0);
    }

    public static void tick() {
        ENTRIES.removeIf(Entry::done);
    }

    public static List<Entry> active() { return ENTRIES; }
}