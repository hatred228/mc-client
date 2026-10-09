package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.gui.CircleTextureCache;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class RadarModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "TopRight",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));

    public final NumberSetting size = add(new NumberSetting("Size", 100, 60, 200, 4));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 16, 128, 4));
    public final NumberSetting maxEnt = add(new NumberSetting("Max entities", 30, 10, 60, 5));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 3, 1, 6, 1));

    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgInner = add(new ColorSetting("Background inner", 0xFF1A0E2E));
    public final ColorSetting bgOuter = add(new ColorSetting("Background outer", 0xFF0A0510));

    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", true));
    public final BooleanSetting items = add(new BooleanSetting("Items", false));
    public final BooleanSetting heightColor = add(new BooleanSetting("Height color", true));

    public RadarModule() {
        super("Radar", "Round radar", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client, float tickDelta) {
        RadarModule mod = com.example.bfb.ModuleManager.get(RadarModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;
        var player = client.player;
        var world = client.world;
        if (world == null) return;

        int size = mod.size.getInt();
        int range = mod.range.getInt();
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        int defX, defY;
        switch (mod.position.get()) {
            case "TopLeft" -> { defX = 6; defY = 6; }
            case "BottomLeft" -> { defX = 6; defY = sh - size - 6; }
            case "BottomRight" -> { defX = sw - size - 6; defY = sh - size - 6; }
            default -> { defX = sw - size - 6; defY = 6; }
        }

        int x = HudEditor.drawX("radar", defX, defY);
        int y = HudEditor.drawY("radar", defX, defY);
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - mod.edgeWidth.getInt() - 1;

        int bgInner = mod.bgInner.getRgb() & 0xFFFFFF;
        int bgOuter = mod.bgOuter.getRgb() & 0xFFFFFF;

        // === фон: solid outer + radial fade inner ===
        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                CircleTextureCache.solid(size),
                x, y, 0f, 0f, size, size, size, size, size, size,
                0xFF000000 | bgOuter);
        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                CircleTextureCache.radialFade(size),
                x, y, 0f, 0f, size, size, size, size, size, size,
                0xFF000000 | bgInner);

        // === точки сущностей ===
        double yawRad = mod.rotate.get() ? Math.toRadians(player.getYaw(tickDelta)) : 0;
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double rangeSq = (double) range * range;
        int limit = mod.maxEnt.getInt();

        int drawn = 0;
        for (Entity entity : world.getEntities()) {
            if (entity == player || !entity.isAlive()) continue;
            int baseColor;
            if (entity instanceof PlayerEntity) {
                if (!mod.players.get()) continue;
                baseColor = 0xFFFF3B3B;
            } else if (entity instanceof AnimalEntity) {
                if (!mod.animals.get()) continue;
                baseColor = 0xFF3BFF6B;
            } else if (entity instanceof HostileEntity) {
                if (!mod.mobs.get()) continue;
                baseColor = 0xFFFF8B20;
            } else if (entity instanceof ItemEntity) {
                if (!mod.items.get()) continue;
                baseColor = 0xFFFFE040;
            } else continue;

            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq > rangeSq) continue;

            double rx, rz;
            if (mod.rotate.get()) {
                rx = dx * cos - dz * sin;
                rz = dx * sin + dz * cos;
            } else {
                rx = dx;
                rz = dz;
            }
            int px = cx + (int) Math.round(rx / range * r);
            int py = cy + (int) Math.round(rz / range * r);

            int ddx = px - cx;
            int ddy = py - cy;
            if (ddx * ddx + ddy * ddy > r * r) continue;

            int color = baseColor;
            if (mod.heightColor.get()) {
                double dy = entity.getY() - player.getY();
                double k = Math.max(-1.0, Math.min(1.0, dy / 16.0));
                int shade = (int) (170 + 85 * (1.0 - k));
                int rr = ((baseColor >> 16) & 0xFF) * shade / 255;
                int gg = ((baseColor >> 8) & 0xFF) * shade / 255;
                int bb = (baseColor & 0xFF) * shade / 255;
                color = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
            context.fill(px, py, px + 2, py + 2, color);
            if (++drawn >= limit) break;
        }

        // === центр — игрок ===
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF00E0FF);

        // === кольцо-контур, разделённое пополам между двумя цветами ===
        int edgeW = Math.max(1, mod.edgeWidth.getInt());
        int c1 = mod.edgeColor1.getRgb() & 0xFFFFFF;
        int c2 = mod.edgeColor2.getRgb() & 0xFFFFFF;
        Identifier ring = CircleTextureCache.outline(size, edgeW);

        // верхняя половина — c1
        context.enableScissor(x, y, x + size, cy);
        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                ring, x, y, 0f, 0f, size, size, size, size, size, size,
                0xFF000000 | c1);
        context.disableScissor();

        // нижняя половина — c2
        context.enableScissor(x, cy, x + size, y + size);
        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                ring, x, y, 0f, 0f, size, size, size, size, size, size,
                0xFF000000 | c2);
        context.disableScissor();

        HudEditor.outline(context, "radar", x, y, size, size);
    }
}