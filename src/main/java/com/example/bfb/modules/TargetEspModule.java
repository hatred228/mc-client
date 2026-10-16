package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.ModuleManager;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class TargetEspModule extends CheatModule {
    public final ModeSetting source = add(new ModeSetting("Source", "Auto",
            "Auto", "KillAura", "AimAssist", "Manual"));
    public final BooleanSetting useKillAura = add(new BooleanSetting("Use KillAura", true));
    public final BooleanSetting useAimAssist = add(new BooleanSetting("Use AimAssist", true));

    public final ModeSetting style = add(new ModeSetting("Style", "Cube",
            "Cube", "Rings", "Chains", "Orb", "Scanner", "All"));

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

    // === CUBE — маленький куб в центре цели, крутится ===
    public final NumberSetting cubeSize = add(new NumberSetting("Cube size", 0.4, 0.1, 1.2, 0.05));
    public final NumberSetting cubeSpinSpeed = add(new NumberSetting("Cube spin", 1.0, 0.2, 4.0, 0.1));
    public final NumberSetting cubeThickness = add(new NumberSetting("Cube thickness", 1.8, 0.8, 4.0, 0.2));
    public final BooleanSetting cubeFaces = add(new BooleanSetting("Cube filled faces", false));
    public final NumberSetting cubeFaceAlpha = add(new NumberSetting("Face alpha", 60, 10, 180, 5));
    public final NumberSetting cubeHeightOffset = add(new NumberSetting("Cube Y offset", 0.5, 0.0, 1.5, 0.05));

    // === scanner ===
    public final NumberSetting scanSpeed = add(new NumberSetting("Scan speed", 1.5, 0.3, 4.0, 0.1));
    public final NumberSetting scanHeight = add(new NumberSetting("Scan height", 1.0, 0.5, 2.0, 0.1));
    public final NumberSetting scanTrail = add(new NumberSetting("Scan trail", 1, 1, 16, 1));
    public final BooleanSetting scanRotate = add(new BooleanSetting("Scan rotate", true));
    public final NumberSetting scanRadius = add(new NumberSetting("Scan radius", 1.4, 0.5, 3.0, 0.1));
    public final ColorSetting scanColor = add(new ColorSetting("Scan color", 0x9C5CFF));
    public final BooleanSetting scanTrailFade = add(new BooleanSetting("Trail fade", true));

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

        String st = style.get();
        boolean doCube = st.equals("Cube") || st.equals("All");
        boolean doRings = st.equals("Rings") || st.equals("All");
        boolean doChains = st.equals("Chains") || st.equals("All");
        boolean doOrb = st.equals("Orb") || st.equals("All");
        boolean doScanner = st.equals("Scanner") || st.equals("All");

        // === CUBE — маленький куб в центре цели ===
        if (doCube) {
            Vec3d cubeCenter = pos.add(0, height * cubeHeightOffset.getFloat(), 0);
            drawRotatingCube(matrices, consumers, camera, cubeCenter, time, c1, c2);
        }

        // === RINGS ===
        if (doRings) {
            drawRing(matrices, consumers, camera, pos, baseScale * 1.0, 0, time,
                    ringSpeed.getFloat(), c1, seg, thick, 0xCC);
            drawRing(matrices, consumers, camera, pos, baseScale * 0.75, 1, time,
                    ringSpeed.getFloat() * 1.4f, c2, seg, thick, 0xCC);
            drawRing(matrices, consumers, camera, pos, baseScale * 0.55, 2, time,
                    ringSpeed.getFloat() * 0.8f, c1, seg, thick, 0xCC);
        }

        // === CHAINS ===
        if (doChains) {
            drawChains(matrices, consumers, camera, pos, baseScale * 1.3, height, time,
                    ringSpeed.getFloat(), chainRgb, thick, chainLayers.getInt());
        }

        // === ORB ===
        if (doOrb) {
            double orbRadius = baseScale * 0.3;
            double pulse = 1.0 + Math.sin(time / 400.0) * 0.15;
            Vec3d center = pos.add(0, height * 0.5, 0);
            for (int i = 0; i < 4; i++) {
                double r = orbRadius * pulse * (1.0 + i * 0.4);
                int alpha = 0xCC / (i + 1);
                drawRing(matrices, consumers, camera, center, r, i, time, 2f, c1, seg,
                        thick * (1.0f - i * 0.15f), alpha);
            }
        }

        // === SCANNER ===
        if (doScanner) {
            double seconds = (time % 200000L) / 1000.0;
            double cycle = (seconds * scanSpeed.getFloat()) % 2.0;
            boolean goingUp = cycle < 1.0;
            double scanT = goingUp ? cycle : (2.0 - cycle);

            double targetY = pos.y + height * scanT * scanHeight.getFloat() - 0.05;
            double scanR = baseScale * scanRadius.getFloat();

            int scanRgb = rainbow.get()
                    ? RenderUtil.rainbow(rainbowSpeed.get())
                    : (scanColor.getRgb() & 0xFFFFFF);

            double rotPhase = scanRotate.get() ? seconds * 1.2 : 0;

            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg + rotPhase;
                double a2 = Math.PI * 2 * (i + 1) / seg + rotPhase;
                Vec3d p1 = new Vec3d(pos.x + Math.cos(a1) * scanR, targetY, pos.z + Math.sin(a1) * scanR);
                Vec3d p2 = new Vec3d(pos.x + Math.cos(a2) * scanR, targetY, pos.z + Math.sin(a2) * scanR);
                RenderUtil.tracer(matrices, consumers, p1, p2, camera, (0xFF << 24) | scanRgb, 2.4f);
            }

            int trailN = scanTrail.getInt();
            for (int trail = 1; trail <= trailN; trail++) {
                double delay = trail * 0.03;
                double trailT = goingUp ? (scanT - delay) : (scanT + delay);
                if (trailT < 0.0 || trailT > 1.0) continue;

                double trailY = pos.y + height * trailT * scanHeight.getFloat() - 0.05;
                double spreadR = scanR + trail * 0.05;

                double alpha = (1.0 - trail / (double) (trailN + 1)) * 0.55;
                int ta = (int) (alpha * 220);
                if (ta <= 5) continue;

                int trailRgb = scanTrailFade.get()
                        ? RenderUtil.lerpRgb(scanRgb, 0xFFFFFF, (float) (trail / (double) (trailN + 1) * 0.5))
                        : scanRgb;

                for (int i = 0; i < seg; i++) {
                    double a1 = Math.PI * 2 * i / seg + rotPhase;
                    double a2 = Math.PI * 2 * (i + 1) / seg + rotPhase;

                    Vec3d p1 = new Vec3d(pos.x + Math.cos(a1) * spreadR, trailY, pos.z + Math.sin(a1) * spreadR);
                    Vec3d p2 = new Vec3d(pos.x + Math.cos(a2) * spreadR, trailY, pos.z + Math.sin(a2) * spreadR);
                    RenderUtil.tracer(matrices, consumers, p1, p2, camera,
                            (ta << 24) | (trailRgb & 0xFFFFFF), 1.8f);
                }
            }
        }
    }

    // ============================================================
    // ROTATING CUBE
    // ============================================================

    private void drawRotatingCube(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                   Vec3d center, long time, int c1, int c2) {
        double s = cubeSize.getFloat();
        double spin = time * 0.001 * cubeSpinSpeed.getFloat();
        double tilt = time * 0.0007 * cubeSpinSpeed.getFloat();

        // 8 вершин куба в локальной системе
        double[][] local = {
                {-s, -s, -s}, { s, -s, -s}, { s, -s,  s}, {-s, -s,  s},
                {-s,  s, -s}, { s,  s, -s}, { s,  s,  s}, {-s,  s,  s}
        };

        // вращение вокруг Y и X
        double cy = Math.cos(spin), sy = Math.sin(spin);
        double cx = Math.cos(tilt), sx = Math.sin(tilt);
        Vec3d[] world = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            double x = local[i][0], y = local[i][1], z = local[i][2];
            // Y
            double x1 = x * cy - z * sy;
            double z1 = x * sy + z * cy;
            // X
            double y2 = y * cx - z1 * sx;
            double z2 = y * sx + z1 * cx;
            world[i] = center.add(x1, y2, z2);
        }

        // 12 рёбер
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        float th = cubeThickness.getFloat();

        // gradient по вертикали куба: c1 сверху, c2 снизу
        int topC = c1, botC = c2;

        for (int[] e : edges) {
            Vec3d a = world[e[0]];
            Vec3d b = world[e[1]];
            // средний Y определяет цвет ребра
            double midY = (a.y + b.y) * 0.5 - center.y;
            float t = (float) ((midY / s + 1.0) * 0.5);
            t = Math.max(0f, Math.min(1f, t));
            int col = RenderUtil.lerpRgb(botC, topC, t);
            RenderUtil.tracer(matrices, consumers, a, b, camera, (0xFF << 24) | (col & 0xFFFFFF), th);
        }

        // заливка граней — 6 граней, каждая = 2 треугольника
        if (cubeFaces.get()) {
            int faceAlpha = cubeFaceAlpha.getInt();
            int[][] faces = {
                    {0,1,2,3}, // низ
                    {4,5,6,7}, // верх
                    {0,1,5,4}, // перед
                    {2,3,7,6}, // зад
                    {1,2,6,5}, // право
                    {3,0,4,7}  // лево
            };
            for (int[] face : faces) {
                Vec3d a = world[face[0]];
                Vec3d b = world[face[1]];
                Vec3d c = world[face[2]];
                Vec3d d = world[face[3]];
                double midY = (a.y + b.y + c.y + d.y) * 0.25 - center.y;
                float t = (float) ((midY / s + 1.0) * 0.5);
                t = Math.max(0f, Math.min(1f, t));
                int col = RenderUtil.lerpRgb(botC, topC, t);
                int argb = (faceAlpha << 24) | (col & 0xFFFFFF);
                RenderUtil.triangle(matrices, consumers, camera, a, b, c, argb);
                RenderUtil.triangle(matrices, consumers, camera, a, c, d, argb);
            }
        }
    }

    // ============================================================
    // RINGS / CHAINS
    // ============================================================

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
            int c = RenderUtil.lerpRgb(rgb, 0xFFFFFF, t * 0.3f);
            RenderUtil.tracer(matrices, consumers, p1, p2, camera, (alpha << 24) | (c & 0xFFFFFF), thick);
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

    private void drawChains(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                            Vec3d center, double radius, double height,
                            long time, float speed, int rgb, float thick, int layers) {
        int linkCount = Math.max(8, (int) (radius * 12));
        double spin = time * 0.0008 * speed;

        for (int layer = 0; layer < layers; layer++) {
            double layerT = layers <= 1 ? 0.5 : layer / (double) (layers - 1);
            double y = center.y + height * (0.2 + layerT * 0.7);
            double breath = 1.0 + Math.sin(time / 500.0 + layer * 1.2) * 0.05;
            double layerRadius = radius * breath;
            double layerSpin = spin + layer * 0.3;

            for (int i = 0; i < linkCount; i++) {
                double a1 = Math.PI * 2 * i / linkCount + layerSpin;
                double a2 = Math.PI * 2 * (i + 1) / linkCount + layerSpin;
                Vec3d p1 = new Vec3d(center.x + Math.cos(a1) * layerRadius, y,
                        center.z + Math.sin(a1) * layerRadius);
                Vec3d p2 = new Vec3d(center.x + Math.cos(a2) * layerRadius, y,
                        center.z + Math.sin(a2) * layerRadius);

                int alpha = (int) (0xCC * (1.0 - layerT * 0.3));
                int c = RenderUtil.lerpRgb(rgb, 0x888888, (float) (layerT * 0.5));
                RenderUtil.tracer(matrices, consumers, p1, p2, camera, (alpha << 24) | (c & 0xFFFFFF), thick);

                if (i % 2 == 0) {
                    double linkR = 0.05;
                    double swirl = Math.sin(time / 300.0 + i) * 0.03;
                    Vec3d top = new Vec3d(p1.x, y + linkR + swirl, p1.z);
                    Vec3d bot = new Vec3d(p1.x, y - linkR + swirl, p1.z);
                    Vec3d left = new Vec3d(p1.x - linkR * 0.6, y + swirl, p1.z);
                    Vec3d right = new Vec3d(p1.x + linkR * 0.6, y + swirl, p1.z);
                    RenderUtil.tracer(matrices, consumers, top, right, camera, (alpha << 24) | rgb, thick * 0.7f);
                    RenderUtil.tracer(matrices, consumers, right, bot, camera, (alpha << 24) | rgb, thick * 0.7f);
                    RenderUtil.tracer(matrices, consumers, bot, left, camera, (alpha << 24) | rgb, thick * 0.7f);
                    RenderUtil.tracer(matrices, consumers, left, top, camera, (alpha << 24) | rgb, thick * 0.7f);
                }
            }
        }
    }

    // ============================================================
    // TARGET PICK
    // ============================================================

    private Entity pickTarget() {
        if (source.is("Auto") || source.is("KillAura") || source.is("Manual")) {
            if (useKillAura.get()) {
                KillAuraModule aura = ModuleManager.get(KillAuraModule.class);
                if (aura != null && aura.isEnabled()
                        && KillAuraModule.target instanceof LivingEntity le && le.isAlive()) {
                    return le;
                }
            }
        }
        if (source.is("Auto") || source.is("AimAssist") || source.is("Manual")) {
            if (useAimAssist.get()) {
                AimAssistModule assist = ModuleManager.get(AimAssistModule.class);
                if (assist != null && assist.isEnabled()
                        && AimAssistModule.target instanceof LivingEntity le && le.isAlive()) {
                    return le;
                }
            }
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        return null;
    }
}