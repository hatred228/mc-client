package com.example.bfb.hud;

import com.example.bfb.ModuleManager;
import com.example.bfb.hud.widgets.ArraylistWidget;
import com.example.bfb.hud.widgets.ArmorWidget;
import com.example.bfb.hud.widgets.CooldownsWidget;
import com.example.bfb.hud.widgets.EspBarWidget;
import com.example.bfb.hud.widgets.KeybindsWidget;
import com.example.bfb.hud.widgets.NametagsWidget;
import com.example.bfb.hud.widgets.NotificationWidget;
import com.example.bfb.hud.widgets.PotionsWidget;
import com.example.bfb.hud.widgets.StaffDetectorWidget;
import com.example.bfb.hud.widgets.TargetInfoWidget;
import com.example.bfb.hud.widgets.TracersWidget;
import com.example.bfb.hud.widgets.WatermarkWidget;
import com.example.bfb.modules.CustomVisualsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class HudIntegration {

    private static HudIntegration instance;

    private final WidgetManager widgetManager = new WidgetManager();

    private final HudCompactStyle.Options potionOptions =
            new HudCompactStyle.Options("Potions", "Header", "Icon", "Name", "Level", "Duration");
    private final HudCompactStyle.Options cooldownOptions =
            new HudCompactStyle.Options("Cooldowns", "Header", "Icon", "Name", "Duration");
    private final HudCompactStyle.Options keybindOptions =
            new HudCompactStyle.Options("Keybinds", "Header", "Icon", "Name", "Key");

    private final ArraylistWidget arraylist = new ArraylistWidget();
    private final NotificationWidget notifications = new NotificationWidget();
    private final StaffDetectorWidget staffDetector = new StaffDetectorWidget();
    private final TracersWidget tracers = new TracersWidget();
    private final NametagsWidget nametags = new NametagsWidget();
    private final WatermarkWidget watermark = new WatermarkWidget();
    private final PotionsWidget potions = new PotionsWidget(potionOptions);
    private final CooldownsWidget cooldowns = new CooldownsWidget(cooldownOptions);
    private final KeybindsWidget keybinds = new KeybindsWidget(keybindOptions);
    private final TargetInfoWidget targetInfo = new TargetInfoWidget();
    private final ArmorWidget armor = new ArmorWidget();
    private final EspBarWidget espBar = new EspBarWidget();

    private boolean enabled = true;

    public HudIntegration() {
        instance = this;
        widgetManager.register(arraylist);
        widgetManager.register(notifications);
        widgetManager.register(staffDetector);
        widgetManager.register(tracers);
        widgetManager.register(nametags);
        widgetManager.register(watermark);
        widgetManager.register(potions);
        widgetManager.register(cooldowns);
        widgetManager.register(keybinds);
        widgetManager.register(targetInfo);
        widgetManager.register(armor);
        widgetManager.register(espBar);
    }

    public static HudIntegration get() { return instance; }
    public WidgetManager widgetManager() { return widgetManager; }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean value) { this.enabled = value; }

    public HudWidget find(String name) {
        for (HudWidget w : widgetManager.widgets()) {
            if (w.name().equalsIgnoreCase(name)) return w;
        }
        return null;
    }

    public void toggle(String name) {
        HudWidget w = find(name);
        if (w != null) w.toggle();
    }

    public void render(DrawContext ctx) {
        if (!enabled) return;
        drawCrosshair(ctx);
        widgetManager.render(ctx);
    }

    public boolean mouseButton(double x, double y, int button, int action) {
        if (!enabled) return false;
        return widgetManager.mouseButton(x, y, button, action);
    }

    public void setUserScale(float scale) {
        widgetManager.setUserScale(scale);
    }

    // ============================================================
    // CROSSHAIR
    // ============================================================

    private static void drawCrosshair(DrawContext ctx) {
        CustomVisualsModule mod = ModuleManager.get(CustomVisualsModule.class);
        if (mod == null || !mod.isEnabled() || !mod.customCrosshair.get()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.currentScreen != null) return;

        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int color = 0xFF000000 | mod.crosshairColor.getRgb();
        int outlineColor = mod.crosshairOutline.get()
                ? (0xFF000000 | mod.crosshairOutlineColor.getRgb())
                : 0;

        float[] state = CustomVisualsModule.crosshairState();
        float extraGap = state[0];

        int length = mod.crosshairLength.getInt();
        int gap = Math.round(mod.crosshairGap.getInt() + extraGap);
        int t = mod.crosshairWidth.getInt();
        int half = t / 2;
        int half2 = t - half;

        String style = mod.crosshairStyle.get();

        switch (style) {
            case "Dot" -> {
                int ds = mod.crosshairDotSize.getInt();
                int dh = ds / 2;
                int dh2 = ds - dh;
                drawCxRect(ctx, cx - dh, cy - dh, cx + dh2, cy + dh2, color, outlineColor);
            }
            case "Circle" -> drawCxCircle(ctx, cx, cy, length + gap, t, color, outlineColor);
            case "T-Shape" -> {
                drawCxRect(ctx, cx - gap - length, cy - half, cx - gap, cy + half2, color, outlineColor);
                drawCxRect(ctx, cx + gap, cy - half, cx + gap + length, cy + half2, color, outlineColor);
                drawCxRect(ctx, cx - half, cy + gap, cx + half2, cy + gap + length, color, outlineColor);
            }
            case "X-Shape" -> {
                drawCxDiagonal(ctx, cx, cy, gap, length, t, color, 1, 1);
                drawCxDiagonal(ctx, cx, cy, gap, length, t, color, 1, -1);
                drawCxDiagonal(ctx, cx, cy, gap, length, t, color, -1, 1);
                drawCxDiagonal(ctx, cx, cy, gap, length, t, color, -1, -1);
            }
            case "Arrow" -> drawCxTriangle(ctx, cx, cy - gap - length, length, color);
            default -> {
                drawCxRect(ctx, cx - gap - length, cy - half, cx - gap, cy + half2, color, outlineColor);
                drawCxRect(ctx, cx + gap, cy - half, cx + gap + length, cy + half2, color, outlineColor);
                drawCxRect(ctx, cx - half, cy - gap - length, cx + half2, cy - gap, color, outlineColor);
                drawCxRect(ctx, cx - half, cy + gap, cx + half2, cy + gap + length, color, outlineColor);
                if (mod.crosshairDot.get()) {
                    int ds = mod.crosshairDotSize.getInt();
                    int dh = ds / 2;
                    int dh2 = ds - dh;
                    drawCxRect(ctx, cx - dh, cy - dh, cx + dh2, cy + dh2, color, outlineColor);
                }
            }
        }
    }

    private static void drawCxRect(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int outlineColor) {
        if (outlineColor != 0) {
            ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, outlineColor);
        }
        ctx.fill(x1, y1, x2, y2, color);
    }

    private static void drawCxCircle(DrawContext ctx, int cx, int cy, int radius, int thickness, int color, int outlineColor) {
        int steps = Math.max(48, radius * 6);
        int th = Math.max(1, thickness);
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            if (outlineColor != 0) {
                ctx.fill(px - th / 2 - 1, py - th / 2 - 1, px + th / 2 + 2, py + th / 2 + 2, outlineColor);
            }
            ctx.fill(px - th / 2, py - th / 2, px + th / 2 + 1, py + th / 2 + 1, color);
        }
    }

    private static void drawCxDiagonal(DrawContext ctx, int cx, int cy, int gap, int length, int thickness, int color, int dx, int dy) {
        for (int i = gap; i <= length; i++) {
            int px = cx + i * dx;
            int py = cy + i * dy;
            ctx.fill(px - thickness / 2, py - thickness / 2,
                    px + thickness / 2 + 1, py + thickness / 2 + 1, color);
        }
    }

    private static void drawCxTriangle(DrawContext ctx, int apexX, int apexY, int size, int color) {
        for (int i = 0; i < size; i++) {
            ctx.fill(apexX - i, apexY + i, apexX + i + 1, apexY + i + 1, color);
        }
    }
}