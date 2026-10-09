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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SkeletonModule extends CheatModule {
    public final ModeSetting style = add(new ModeSetting("Style", "Full", "Full", "Simple", "Box-only"));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFF));
    public final ColorSetting jointColor = add(new ColorSetting("Joint color", 0x7C5CFF));
    public final NumberSetting range = add(new NumberSetting("Range", 32, 8, 64, 1));
    public final NumberSetting width = add(new NumberSetting("Width", 1.8, 0.8, 4.0, 0.1));
    public final NumberSetting jointSize = add(new NumberSetting("Joint radius", 0.08, 0.02, 0.2, 0.01));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", true));
    public final BooleanSetting invisibles = add(new BooleanSetting("Invisibles", false));
    public final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));

    public SkeletonModule() {
        super("Skeleton", "Full humanoid skeleton through walls", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        var self = client.player;
        if (self == null || client.world == null) return;
        float tick = client.getRenderTickCounter().getTickProgress(false);
        int argb = 0xFF000000 | color.getRgb();
        int jointArgb = 0xE0000000 | jointColor.getRgb();
        double reach = range.get();
        double reachSq = reach * reach;
        Camera view = client.gameRenderer.getCamera();
        Box area = self.getBoundingBox().expand(reach);
        float lineW = width.getFloat();
        double jr = jointSize.get();

        int drawn = 0;
        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, area, candidate ->
                CombatUtil.accept(candidate, self, players.get(), mobs.get(), animals.get())
                        && (invisibles.get() || !candidate.isInvisible())
                        && candidate.squaredDistanceTo(self) <= reachSq
                        && RenderUtil.inFront(view, candidate.getEntityPos()))) {
            if (drawn++ >= 40) break;

            Vec3d pos = entity.getLerpedPos(tick);
            float bodyYaw = MathHelper.lerpAngleDegrees(tick, entity.lastBodyYaw, entity.bodyYaw);
            float headYaw = MathHelper.lerpAngleDegrees(tick, entity.lastHeadYaw, entity.headYaw);

            // координаты модели в world
            double cx = pos.x;
            double cz = pos.z;
            double feetY = pos.y;
            double h = entity.getHeight();

            // ориентиры
            double headTop = feetY + h;
            double headCenter = feetY + h - 0.15;
            double neck = feetY + h - 0.30;
            double chest = feetY + h * 0.68;
            double waist = feetY + h * 0.50;
            double hip = feetY + h * 0.42;
            double kneeY = feetY + h * 0.22;
            double armTop = feetY + h - 0.35;
            double armMid = feetY + h * 0.68;
            double armBot = feetY + h * 0.52;

            // направления
            double bodyRad = Math.toRadians(bodyYaw);
            double bodyFx = -Math.sin(bodyRad);
            double bodyFz = Math.cos(bodyRad);
            double bodyRx = -bodyFz;
            double bodyRz = bodyFx;

            double headRad = Math.toRadians(headYaw);
            double headFx = -Math.sin(headRad);
            double headFz = Math.cos(headRad);

            double shWidth = entity.getWidth() * 0.5;

            // === точка в мире через якорь ===
            Vec3d feet = new Vec3d(cx, feetY, cz);
            Vec3d headCV = new Vec3d(cx, headCenter, cz);
            Vec3d neckV = new Vec3d(cx, neck, cz);
            Vec3d chestV = new Vec3d(cx, chest, cz);
            Vec3d waistV = new Vec3d(cx, waist, cz);
            Vec3d hipV = new Vec3d(cx, hip, cz);
            Vec3d headTopV = new Vec3d(cx, headTop, cz);

            // плечи
            Vec3d shoulderL = new Vec3d(cx - bodyRx * shWidth, armTop, cz - bodyRz * shWidth);
            Vec3d shoulderR = new Vec3d(cx + bodyRx * shWidth, armTop, cz + bodyRz * shWidth);
            // руки
            Vec3d elbowL = new Vec3d(cx - bodyRx * (shWidth + 0.02), armMid, cz - bodyRz * (shWidth + 0.02));
            Vec3d elbowR = new Vec3d(cx + bodyRx * (shWidth + 0.02), armMid, cz + bodyRz * (shWidth + 0.02));
            Vec3d handL = new Vec3d(cx - bodyRx * (shWidth + 0.03), armBot, cz - bodyRz * (shWidth + 0.03));
            Vec3d handR = new Vec3d(cx + bodyRx * (shWidth + 0.03), armBot, cz + bodyRz * (shWidth + 0.03));
            // ноги
            double legOff = entity.getWidth() * 0.22;
            Vec3d hipL = new Vec3d(cx - bodyRx * legOff, hip, cz - bodyRz * legOff);
            Vec3d hipR = new Vec3d(cx + bodyRx * legOff, hip, cz + bodyRz * legOff);
            Vec3d kneeL = new Vec3d(cx - bodyRx * legOff, kneeY, cz - bodyRz * legOff);
            Vec3d kneeR = new Vec3d(cx + bodyRx * legOff, kneeY, cz + bodyRz * legOff);
            Vec3d footL = new Vec3d(cx - bodyRx * legOff, feetY, cz - bodyRz * legOff);
            Vec3d footR = new Vec3d(cx + bodyRx * legOff, feetY, cz + bodyRz * legOff);

            if (style.is("Simple")) {
                // упрощённый — только spine + руки + ноги
                RenderUtil.tracer(matrices, consumers, feet, headTopV, camera, argb, lineW);
                RenderUtil.tracer(matrices, consumers, shoulderL, shoulderR, camera, argb, lineW);
                RenderUtil.tracer(matrices, consumers, hipL, hipR, camera, argb, lineW);
                RenderUtil.tracer(matrices, consumers, shoulderL, handL, camera, argb, lineW);
                RenderUtil.tracer(matrices, consumers, shoulderR, handR, camera, argb, lineW);
                RenderUtil.tracer(matrices, consumers, hipL, footL, camera, argb, lineW);
                RenderUtil.tracer(matrices, consumers, hipR, footR, camera, argb, lineW);
                if (tracers.get()) {
                    Vec3d mid = new Vec3d(cx, feetY + h * 0.5, cz);
                    RenderUtil.tracer(matrices, consumers, self.getEyePos(), mid, camera, argb, 1.4f);
                }
                continue;
            }

            // === FULL SKELETON ===

            // голова — круг из 12 точек
            int headSeg = 12;
            double headR = entity.getWidth() * 0.55;
            for (int i = 0; i < headSeg; i++) {
                double a1 = Math.PI * 2 * i / headSeg;
                double a2 = Math.PI * 2 * (i + 1) / headSeg;
                RenderUtil.bone(matrices, consumers, camera,
                        new Vec3d(cx + Math.cos(a1) * headR, headCenter, cz + Math.sin(a1) * headR),
                        new Vec3d(cx + Math.cos(a2) * headR, headCenter, cz + Math.sin(a2) * headR),
                        argb, lineW);
            }
            // направление взгляда (короткая стрелка от головы)
            Vec3d lookEnd = new Vec3d(cx + headFx * 0.4, headCenter + 0.05, cz + headFz * 0.4);
            RenderUtil.tracer(matrices, consumers, headCV, lookEnd, camera, argb, lineW * 0.8f);

            // шея
            RenderUtil.tracer(matrices, consumers, headCV, neckV, camera, argb, lineW);
            // позвоночник
            RenderUtil.tracer(matrices, consumers, neckV, chestV, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, chestV, waistV, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, waistV, hipV, camera, argb, lineW);
            // плечи
            RenderUtil.tracer(matrices, consumers, shoulderL, shoulderR, camera, argb, lineW);
            // бёдра (соединяем крестец с каждой ногой)
            RenderUtil.tracer(matrices, consumers, hipV, hipL, camera, argb, lineW * 0.8f);
            RenderUtil.tracer(matrices, consumers, hipV, hipR, camera, argb, lineW * 0.8f);
            // руки — плечо → локоть → кисть
            RenderUtil.tracer(matrices, consumers, shoulderL, elbowL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, elbowL, handL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, shoulderR, elbowR, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, elbowR, handR, camera, argb, lineW);
            // ноги — бедро → колено → ступня
            RenderUtil.tracer(matrices, consumers, hipL, kneeL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, kneeL, footL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, hipR, kneeR, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, kneeR, footR, camera, argb, lineW);

            // === суставы (кольца) ===
            drawJoint(matrices, consumers, camera, headCV, jr * 1.6, jointArgb);
            drawJoint(matrices, consumers, camera, neckV, jr, jointArgb);
            drawJoint(matrices, consumers, camera, chestV, jr, jointArgb);
            drawJoint(matrices, consumers, camera, waistV, jr, jointArgb);
            drawJoint(matrices, consumers, camera, hipV, jr, jointArgb);
            drawJoint(matrices, consumers, camera, shoulderL, jr, jointArgb);
            drawJoint(matrices, consumers, camera, shoulderR, jr, jointArgb);
            drawJoint(matrices, consumers, camera, elbowL, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, elbowR, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, handL, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, handR, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, hipL, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, hipR, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, kneeL, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, kneeR, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, footL, jr * 0.8, jointArgb);
            drawJoint(matrices, consumers, camera, footR, jr * 0.8, jointArgb);

            // === трейсер ===
            if (tracers.get()) {
                Vec3d mid = new Vec3d(cx, feetY + h * 0.5, cz);
                RenderUtil.tracer(matrices, consumers, self.getEyePos(), mid, camera, argb, 1.4f);
            }
        }
    }

    private static void drawJoint(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                  Vec3d center, double radius, int argb) {
        int seg = 12;
        for (int i = 0; i < seg; i++) {
            double a1 = Math.PI * 2 * i / seg;
            double a2 = Math.PI * 2 * (i + 1) / seg;
            RenderUtil.bone(matrices, consumers, camera,
                    new Vec3d(center.x + Math.cos(a1) * radius, center.y, center.z + Math.sin(a1) * radius),
                    new Vec3d(center.x + Math.cos(a2) * radius, center.y, center.z + Math.sin(a2) * radius),
                    argb, 1.6f);
        }
    }
}