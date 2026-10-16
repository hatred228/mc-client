package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class EspModule extends CheatModule {
    public final ModeSetting style = add(new ModeSetting("Style", "Box",
            "Box", "Filled", "Wireframe", "Glow", "Outline", "Chams",
            "Skeleton", "Health Bar", "Glass", "Gradient"));

    public final NumberSetting width = add(new NumberSetting("Width", 1.5f, 0.5f, 4.0f, 0.1f));
    public final BooleanSetting corners = add(new BooleanSetting("Corners", false));
    public final NumberSetting cornerSize = add(new NumberSetting("Corner size", 0.25f, 0.1f, 0.5f, 0.05f));

    public final BooleanSetting glow = add(new BooleanSetting("Glow", false));
    public final NumberSetting glowWidth = add(new NumberSetting("Glow width", 2.0f, 1.0f, 5.0f, 0.2f));
    public final NumberSetting glowAlpha = add(new NumberSetting("Glow alpha", 80, 20, 200, 5));

    public final ColorSetting fillColor = add(new ColorSetting("Fill color", 0x333DE1FF));
    public final NumberSetting fillAlpha = add(new NumberSetting("Fill alpha", 50, 10, 150, 5));

    public final ColorSetting color = add(new ColorSetting("Box color", 0x3DE1FF));
    public final ColorSetting colorTop = add(new ColorSetting("Gradient top", 0xFF5CA8));
    public final ColorSetting colorBottom = add(new ColorSetting("Gradient bottom", 0x3DE1FF));
    public final BooleanSetting health = add(new BooleanSetting("Health color", true));

    public final BooleanSetting pulse = add(new BooleanSetting("Pulse", false));
    public final NumberSetting pulseSpeed = add(new NumberSetting("Pulse speed", 1.0f, 0.2f, 3.0f, 0.1f));

    public final BooleanSetting fade = add(new BooleanSetting("Fade", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    public final NumberSetting range = add(new NumberSetting("Range", 64, 8, 128, 1));
    public final NumberSetting maxEntities = add(new NumberSetting("Max entities", 50, 10, 100, 5));

    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting items = add(new BooleanSetting("Items", true));
    public final BooleanSetting itemsThroughWalls = add(new BooleanSetting("Items through walls", false));

    // === HP Bar (2D полоска сбоку, только HP) ===
    public final BooleanSetting barEnabled = add(new BooleanSetting("HP Bar", false));
    public final ModeSetting barStyle = add(new ModeSetting("Bar style", "Solid", "Solid", "Gradient"));
    public final NumberSetting barWidth = add(new NumberSetting("Bar width", 1.0f, 0.5f, 2.0f, 0.05f));
    public final NumberSetting barHeight = add(new NumberSetting("Bar height", 1.0f, 0.5f, 3.0f, 0.1f));

    public EspModule() {
        super("ESP", "Box, wireframe, glow and more", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null) return;

        double reach = range.get();
        double reachSq = reach * reach;
        Camera view = client.gameRenderer.getCamera();
        Box area = player.getBoundingBox().expand(reach);

        float tickF = client.getRenderTickCounter().getTickProgress(false);
        long time = System.currentTimeMillis();
        float pulseMult = pulse.get() ? 0.7f + 0.3f * (float) Math.sin(time / 300.0 * pulseSpeed.get()) : 1.0f;

        int drawn = 0;
        int max = maxEntities.getInt();

        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, area, candidate ->
                CombatUtil.accept(candidate, player, players.get(), mobs.get(), animals.get())
                        && (!throughWalls.get() || RenderUtil.inFront(view, candidate.getEntityPos()))
                        && candidate.squaredDistanceTo(player) <= reachSq)) {
            if (drawn++ >= max) break;

            int rgb = color.getRgb();
            if (health.get() && entity.getMaxHealth() > 0) {
                float hpRatio = entity.getHealth() / entity.getMaxHealth();
                rgb = healthColor(hpRatio);
            }

            Vec3d lerpedPos = entity.getLerpedPos(tickF);
            Vec3d posOffset = lerpedPos.subtract(entity.getEntityPos());
            Box bounds = entity.getBoundingBox().offset(posOffset);

            double dist = player.squaredDistanceTo(entity) <= reachSq ? player.distanceTo(entity) : reach;
            float alpha = fade.get() ? Math.max(0.4f, 1.0f - (float)(dist / reach)) : 1.0f;
            int argb = ((int)(alpha * 255) << 24) | (rgb & 0xFFFFFF);

            float w = width.getFloat() * pulseMult;

            if (corners.get()) {
                RenderUtil.corners(matrices, consumers, bounds, camera, argb, w);
            }

            if (style.is("Filled")) {
                int fillArgb = ((int)(fillAlpha.get() * alpha) << 24) | (fillColor.getRgb() & 0xFFFFFF);
                RenderUtil.fill(matrices, consumers, bounds, camera, fillArgb);
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Wireframe")) {
                RenderUtil.corners(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Glow")) {
                int alphaVal = (int)(glowAlpha.get() * alpha);
                int glowColor = (alphaVal << 24) | (rgb & 0xFFFFFF);
                RenderUtil.outline(matrices, consumers, bounds.expand(0.05), camera, glowColor, glowWidth.getFloat());
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Outline")) {
                int outColor = ((int)(120 * alpha) << 24) | 0x000000;
                RenderUtil.outline(matrices, consumers, bounds.expand(0.1), camera, outColor, glowWidth.getFloat() * 0.5f);
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Chams")) {
                int chamColor = (int)(180 * alpha) << 24 | 0x9C5CFF;
                RenderUtil.fill(matrices, consumers, bounds, camera, chamColor);
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Glass")) {
                int glassAlpha = (int)(60 * alpha);
                int glassColor = (glassAlpha << 24) | 0xFFFFFF;
                RenderUtil.fill(matrices, consumers, bounds, camera, glassColor);
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Gradient")) {
                gradientBox(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Skeleton")) {
                drawSkeleton(matrices, consumers, camera, entity, bounds, argb, w, pulseMult, tickF);
            } else if (style.is("Health Bar")) {
                drawHealthBar(matrices, consumers, camera, entity, bounds, argb, w);
            } else {
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            }
        }

        if (items.get()) {
            int itemDrawn = 0;
            int itemMax = maxEntities.getInt() / 2;
            int itemRgb = color.getRgb();
            Box itemArea = player.getBoundingBox().expand(Math.min(reach, 24));

            for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class, itemArea, candidate ->
                    (itemsThroughWalls.get() || RenderUtil.inFront(view, candidate.getEntityPos()))
                            && candidate.squaredDistanceTo(player) <= reachSq)) {
                if (itemDrawn++ >= itemMax) break;

                float alpha = fade.get() ? Math.max(0.5f, 1.0f - (float)(player.distanceTo(item) / reach)) : 1.0f;
                int itemArgb = ((int)(alpha * 255) << 24) | (itemRgb & 0xFFFFFF);

                Vec3d lerpedPos = item.getLerpedPos(tickF);
                Vec3d posOffset = lerpedPos.subtract(item.getEntityPos());
                Box itemBox = item.getBoundingBox().offset(posOffset);
                RenderUtil.outline(matrices, consumers, itemBox, camera, itemArgb, 1.0f * pulseMult);
            }
        }
    }

    private void drawSkeleton(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                              LivingEntity entity, Box bounds, int argb, float w, float pulseMult, float tickF) {
        Vec3d lerpedPos = entity.getLerpedPos(tickF);
        float height = entity.getHeight();

        Vec3d head = lerpedPos.add(0, height - 0.25, 0);
        RenderUtil.tracer(matrices, consumers, lerpedPos.add(0, height * 0.8, 0), head, camera, argb, w);

        float armY = (float)(lerpedPos.y + height * 0.65);
        float armSpread = 0.25f;
        RenderUtil.tracer(matrices, consumers, head.add(-0.05, 0, 0), new Vec3d(lerpedPos.x - armSpread, armY, lerpedPos.z), camera, argb, w);
        RenderUtil.tracer(matrices, consumers, head.add(0.05, 0, 0), new Vec3d(lerpedPos.x + armSpread, armY, lerpedPos.z), camera, argb, w);

        Vec3d chest = lerpedPos.add(0, height * 0.6, 0);
        RenderUtil.tracer(matrices, consumers, head.add(0, -0.15, 0), chest, camera, argb, w);

        float legSpread = 0.15f;
        RenderUtil.tracer(matrices, consumers, chest.add(-0.05, 0, 0), new Vec3d(lerpedPos.x - legSpread, lerpedPos.y, lerpedPos.z), camera, argb, w);
        RenderUtil.tracer(matrices, consumers, chest.add(0.05, 0, 0), new Vec3d(lerpedPos.x + legSpread, lerpedPos.y, lerpedPos.z), camera, argb, w);
    }

    private void drawHealthBar(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                               LivingEntity entity, Box bounds, int argb, float w) {
        float hpRatio = Math.max(0f, Math.min(1f, entity.getHealth() / entity.getMaxHealth()));
        int barWidth = (int)(bounds.maxX - bounds.minX + 2);
        int barX = (int)bounds.minX - barWidth / 2;
        int barY = (int)bounds.maxY + 1;

        int bgAlpha = (int)(180 * (argb >>> 24) / 255.0);
        int bgRgb = 0x101010;

        RenderUtil.tracer(matrices, consumers,
                new Vec3d(barX, barY, camera.z),
                new Vec3d(barX + barWidth, barY, camera.z),
                camera, (bgAlpha << 24) | bgRgb, w);

        int filled = (int)(barWidth * hpRatio);
        int healthColor = healthColor(hpRatio);
        RenderUtil.tracer(matrices, consumers,
                new Vec3d(barX, barY, camera.z),
                new Vec3d(barX + filled, barY, camera.z),
                camera, ((argb >>> 24) << 24) | healthColor, w);
    }

    private void gradientBox(MatrixStack m, VertexConsumerProvider c, Box b, Vec3d cam, int argb, float w) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ;
        double x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;

        int topColor = colorTop.getRgb() & 0xFFFFFF;
        int bottomColor = colorBottom.getRgb() & 0xFFFFFF;

        RenderUtil.tracer(m, c, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), cam, (argb & 0xFF000000) | topColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), cam, (argb & 0xFF000000) | topColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), cam, (argb & 0xFF000000) | topColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), cam, (argb & 0xFF000000) | topColor, w);

        RenderUtil.tracer(m, c, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), cam, (argb & 0xFF000000) | bottomColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), cam, (argb & 0xFF000000) | bottomColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), cam, (argb & 0xFF000000) | bottomColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), cam, (argb & 0xFF000000) | bottomColor, w);

        double ym = (y1 + y2) * 0.5;
        int midColor = lerpArgb(bottomColor, topColor, 0.5f);
        vertical(m, c, cam, x1, z1, y1, ym, y2, bottomColor, midColor, topColor, w);
        vertical(m, c, cam, x2, z1, y1, ym, y2, bottomColor, midColor, topColor, w);
        vertical(m, c, cam, x2, z2, y1, ym, y2, bottomColor, midColor, topColor, w);
        vertical(m, c, cam, x1, z2, y1, ym, y2, bottomColor, midColor, topColor, w);
    }

    private static void vertical(MatrixStack m, VertexConsumerProvider c, Vec3d cam,
                                 double x, double z, double yLow, double yMid, double yHigh,
                                 int bottomColor, int midColor, int topColor, float w) {
        RenderUtil.tracer(m, c, new Vec3d(x, yLow, z), new Vec3d(x, yMid, z), cam, (0xFF << 24) | bottomColor, w);
        RenderUtil.tracer(m, c, new Vec3d(x, yMid, z), new Vec3d(x, yHigh, z), cam, (0xFF << 24) | topColor, w);
    }

    private static int lerpArgb(int bottom, int top, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ba = (bottom >>> 24) & 0xFF, br = (bottom >> 16) & 0xFF, bg = (bottom >> 8) & 0xFF, bb = bottom & 0xFF;
        int ta = (top >>> 24) & 0xFF, tr = (top >> 16) & 0xFF, tg = (top >> 8) & 0xFF, tb = top & 0xFF;
        int ra = (int)(ba + (ta - ba) * t);
        int rr = (int)(br + (tr - br) * t);
        int rg = (int)(bg + (tg - bg) * t);
        int rb = (int)(bb + (tb - bb) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static int healthColor(float ratio) {
        int red = (int)((1f - ratio) * 255);
        int green = (int)(ratio * 255);
        return (red << 16) | (green << 8);
    }
}