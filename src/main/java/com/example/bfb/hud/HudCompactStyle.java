package com.example.bfb.hud;

import com.example.bfb.gui.UiFont;
import com.example.bfb.render.ui.UiDrawList;
import com.example.bfb.setting.Setting;

public final class HudCompactStyle {

    public static final float FONT = 8f;
    public static final float FONT_SMALL = 7f;
    public static final float ICON = 8f;
    public static final float HEADER = 14f;
    public static final float ROW = 11f;
    public static final float PADDING = 5f;
    public static final float MIN_WIDTH = 84f;

    private HudCompactStyle() {}

    public static float referenceScale(float vw, float vh, float userScale) {
        return Math.max(0.25f, Math.min(vw / 952.5f, vh / 280f)) * userScale;
    }

    public static float approach(float current, float target, float dt, float speed) {
        float safe = Math.max(0f, Math.min(dt, 0.1f));
        return current + (target - current) * (1f - (float) Math.exp(-speed * safe));
    }

    public static float textWidth(String text, float sizePx) {
        if (text == null || text.isEmpty()) return 0;
        return UiFont.forSize(sizePx).width(text);
    }

    public static void panel(UiDrawList ui, float x, float y, float w, float h,
                             float scale, int alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0) return;
        float radius = Math.min(4f * scale, Math.min(w, h) * 0.5f);
        float a = alpha / 255f;
        ui.opacity(a);
        ui.roundedRect(x, y, w, h, radius, HudTheme.background());
        if (w > radius * 2 + 4) {
            ui.rect(x + radius, y + 1, w - radius * 2, 1, 0x18FFFFFF);
        }
        ui.opacity(1f);
    }

    /** Заголовок — по центру, тонкий разделитель снизу. Без иконки, без точек. */
    public static void header(UiDrawList ui, String title, int glyph,
                              float x, float y, float w, float scale, int alpha) {
        if (w <= 0 || alpha <= 0) return;
        float a = alpha / 255f;
        ui.opacity(a);
        float cy = y + HEADER * scale * 0.5f;

        float titleW = textWidth(title, 8f) * scale;
        float titleX = x + (w - titleW) * 0.5f;
        ui.text(titleX, cy - 4f, 8f, HudTheme.text(), title);

        ui.rect(x + 0.5f * scale, y + HEADER * scale,
                w - 1 * scale, Math.max(0.5f, 0.3f * scale), 0x18FFFFFF);
        ui.opacity(1f);
    }

    public static void progress(UiDrawList ui, float x, float y, float w, float h,
                                float value, int alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0) return;
        ui.opacity(alpha / 255f);
        float radius = h * 0.5f;
        ui.roundedRect(x, y, w, h, radius, HudTheme.track());
        float clamped = Math.max(0f, Math.min(1f, value));
        float fillW = w * clamped;
        if (fillW > 0.5f) {
            ui.roundedRect(x, y, fillW, h, Math.min(radius, fillW * 0.5f), HudTheme.accent());
        }
        ui.opacity(1f);
    }

    public static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static float listPadding(boolean header, float scale) {
        return (header ? 3 : PADDING) * scale;
    }

    public static float listBodyTop(float y, float height, float header, float body) {
        return y + header + Math.max(0, height - header - body) * 0.5f;
    }

    public static final class Options extends Setting {
        private final String widgetName;
        private final java.util.List<Option> options = new java.util.ArrayList<>();

        public Options(String widgetName, String... fields) {
            super(widgetName);
            this.widgetName = widgetName;
            for (String f : fields) options.add(new Option(f, true));
        }

        public String widgetName() { return widgetName; }
        public java.util.List<Option> all() { return options; }

        public boolean show(String field) {
            for (Option o : options) if (o.name.equals(field)) return o.enabled;
            return false;
        }

        public float header(float scale) {
            return show("Header") ? HEADER * scale : 0f;
        }

        @Override public String serialize() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(options.get(i).enabled ? '1' : '0');
            }
            return sb.toString();
        }

        @Override public void deserialize(String raw) {
            if (raw == null || raw.isEmpty()) return;
            String[] parts = raw.split(",");
            for (int i = 0; i < options.size() && i < parts.length; i++) {
                options.get(i).enabled = "1".equals(parts[i]);
            }
        }
    }

    public static final class Option {
        public final String name;
        public boolean enabled;
        public Option(String name, boolean enabled) { this.name = name; this.enabled = enabled; }
        public void toggle() { enabled = !enabled; }
    }

    public static final class Menu {
        private Options options;
        private float x, y, w, h, scale = 1f;

        public boolean isOpen() { return options != null; }
        public void close() { options = null; }

        public void show(Options opts, float anchorX, float anchorY,
                         float scale, float viewportW, float viewportH) {
            this.options = opts;
            this.scale = scale;
            int rows = opts.all().size();
            float wMin = MIN_WIDTH * scale;
            this.w = Math.min(wMin, viewportW - 8 * scale);
            this.h = (HEADER + 4 + ROW * rows) * scale;
            this.x = Math.max(4 * scale, Math.min(anchorX, viewportW - w - 4 * scale));
            float top = anchorY - h + 9 * scale;
            this.y = Math.max(4 * scale,
                    Math.min(top < 4 * scale ? anchorY + HEADER * scale : top,
                            viewportH - h - 4 * scale));
        }

        public void render(UiDrawList ui) {
            if (!isOpen()) return;
            panel(ui, x, y, w, h, scale, 240);
            header(ui, "Settings", 0, x, y, w, scale, 255);

            for (int i = 0; i < options.all().size(); i++) {
                Option opt = options.all().get(i);
                float cy = y + (HEADER + 2 + ROW * (i + 0.5f)) * scale;
                ui.text(x + PADDING * scale, cy - 3.5f, 7f, HudTheme.text(), opt.name);

                float sw = 9 * scale;
                float sh = 5f * scale;
                float sx = x + w - 12 * scale;
                int trackColor = opt.enabled ? HudTheme.accent() : HudTheme.track();
                ui.roundedRect(sx, cy - sh * 0.5f, sw, sh, sh * 0.5f, trackColor);

                float knobR = 1.8f * scale;
                float knobX = opt.enabled ? sx + sw - knobR * 2 : sx + knobR * 2;
                ui.circle(knobX, cy, knobR, 0xFFFFFFFF);
            }
        }

        public boolean mouseClick(double mx, double my, int button, int action) {
            if (!isOpen() || action != 1) return false;
            if (!hit(mx, my, x, y, w, h)) { close(); return true; }
            if (button == 0) {
                int index = (int) ((my - y - (HEADER + 2) * scale) / (ROW * scale));
                if (my >= y + (HEADER + 2) * scale
                        && index >= 0 && index < options.all().size()) {
                    options.all().get(index).toggle();
                }
            }
            return true;
        }
    }
}