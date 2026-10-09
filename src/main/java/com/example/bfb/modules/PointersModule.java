package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.SalFont;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointersModule extends CheatModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", 28, 16, 120, 1));
    public final NumberSetting arrowSize = add(new NumberSetting("Arrow size", 10.0, 4.0, 24.0, 0.5));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 16, 128, 1));
    public final BooleanSetting fade = add(new BooleanSetting("Distance fade", false));
    public final BooleanSetting glow = add(new BooleanSetting("Glow", true));
    public final BooleanSetting smooth = add(new BooleanSetting("Smooth", true));
    public final ColorSetting color1 = add(new ColorSetting("Color", 0xFFFFE14D));
    public final BooleanSetting showName = add(new BooleanSetting("Show name", false));
    public final BooleanSetting showDistance = add(new BooleanSetting("Show distance", false));

    private static final Map<UUID, Float> ANGLES = new HashMap<>();
    private static final Map<UUID, Float> ALPHAS = new HashMap<>();

    public PointersModule() {
        super("Pointers", "Arrows to nearby players", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client, float tickDelta,
                            double radius, double range, boolean fade, boolean glow, boolean smooth,
                            double arrowSize, double thickness, int ignored) {
        PointersModule mod = com.example.bfb.ModuleManager.get(PointersModule.class);
        if (mod == null || !mod.isEnabled()) return;
        var player = client.player;
        if (player == null || client.world == null) return;

        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;
        float playerYaw = player.getYaw(tickDelta);
        int c1 = mod.color1.getRgb() & 0xFFFFFF;

        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == player || !p.isAlive()) continue;
            double dx = p.getX() - player.getX();
            double dz = p.getZ() - player.getZ();
            double dist = Math.hypot(dx, dz);
            if (dist > range || dist < 1.5) {
                ANGLES.remove(p.getUuid());
                ALPHAS.remove(p.getUuid());
                continue;
            }

            double target = Math.toDegrees(Math.atan2(dz, dx)) - 90.0 - playerYaw;
            Float prevAngle = ANGLES.get(p.getUuid());
            float current;
            if (prevAngle == null || !mod.smooth.get()) {
                current = (float) target;
            } else {
                float d = angleDelta(prevAngle, (float) target);
                // easing: сначала быстрее, потом медленнее
                current = prevAngle + d * 0.22f;
            }
            ANGLES.put(p.getUuid(), current);

            float targetAlpha = 1f;
            Float prevAlpha = ALPHAS.get(p.getUuid());
            float curAlpha = prevAlpha == null ? 0f : prevAlpha + (targetAlpha - prevAlpha) * 0.18f;
            ALPHAS.put(p.getUuid(), curAlpha);

            double a = Math.toRadians(current);
            int baseAlpha = fade ? (int) Math.max(140, 255 * (1 - dist / range)) : 255;
            int alpha = (int) (baseAlpha * curAlpha);
            if (alpha < 5) continue;

            double ax = cx + Math.sin(a) * radius;
            double ay = cy - Math.cos(a) * radius;

            double dirX = Math.sin(a);
            double dirY = -Math.cos(a);
            double perpX = Math.cos(a);
            double perpY = Math.sin(a);

            double size = arrowSize;
            double width = size * 0.65;

            // острие
            double tipX = ax + dirX * size;
            double tipY = ay + dirY * size;
            // задний центр — сдвинут внутрь
            double backX = ax - dirX * size * 0.45;
            double backY = ay - dirY * size * 0.45;
            // боковые точки основания
            double b1X = backX + perpX * width;
            double b1Y = backY + perpY * width;
            double b2X = backX - perpX * width;
            double b2Y = backY - perpY * width;
            // средняя точка сзади (форма «галочки» с провалом)
            double midX = ax - dirX * size * 0.1;
            double midY = ay - dirY * size * 0.1;

            int solidArgb = (alpha << 24) | c1;

            // glow
            if (glow) {
                int glowAlpha = Math.max(28, alpha / 4);
                double gs = size * 1.6;
                double gw = width * 1.7;
                double gTipX = ax + dirX * gs;
                double gTipY = ay + dirY * gs;
                double gBackX = ax - dirX * gs * 0.5;
                double gBackY = ay - dirY * gs * 0.5;
                double gB1X = gBackX + perpX * gw;
                double gB1Y = gBackY + perpY * gw;
                double gB2X = gBackX - perpX * gw;
                double gB2Y = gBackY - perpY * gw;
                fillTriangle(context, gTipX, gTipY, gB1X, gB1Y, gB2X, gB2Y, (glowAlpha << 24) | c1);
            }

            // основная стрелка — две половинки
            fillTriangle(context, tipX, tipY, b1X, b1Y, midX, midY, solidArgb);
            fillTriangle(context, tipX, tipY, b2X, b2Y, midX, midY, solidArgb);

            if (mod.showName.get() || mod.showDistance.get()) {
                StringBuilder sb = new StringBuilder();
                if (mod.showName.get()) sb.append(p.getGameProfile().name());
                if (mod.showDistance.get()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(String.format("%.0fм", dist));
                }
                String label = sb.toString();
                int lw = SalFont.width(label);
                int lx = (int) (ax - lw / 2);
                int ly = (int) (ay + size + 4);
                context.fill(lx - 3, ly - 2, lx + lw + 3, ly + SalFont.height() + 2, (alpha / 2 << 24));
                SalFont.draw(context, label, lx, ly, (alpha << 24) | 0xFFFFFFFF);
            }
        }
        if (ANGLES.size() > 128) ANGLES.clear();
        if (ALPHAS.size() > 128) ALPHAS.clear();
    }

    private static float angleDelta(float from, float to) {
        float d = (to - from) % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    /** Плавная заливка треугольника с anti-aliasing (4 суб-сэмпла на пиксель). */
    private static void fillTriangle(DrawContext c, double x1, double y1,
                                     double x2, double y2, double x3, double y3, int color) {
        int minX = (int) Math.floor(Math.min(x1, Math.min(x2, x3))) - 1;
        int maxX = (int) Math.ceil(Math.max(x1, Math.max(x2, x3))) + 1;
        int minY = (int) Math.floor(Math.min(y1, Math.min(y2, y3))) - 1;
        int maxY = (int) Math.ceil(Math.max(y1, Math.max(y2, y3))) + 1;
        if (maxX - minX > 100 || maxY - minY > 100) return;

        int baseAlpha = (color >>> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int inside = 0;
                for (int sy = 0; sy < 2; sy++) {
                    for (int sx = 0; sx < 2; sx++) {
                        double fx = px + 0.25 + sx * 0.5;
                        double fy = py + 0.25 + sy * 0.5;
                        if (pointInTriangle(fx, fy, x1, y1, x2, y2, x3, y3)) inside++;
                    }
                }
                if (inside == 0) continue;
                int a = baseAlpha * inside / 4;
                if (a <= 0) continue;
                c.fill(px, py, px + 1, py + 1, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    private static boolean pointInTriangle(double px, double py,
                                           double x1, double y1, double x2, double y2, double x3, double y3) {
        double d1 = sign(px, py, x1, y1, x2, y2);
        double d2 = sign(px, py, x2, y2, x3, y3);
        double d3 = sign(px, py, x3, y3, x1, y1);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static double sign(double px, double py, double x1, double y1, double x2, double y2) {
        return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
    }
}