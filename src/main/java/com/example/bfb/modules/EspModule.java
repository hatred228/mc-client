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
            "Box", "Filled", "Corners", "Wireframe", "2D", "Bars", "Glow",
            "Gradient", "CornerPulse", "Double"));
    public final BooleanSetting tracers = add(new BooleanSetting("Tracers", true));
    public final BooleanSetting tracerGradient = add(new BooleanSetting("Tracer gradient", true));
    public final ModeSetting origin = add(new ModeSetting("Tracer from", "Crosshair", "Top", "Crosshair", "Bottom"));
    public final BooleanSetting items = add(new BooleanSetting("Items 3D", true));
    public final BooleanSetting itemTags = add(new BooleanSetting("Items tags", true));
    public final BooleanSetting gear = add(new BooleanSetting("Gear only", false));
    public final NumberSetting itemSize = add(new NumberSetting("Item box size", 0.10, 0.00, 0.20, 0.02));
    public final NumberSetting itemTagSize = add(new NumberSetting("Item tag size", 0.70, 0.30, 1.50, 0.05));
    public final ColorSetting itemColor = add(new ColorSetting("Item color", 0xFFE14D));
    public final BooleanSetting health = add(new BooleanSetting("Health color", true));
    public final BooleanSetting fade = add(new BooleanSetting("Fade", true));
    public final BooleanSetting damageFlash = add(new BooleanSetting("Damage flash", true));
    public final BooleanSetting glow = add(new BooleanSetting("Glow", false));
    public final NumberSetting glowThickness = add(new NumberSetting("Glow thickness", 3.0, 1.0, 6.0, 0.5));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 1));
    public final NumberSetting maxEntities = add(new NumberSetting("Max entities", 30, 5, 60, 1));
    public final NumberSetting width = add(new NumberSetting("Width", 1.5, 1.0, 4.0, 0.5));
    public final NumberSetting cornerSize = add(new NumberSetting("Corner size", 0.22, 0.10, 0.40, 0.02));
    public final ColorSetting color = add(new ColorSetting("Color", 0x3DE1FF));
    public final ColorSetting colorTop = add(new ColorSetting("Gradient top", 0xFF5CA8));
    public final ColorSetting colorBottom = add(new ColorSetting("Gradient bottom", 0x3DE1FF));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    public EspModule() {
        super("ESP", "Box, fill, corners, wireframe, 2D, glow and tracers", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
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
        int drawn = 0;
        int max = maxEntities.getInt();
        boolean do2D = style.is("2D") || style.is("Bars");
        boolean doWireframe = style.is("Wireframe");
        boolean doGlow = style.is("Glow") || glow.get();
        float glowT = glowThickness.getFloat();

        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, area, candidate ->
                CombatUtil.accept(candidate, player, players.get(), mobs.get(), animals.get())
                        && candidate.squaredDistanceTo(player) <= reachSq
                        && RenderUtil.inFront(view, candidate.getEntityPos()))) {
            if (drawn++ >= max) break;
            if (do2D) continue;

            int rgb = color.getRgb();
            if (health.get() && entity.getMaxHealth() > 0) {
                float ratio = Math.max(0f, Math.min(1f, entity.getHealth() / entity.getMaxHealth()));
                int red = (int) ((1f - ratio) * 255);
                int green = (int) (ratio * 255);
                rgb = (red << 16) | (green << 8);
            }

            // damage flash — цель только что получила урон
            boolean flash = damageFlash.get() && entity.hurtTime > 0;
            if (flash) rgb = 0xFF3B30;

            var bounds = entity.getBoundingBox();
            double dist = Math.sqrt(entity.squaredDistanceTo(player));
            int a = fade.get() ? Math.max(90, (int) (255 * (1 - 0.5 * dist / reach))) : 255;
            int argb = (a << 24) | rgb;
            float w = width.getFloat();

            // glow layers
            if (doGlow) {
                int glowAlpha = Math.max(40, a / 3);
                RenderUtil.outline(matrices, consumers, bounds.expand(0.15), camera,
                        ((glowAlpha / 2) << 24) | rgb, glowT * 2f);
                RenderUtil.outline(matrices, consumers, bounds.expand(0.08), camera,
                        (glowAlpha << 24) | rgb, glowT * 1.3f);
                RenderUtil.outline(matrices, consumers, bounds.expand(0.03), camera,
                        (glowAlpha << 24) | rgb, glowT * 0.7f);
            }

            if (doWireframe) {
                RenderUtil.corners(matrices, consumers, bounds, camera, argb, w);
                Vec3d center = bounds.getCenter();
                RenderUtil.tracer(matrices, consumers, new Vec3d(bounds.minX, bounds.minY, bounds.minZ), center, camera, argb, w);
                RenderUtil.tracer(matrices, consumers, new Vec3d(bounds.maxX, bounds.maxY, bounds.maxZ), center, camera, argb, w);
                RenderUtil.tracer(matrices, consumers, new Vec3d(bounds.minX, bounds.maxY, bounds.minZ), center, camera, argb, w);
                RenderUtil.tracer(matrices, consumers, new Vec3d(bounds.maxX, bounds.minY, bounds.maxZ), center, camera, argb, w);
            } else if (style.is("Gradient")) {
                gradientBox(matrices, consumers, bounds, camera,
                        (a << 24) | (colorTop.getRgb() & 0xFFFFFF),
                        (a << 24) | (colorBottom.getRgb() & 0xFFFFFF), w);
            } else if (style.is("CornerPulse")) {
                double pulse = 1.0 + (flash ? 0.5 : 0.0) + Math.max(0.0, (5.0 - dist) / 5.0) * 0.4;
                float cs = (float) Math.min(0.45, cornerSize.get() * pulse);
                RenderUtil.corners(matrices, consumers, bounds, camera, argb, w, cs);
            } else if (style.is("Double")) {
                RenderUtil.outline(matrices, consumers, bounds.expand(0.04), camera,
                        (Math.max(60, a / 2) << 24) | (rgb & 0xFFFFFF), w * 0.8f);
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w * 1.6f);
            } else if (style.is("Filled")) {
                RenderUtil.fill(matrices, consumers, bounds, camera, (0x45 << 24) | rgb);
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            } else if (style.is("Corners")) {
                RenderUtil.corners(matrices, consumers, bounds, camera, argb, w);
            } else if (!doGlow || !style.is("Glow")) {
                RenderUtil.outline(matrices, consumers, bounds, camera, argb, w);
            }
        }

        if (!items.get()) return;
        int shown = 0;
        int itemRgb = 0xFF000000 | itemColor.getRgb();
        double itemReach = Math.min(reach, 24);
        Box itemArea = player.getBoundingBox().expand(itemReach);
        double contract = itemSize.get();
        for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class, itemArea, candidate ->
                candidate.squaredDistanceTo(player) <= itemReach * itemReach
                        && RenderUtil.inFront(view, candidate.getEntityPos())
                        && (!gear.get() || com.example.bfb.GearItems.lyingGear(candidate.getStack())))) {
            if (shown++ >= 30) break;
            Box itemBox = item.getBoundingBox().contract(contract);
            if (itemBox.maxX - itemBox.minX < 0.03 || itemBox.maxY - itemBox.minY < 0.03 || itemBox.maxZ - itemBox.minZ < 0.03) {
                itemBox = item.getBoundingBox();
            }
            RenderUtil.outline(matrices, consumers, itemBox, camera, itemRgb, 1.0f);
        }
    }

    /**
     * Коробка с вертикальным переливом: верхние рёбра — topArgb,
     * нижние — bottomArgb, вертикальные — плавный переход.
     */
    private static void gradientBox(MatrixStack m, VertexConsumerProvider c, Box b, Vec3d cam,
                                    int topArgb, int bottomArgb, float w) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ;
        double x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        // верх
        RenderUtil.tracer(m, c, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), cam, topArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), cam, topArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), cam, topArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), cam, topArgb, w);
        // низ
        RenderUtil.tracer(m, c, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), cam, bottomArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), cam, bottomArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), cam, bottomArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), cam, bottomArgb, w);
        // вертикали — 2 сегмента для плавного перехода
        double ym = (y1 + y2) * 0.5;
        int midArgb = lerpArgb(bottomArgb, topArgb, 0.5f);
        vertical(m, c, cam, x1, z1, y1, ym, y2, bottomArgb, midArgb, topArgb, w);
        vertical(m, c, cam, x2, z1, y1, ym, y2, bottomArgb, midArgb, topArgb, w);
        vertical(m, c, cam, x2, z2, y1, ym, y2, bottomArgb, midArgb, topArgb, w);
        vertical(m, c, cam, x1, z2, y1, ym, y2, bottomArgb, midArgb, topArgb, w);
    }

    private static void vertical(MatrixStack m, VertexConsumerProvider c, Vec3d cam,
                                 double x, double z, double yLow, double yMid, double yHigh,
                                 int bottomArgb, int midArgb, int topArgb, float w) {
        RenderUtil.tracer(m, c, new Vec3d(x, yLow, z), new Vec3d(x, yMid, z), cam, bottomArgb, w);
        RenderUtil.tracer(m, c, new Vec3d(x, yMid, z), new Vec3d(x, yHigh, z), cam, topArgb, w);
    }

    private static int lerpArgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}