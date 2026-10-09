package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.ModuleManager;
import com.example.bfb.SalFont;
import com.example.bfb.gui.GuiDraw;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WatermarkModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "TopLeft",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 5, 0, 12, 1));
    public final BooleanSetting showFps = add(new BooleanSetting("Show FPS", true));
    public final BooleanSetting showPing = add(new BooleanSetting("Show ping", true));
    public final BooleanSetting showUid = add(new BooleanSetting("Show UID", true));
    public final BooleanSetting showTime = add(new BooleanSetting("Show time", true));
    public final BooleanSetting showName = add(new BooleanSetting("Show player", true));
    public final BooleanSetting gradientText = add(new BooleanSetting("Gradient text", true));
    public final ColorSetting accentColor1 = add(new ColorSetting("Accent 1", 0x9C5CFF));
    public final ColorSetting accentColor2 = add(new ColorSetting("Accent 2", 0xFF5CA8));
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0xE0100A14));
    public final ColorSetting textColor = add(new ColorSetting("Text", 0xFFFFFF));

    private static final int UID = 10000 + new Random().nextInt(90000);

    public WatermarkModule() {
        super("Watermark", "Celestial-style watermark", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }

    private record Segment(String text, int color) {}

    public static void draw(DrawContext context, MinecraftClient client) {
        WatermarkModule mod = ModuleManager.get(WatermarkModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;

        List<Segment> segments = new ArrayList<>();
        segments.add(new Segment("Library Utils", mod.accentColor1.getRgb() & 0xFFFFFF));
        if (mod.showName.get()) segments.add(new Segment(client.player.getGameProfile().name(), 0xF4F6FF));
        if (mod.showFps.get()) segments.add(new Segment(client.getCurrentFps() + " fps", 0x4CC3FF));
        if (mod.showPing.get()) {
            int ping = 0;
            if (client.getNetworkHandler() != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                ping = entry == null ? 0 : entry.getLatency();
            }
            int color = ping < 80 ? 0x55FF55 : ping < 150 ? 0xFFAA00 : 0xFF5050;
            segments.add(new Segment(ping + " ms", color));
        }
        if (mod.showUid.get()) segments.add(new Segment("UID " + UID, 0xFF8ADF));
        if (mod.showTime.get()) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            segments.add(new Segment(time, 0xB0B8D0));
        }

        String sep = "  ";
        int sepW = SalFont.width(sep);

        int totalW = 20;
        for (int i = 0; i < segments.size(); i++) {
            totalW += SalFont.width(segments.get(i).text);
            if (i < segments.size() - 1) totalW += sepW;
        }
        int boxW = totalW;
        int boxH = SalFont.height() + 8;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        float scale = mod.scale.getFloat();
        int totalScaledW = (int) (boxW * scale);
        int totalScaledH = (int) (boxH * scale);

        int defX, defY;
        switch (mod.position.get()) {
            case "TopRight" -> { defX = sw - totalScaledW - 8; defY = 8; }
            case "BottomLeft" -> { defX = 8; defY = sh - totalScaledH - 8; }
            case "BottomRight" -> { defX = sw - totalScaledW - 8; defY = sh - totalScaledH - 8; }
            default -> { defX = 8; defY = 8; }
        }
        int baseX = HudEditor.getX("watermark", defX);
        int baseY = HudEditor.getY("watermark", defY);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);

        int r = mod.radius.getInt();
        int bg = mod.bgColor.getRgb();

        GuiDraw.shadow(context, 0, 0, boxW, boxH, r);
        GuiDraw.round(context, 0, 0, boxW, boxH, r, bg);

        // верхняя градиентная кромка
        int c1 = mod.accentColor1.getRgb() & 0xFFFFFF;
        int c2 = mod.accentColor2.getRgb() & 0xFFFFFF;
        GuiDraw.roundTop(context, 0, 0, boxW, 2, r, 0xFF000000 | c1);
        context.fillGradient(0, 0, boxW / 2, 2, 0xFF000000 | c1, 0xFF000000 | c2);
        context.fillGradient(boxW / 2, 0, boxW, 2, 0xFF000000 | c2, 0xFF000000 | c1);

        // левая акцент-полоса
        GuiDraw.round(context, 0, 0, 3, boxH, r, 0xFF000000 | c1);

        // текст
        int x = 10;
        int y = (boxH - SalFont.height()) / 2;
        int idx = 0;
        int total = segments.size();
        for (Segment s : segments) {
            int color = s.color;
            if (mod.gradientText.get() && total > 1) {
                float t = idx / (float) (total - 1);
                color = lerpColor(s.color, blendWith(c1, c2, t), 0.35f);
            }
            SalFont.draw(context, s.text, x + 1, y + 1, 0x50000000);
            SalFont.draw(context, s.text, x, y, 0xFF000000 | color);
            x += SalFont.width(s.text);
            if (idx < total - 1) x += sepW;
            idx++;
        }

        matrices.popMatrix();
        HudEditor.outline(context, "watermark", baseX, baseY, totalScaledW, totalScaledH);
    }

    private static int blendWith(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (((int) (ar + (br - ar) * t)) << 16)
                | (((int) (ag + (bg - ag) * t)) << 8)
                | (int) (ab + (bb - ab) * t);
    }
}