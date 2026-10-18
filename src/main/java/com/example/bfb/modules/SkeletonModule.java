package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SkeletonModule extends CheatModule {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFF));
    public final NumberSetting range = add(new NumberSetting("Range", 32, 8, 64, 1));
    public final NumberSetting width = add(new NumberSetting("Width", 1.8, 0.8, 4.0, 0.1));
    public final NumberSetting headRadius = add(new NumberSetting("Head radius", 0.15, 0.05, 0.3, 0.01));
    public final NumberSetting armSpread = add(new NumberSetting("Arm spread", 0.25, 0.1, 0.5, 0.01));
    public final NumberSetting legSpread = add(new NumberSetting("Leg spread", 0.15, 0.05, 0.4, 0.01));
    public final NumberSetting torsoWidth = add(new NumberSetting("Torso width", 0.22, 0.1, 0.5, 0.01));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", true));
    public final BooleanSetting invisibles = add(new BooleanSetting("Invisibles", false));
    public final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));

    public SkeletonModule() {
        super("Skeleton", "Simple connected humanoid skeleton", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        var self = client.player;
        if (self == null || client.world == null) return;
        float tick = client.getRenderTickCounter().getTickProgress(false);
        int argb = 0xFF000000 | color.getRgb();
        double reach = range.get();
        double reachSq = reach * reach;
        Camera view = client.gameRenderer.getCamera();
        Box area = self.getBoundingBox().expand(reach);
        float lineW = width.getFloat();
        float hr = headRadius.getFloat();
        float as = armSpread.getFloat();
        float ls = legSpread.getFloat();
        float tw = torsoWidth.getFloat();

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

            double cx = pos.x;
            double cz = pos.z;
            double feetY = pos.y;
            double h = entity.getHeight();

            // === пропорции фигуры (относительно высоты) ===
            double headTop = feetY + h;              // верх головы
            double headBottom = feetY + h * 0.85;    // низ головы / плечи
            double chestY = feetY + h * 0.70;        // верх торса (плечи-руки)
            double waistY = feetY + h * 0.55;        // середина торса
            double hipY = feetY + h * 0.45;          // таз, начало ног
            double kneeY = feetY + h * 0.25;
            double handY = feetY + h * 0.50;         // кисти
            double footY = feetY + 0.02;

            // === направления (yaw) ===
            double bodyRad = Math.toRadians(bodyYaw);
            double bodyFx = -Math.sin(bodyRad);
            double bodyFz = Math.cos(bodyRad);
            // правая ось в плоскости XZ (перпендикулярно bodyF)
            double bodyRx = bodyFz;
            double bodyRz = -bodyFx;

            // голова повёрнута отдельно (headYaw) — но фигуру рисуем по bodyYaw
            // для простоты — игнорируем headYaw, скелет стабилен

            // === точки скелета ===
            // центр головы
            Vec3d headCenter = new Vec3d(cx, feetY + h * 0.925, cz);
            // шея — над плечами
            Vec3d neck = new Vec3d(cx, headBottom, cz);
            // плечи — на chestY, левое и правое
            Vec3d shoulderL = new Vec3d(cx - bodyRx * tw, chestY, cz - bodyRz * tw);
            Vec3d shoulderR = new Vec3d(cx + bodyRx * tw, chestY, cz + bodyRz * tw);
            // таз
            Vec3d hipCenter = new Vec3d(cx, hipY, cz);
            // руки — кисти на handY, ниже плеч
            Vec3d handL = new Vec3d(cx - bodyRx * as, handY, cz - bodyRz * as);
            Vec3d handR = new Vec3d(cx + bodyRx * as, handY, cz + bodyRz * as);
            // колени
            Vec3d kneeL = new Vec3d(cx - bodyRx * ls, kneeY, cz - bodyRz * ls);
            Vec3d kneeR = new Vec3d(cx + bodyRx * ls, kneeY, cz + bodyRz * ls);
            // ступни
            Vec3d footL = new Vec3d(cx - bodyRx * ls, footY, cz - bodyRz * ls);
            Vec3d footR = new Vec3d(cx + bodyRx * ls, footY, cz + bodyRz * ls);

            // === 1. голова — вертикальная линия от neck до headTop ===
            // (по скрину голова рисуется как маленький кружок или черта)
            RenderUtil.tracer(matrices, consumers, neck, headCenter.add(0, hr * 0.5, 0), camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, headCenter.add(0, -hr * 0.5, 0),
                    headCenter.add(0, hr * 0.5, 0), camera, argb, lineW);

            // === 2. плечи — горизонтальная линия между shoulderL и shoulderR ===
            RenderUtil.tracer(matrices, consumers, shoulderL, shoulderR, camera, argb, lineW);

            // === 3. шея — вертикаль от плеч вверх до головы ===
            RenderUtil.tracer(matrices, consumers, neck, new Vec3d(cx, chestY, cz), camera, argb, lineW);

            // === 4. руки — от плеч вниз до кистей ===
            RenderUtil.tracer(matrices, consumers, shoulderL, handL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, shoulderR, handR, camera, argb, lineW);

            // === 5. торс — вертикаль от плеч до таза ===
            RenderUtil.tracer(matrices, consumers, new Vec3d(cx, chestY, cz), hipCenter, camera, argb, lineW);

            // === 6. ноги — от таза через колени до ступней ===
            // верх ноги: hipCenter → knee
            RenderUtil.tracer(matrices, consumers, hipCenter, kneeL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, hipCenter, kneeR, camera, argb, lineW);
            // низ ноги: knee → foot
            RenderUtil.tracer(matrices, consumers, kneeL, footL, camera, argb, lineW);
            RenderUtil.tracer(matrices, consumers, kneeR, footR, camera, argb, lineW);

            // === 7. трейсер (опционально) ===
            if (tracers.get()) {
                Vec3d mid = new Vec3d(cx, feetY + h * 0.5, cz);
                RenderUtil.tracer(matrices, consumers, self.getEyePos(), mid, camera, argb, 1.4f);
            }
        }
    }
}