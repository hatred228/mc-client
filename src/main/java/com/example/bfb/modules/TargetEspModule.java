package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudStyle;
import com.example.bfb.ModuleManager;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class TargetEspModule extends CheatModule {
    public final ModeSetting source = add(new ModeSetting("Source", "Auto",
            "Auto", "KillAura", "AimAssist", "Manual"));
    public final BooleanSetting useKillAura = add(new BooleanSetting("Use KillAura", true));
    public final BooleanSetting useAimAssist = add(new BooleanSetting("Use AimAssist", true));

    public final ModeSetting style = add(new ModeSetting("Style", "All",
            "Rings", "Chains", "Orb", "All"));

    public final NumberSetting size = add(new NumberSetting("Size", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting ringSpeed = add(new NumberSetting("Ring speed", 1.0, 0.2, 4.0, 0.1));
    public final NumberSetting segments = add(new NumberSetting("Segments", 48, 16, 96, 4));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", 2.0, 1.0, 5.0, 0.5));
    public final NumberSetting chainLayers = add(new NumberSetting("Chain layers", 3, 1, 6, 1));

    public final ColorSetting color1 = add(new ColorSetting("Color 1", 0x9C5CFF));
    public final ColorSetting color2 = add(new ColorSetting("Color 2", 0xFF5CA8));
    public final ColorSetting chainColor = add(new ColorSetting("Chain color", 0xE0E0E0));
    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow speed", 1.0, 0.2, 4.0, 0.1));

    public TargetEspModule() {
        super("TargetESP", "Animated effects around your target", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;

        Entity target = pickTarget();
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;

        float tick = client.getRenderTickCounter().getTickProgress(false);
        Vec3d pos = living.getLerpedPos(tick);

        long time = System.currentTimeMillis();
        double baseScale = size.getFloat() * living.getWidth();
        double height = living.getHeight();

        int c1 = rainbow.get() ? RenderUtil.rainbow(rainbowSpeed.get()) : (color1.getRgb() & 0xFFFFFF);
        int c2 = rainbow.get() ? RenderUtil.rainbow(rainbowSpeed.get() + 0.5) : (color2.getRgb() & 0xFFFFFF);
        int chainRgb = chainColor.getRgb() & 0xFFFFFF;
        float thick = thickness.getFloat();
        int seg = segments.getInt();

        String style = this.style.get();
        boolean doRings = style.equals("Rings") || style.equals("All");
        boolean doChains = style.equals("Chains") || style.equals("All");
        boolean doOrb = style.equals("Orb") || style.equals("All");

        // === 3 вращающихся кольца ===
        if (doRings) {
            drawRing(matrices, consumers, camera, pos, baseScale * 1.0, 0, time, ringSpeed.getFloat(), c1, seg, thick, 0xCC);
            drawRing(matrices, consumers, camera, pos, baseScale * 0.75, 1, time, ringSpeed.getFloat() * 1.4f, c2, seg, thick, 0xCC);
            drawRing(matrices, consumers, camera, pos, baseScale * 0.55, 2, time, ringSpeed.getFloat() * 0.8f, c1, seg, thick, 0xCC);
        }

        // === горизонтальное ожерелье из цепей ===
        if (doChains) {
            drawChains(matrices, consumers, camera, pos, baseScale * 1.3, height, time, ringSpeed.getFloat(),
                    chainRgb, thick, chainLayers.getInt());
        }

        // === пульсирующий орб в центре ===
        if (doOrb) {
            double orbRadius = baseScale * 0.3;
            double pulse = 1.0 + Math.sin(time / 400.0) * 0.15;
            Vec3d center = pos.add(0, height * 0.5, 0);
            for (int i = 0; i < 4; i++) {
                double r = orbRadius * pulse * (1.0 + i * 0.4);
                int alpha = 0xCC / (i + 1);
                drawRing(matrices, consumers, camera, center, r, i, time, 2f, c1, seg, thick * (1.0f - i * 0.15f), alpha);
            }
        }
    }

    private void drawRing(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                          Vec3d center, double radius, int layer, long time, float speed,
                          int rgb, int segments, float thick, int alpha) {
        double yShift = Math.sin(time / 600.0 + layer * 2.0) * 0.08;
        double yawPhase = time * 0.001 * speed + layer * 1.5;
        double tiltPhase = time * 0.0007 * speed + layer * 0.8;

        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;

            Vec3d p1 = ringPoint(center, radius, a1, yawPhase, tiltPhase, yShift);
            Vec3d p2 = ringPoint(center, radius, a2, yawPhase, tiltPhase, yShift);

            float t = i / (float) segments;
            int c = HudStyle.lerpColor(rgb, 0xFFFFFF, t * 0.3f);
            RenderUtil.bone(matrices, consumers, camera, p1, p2, (alpha << 24) | (c & 0xFFFFFF), thick);
        }
    }

    private static Vec3d ringPoint(Vec3d center, double radius, double angle,
                                    double yaw, double tilt, double yShift) {
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        double y = 0;

        double cy = Math.cos(tilt);
        double sy = Math.sin(tilt);
        double y1 = y * cy - z * sy;
        double z1 = y * sy + z * cy;

        double cyaw = Math.cos(yaw);
        double syaw = Math.sin(yaw);
        double x2 = x * cyaw - z1 * syaw;
        double z2 = x * syaw + z1 * cyaw;

        return new Vec3d(center.x + x2, center.y + y1 + yShift, center.z + z2);
    }

    /**
     * Горизонтальное ожерелье — вращающийся круг из звеньев цепи вокруг цели.
     * Несколько ярусов на разной высоте, каждый ярус — звенья цепочки.
     */
    private void drawChains(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                            Vec3d center, double radius, double height,
                            long time, float speed, int rgb, float thick, int layers) {
        // сколько звеньев на ярус — пропорционально радиусу
        int linkCount = Math.max(8, (int) (radius * 12));

        // общий поворот всей цепочки вокруг цели
        double spin = time * 0.0008 * speed;

        for (int layer = 0; layer < layers; layer++) {
            // высота каждого яруса: от ~20% до ~90% высоты цели
            double layerT = layers <= 1 ? 0.5 : layer / (double) (layers - 1);
            double y = center.y + height * (0.2 + layerT * 0.7);

            // лёгкое «дыхание» радиуса по времени
            double breath = 1.0 + Math.sin(time / 500.0 + layer * 1.2) * 0.05;
            double layerRadius = radius * breath;

            // небольшой фазовый сдвиг по слоям
            double layerSpin = spin + layer * 0.3;

            // каждое звено — мини-ромб, соединённый с соседними
            for (int i = 0; i < linkCount; i++) {
                double a1 = Math.PI * 2 * i / linkCount + layerSpin;
                double a2 = Math.PI * 2 * (i + 1) / linkCount + layerSpin;

                Vec3d p1 = new Vec3d(center.x + Math.cos(a1) * layerRadius, y, center.z + Math.sin(a1) * layerRadius);
                Vec3d p2 = new Vec3d(center.x + Math.cos(a2) * layerRadius, y, center.z + Math.sin(a2) * layerRadius);

                // линия между звеньями — сама цепь
                int alpha = (int) (0xCC * (1.0 - layerT * 0.3));
                int c = HudStyle.lerpColor(rgb, 0x888888, (float) (layerT * 0.5));
                RenderUtil.bone(matrices, consumers, camera, p1, p2, (alpha << 24) | (c & 0xFFFFFF), thick);

                // каждый второй — ромб-«звено» поверх, для текстуры
                if (i % 2 == 0) {
                    double linkR = 0.05;
                    double swirl = Math.sin(time / 300.0 + i) * 0.03;
                    Vec3d top = new Vec3d(p1.x, y + linkR + swirl, p1.z);
                    Vec3d bot = new Vec3d(p1.x, y - linkR + swirl, p1.z);
                    Vec3d left = new Vec3d(p1.x - linkR * 0.6, y + swirl, p1.z);
                    Vec3d right = new Vec3d(p1.x + linkR * 0.6, y + swirl, p1.z);

                    RenderUtil.bone(matrices, consumers, camera, top, right, (alpha << 24) | rgb, thick * 0.7f);
                    RenderUtil.bone(matrices, consumers, camera, right, bot, (alpha << 24) | rgb, thick * 0.7f);
                    RenderUtil.bone(matrices, consumers, camera, bot, left, (alpha << 24) | rgb, thick * 0.7f);
                    RenderUtil.bone(matrices, consumers, camera, left, top, (alpha << 24) | rgb, thick * 0.7f);
                }
            }
        }
    }

    private Entity pickTarget() {
        // Auto — берём любую доступную цель из включённых модулей
        if (source.is("Auto")) {
            if (useKillAura.get()) {
                KillAuraModule aura = ModuleManager.get(KillAuraModule.class);
                if (aura != null && aura.isEnabled() && KillAuraModule.target != null && KillAuraModule.target.isAlive()) {
                    return KillAuraModule.target;
                }
            }
            if (useAimAssist.get()) {
                AimAssistModule assist = ModuleManager.get(AimAssistModule.class);
                if (assist != null && assist.isEnabled() && AimAssistModule.target != null && AimAssistModule.target.isAlive()) {
                    return AimAssistModule.target;
                }
            }
        }
        if (source.is("KillAura") || source.is("Manual")) {
            if (useKillAura.get()) {
                KillAuraModule aura = ModuleManager.get(KillAuraModule.class);
                if (aura != null && aura.isEnabled() && KillAuraModule.target != null && KillAuraModule.target.isAlive()) {
                    return KillAuraModule.target;
                }
            }
        }
        if (source.is("AimAssist") || source.is("Manual")) {
            if (useAimAssist.get()) {
                AimAssistModule assist = ModuleManager.get(AimAssistModule.class);
                if (assist != null && assist.isEnabled() && AimAssistModule.target != null && AimAssistModule.target.isAlive()) {
                    return AimAssistModule.target;
                }
            }
        }
        return null;
    }
}