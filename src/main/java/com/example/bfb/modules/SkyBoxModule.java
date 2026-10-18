package com.example.bfb.modules;

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
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkyBoxModule extends CheatModule {

    public final ModeSetting preset = add(new ModeSetting("Preset", "Star Night",
            "Star Night", "Constellations", "Planetary System", "Aurora Night",
            "Nebula", "Galaxy", "Milky Way", "Sunset", "Custom"));

    public final NumberSetting starCount = add(new NumberSetting("Star count", 1200, 20, 3000, 20));
    public final NumberSetting starSize = add(new NumberSetting("Star size", 2.0, 0.5, 8.0, 0.5));
    public final NumberSetting brightness = add(new NumberSetting("Brightness", 1.3, 0.3, 2.5, 0.1));
    public final NumberSetting rotationSpeed = add(new NumberSetting("Rotation speed", 0.05, 0.0, 1.0, 0.02));
    public final NumberSetting altitude = add(new NumberSetting("Altitude", 400, 100, 1500, 50));

    public final ColorSetting starColor = add(new ColorSetting("Star color", 0xFFFFFF));
    public final ColorSetting accentColor = add(new ColorSetting("Constellation line", 0x9C5CFF));
    public final ColorSetting planetColor = add(new ColorSetting("Planet color", 0x4CC3FF));
    public final ColorSetting planetColor2 = add(new ColorSetting("Planet color 2", 0xFF5CA8));
    public final ColorSetting nebulaColor = add(new ColorSetting("Nebula color", 0x5C2E9C));
    public final ColorSetting auroraColor = add(new ColorSetting("Aurora color", 0x3DFFB0));
    public final ColorSetting galaxyColor = add(new ColorSetting("Galaxy color", 0xC8A0FF));
    public final ColorSetting sunsetColor = add(new ColorSetting("Sunset color", 0xFF8A3D));
    public final ColorSetting blackHoleColor = add(new ColorSetting("Black hole color", 0xFFC8A0));
    public final ColorSetting milkyWayColor = add(new ColorSetting("Milky Way color", 0xB8A8E0));

    public final BooleanSetting customStars = add(new BooleanSetting("Stars", true));
    public final BooleanSetting customConstellations = add(new BooleanSetting("Constellations", false));
    public final BooleanSetting customPlanets = add(new BooleanSetting("Planets", false));
    public final BooleanSetting customAurora = add(new BooleanSetting("Aurora", false));
    public final BooleanSetting customNebula = add(new BooleanSetting("Nebula", false));
    public final BooleanSetting customGalaxy = add(new BooleanSetting("Galaxy", false));
    public final BooleanSetting customBlackHole = add(new BooleanSetting("Black hole", false));
    public final BooleanSetting customSunset = add(new BooleanSetting("Sunset", false));
    public final BooleanSetting customMilkyWay = add(new BooleanSetting("Milky Way", false));
    public final BooleanSetting customComets = add(new BooleanSetting("Comets", true));
    public final BooleanSetting customMoon = add(new BooleanSetting("Moon", true));

    public final BooleanSetting onlyAtNight = add(new BooleanSetting("Only at night", false));
    public final BooleanSetting colorfulStars = add(new BooleanSetting("Colorful stars", true));
    public final BooleanSetting hideClouds = add(new BooleanSetting("Hide clouds", true));

    public final NumberSetting planetCount = add(new NumberSetting("Planet count", 4, 1, 6, 1));
    public final NumberSetting nebulaIntensity = add(new NumberSetting("Nebula intensity", 0.6, 0.0, 1.0, 0.05));
    public final NumberSetting galaxyArms = add(new NumberSetting("Galaxy arms", 4, 2, 6, 1));
    public final NumberSetting constellationCount = add(new NumberSetting("Constellation count", 6, 1, 14, 1));

    private static final List<Vec3d> STARS = new ArrayList<>();
    private static final List<Integer> STAR_COLORS = new ArrayList<>();
    private static final List<Float> STAR_MAG = new ArrayList<>();
    private static final List<int[]> CONSTELLATIONS = new ArrayList<>();
    private static boolean generated = false;

    public SkyBoxModule() {
        super("SkyBox", "Custom sky: stars, planets, galaxy, aurora", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(false);
    }

    private static void ensureStars() {
        if (generated) return;
        generated = true;

        Random rng = new Random(1337L);
        for (int i = 0; i < 3000; i++) {
            double u = rng.nextDouble() * 2 - 1;
            double theta = rng.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(1 - u * u);
            double x = r * Math.cos(theta);
            double y = u;
            double z = r * Math.sin(theta);
            // Минимум Y = 0.15 — звёзды не лезут к горизонту
            STARS.add(new Vec3d(x, Math.abs(y) * 0.85 + 0.15, z).normalize());

            double colorRoll = rng.nextDouble();
            int color;
            if (colorRoll < 0.55) color = 0xFFFFFF;
            else if (colorRoll < 0.72) color = 0xDDEEFF;
            else if (colorRoll < 0.86) color = 0xFFEECC;
            else if (colorRoll < 0.95) color = 0xFFCC88;
            else color = 0xFFAA88;
            STAR_COLORS.add(color);

            STAR_MAG.add((float) Math.pow(rng.nextDouble(), 2.5));
        }

        Random constRng = new Random(4242L);
        for (int c = 0; c < 14; c++) {
            int size = 4 + constRng.nextInt(4);
            int[] indices = new int[size];
            for (int i = 0; i < size; i++) {
                indices[i] = constRng.nextInt(STARS.size());
            }
            CONSTELLATIONS.add(indices);
        }
    }

    public static void renderSky(MatrixStack matrices, VertexConsumerProvider consumers, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        SkyBoxModule m = ModuleManager.get(SkyBoxModule.class);
        if (m == null || !m.isEnabled()) return;

        if (m.onlyAtNight.get()) {
            long timeOfDay = client.world.getTimeOfDay() % 24000L;
            boolean isNight = timeOfDay > 12800L && timeOfDay < 23200L;
            if (!isNight) return;
        }

        ensureStars();

        long now = System.currentTimeMillis();
        float rotPhase = now / 1000f * m.rotationSpeed.getFloat() * 0.1f;
        double alt = m.altitude.get();
        float bright = m.brightness.getFloat();

        String p = m.preset.get();
        boolean doStars, doConst, doPlanets, doAurora, doNebula, doGalaxy, doBlackHole, doSunset, doMilkyWay, doComets, doMoon;
        if (p.equals("Custom")) {
            doStars = m.customStars.get();
            doConst = m.customConstellations.get();
            doPlanets = m.customPlanets.get();
            doAurora = m.customAurora.get();
            doNebula = m.customNebula.get();
            doGalaxy = m.customGalaxy.get();
            doBlackHole = m.customBlackHole.get();
            doSunset = m.customSunset.get();
            doMilkyWay = m.customMilkyWay.get();
            doComets = m.customComets.get();
            doMoon = m.customMoon.get();
        } else {
            switch (p) {
                case "Star Night" -> { doStars = true; doConst = false; doPlanets = false; doAurora = false; doNebula = false; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = false; doComets = true; doMoon = true; }
                case "Constellations" -> { doStars = true; doConst = true; doPlanets = false; doAurora = false; doNebula = false; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = false; doComets = false; doMoon = false; }
                case "Planetary System" -> { doStars = true; doConst = false; doPlanets = true; doAurora = false; doNebula = true; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = false; doComets = true; doMoon = true; }
                case "Aurora Night" -> { doStars = true; doConst = false; doPlanets = false; doAurora = true; doNebula = false; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = true; doComets = false; doMoon = false; }
                case "Nebula" -> { doStars = true; doConst = false; doPlanets = false; doAurora = false; doNebula = true; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = false; doComets = false; doMoon = false; }
                case "Galaxy" -> { doStars = true; doConst = false; doPlanets = false; doAurora = false; doNebula = false; doGalaxy = true; doBlackHole = false; doSunset = false; doMilkyWay = false; doComets = false; doMoon = false; }
                case "Milky Way" -> { doStars = true; doConst = false; doPlanets = false; doAurora = false; doNebula = true; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = true; doComets = true; doMoon = true; }
                case "Sunset" -> { doStars = false; doConst = false; doPlanets = false; doAurora = false; doNebula = false; doGalaxy = false; doBlackHole = false; doSunset = true; doMilkyWay = false; doComets = false; doMoon = false; }
                default -> { doStars = true; doConst = false; doPlanets = false; doAurora = false; doNebula = false; doGalaxy = false; doBlackHole = false; doSunset = false; doMilkyWay = false; doComets = false; doMoon = false; }
            }
        }

        Vec3d cam = Vec3d.ZERO;

        int starRgb = m.starColor.getRgb() & 0xFFFFFF;
        int accentRgb = m.accentColor.getRgb() & 0xFFFFFF;
        int planetRgb1 = m.planetColor.getRgb() & 0xFFFFFF;
        int planetRgb2 = m.planetColor2.getRgb() & 0xFFFFFF;
        int nebRgb = m.nebulaColor.getRgb() & 0xFFFFFF;
        int auroraRgb = m.auroraColor.getRgb() & 0xFFFFFF;
        int galRgb = m.galaxyColor.getRgb() & 0xFFFFFF;
        int sunsetRgb = m.sunsetColor.getRgb() & 0xFFFFFF;
        int bhRgb = m.blackHoleColor.getRgb() & 0xFFFFFF;
        int mwRgb = m.milkyWayColor.getRgb() & 0xFFFFFF;

        // === SUNSET ===
        if (doSunset) {
            int seg = 96;
            int layers = 70;
            double radius = alt * 1.1;

            for (int layer = 0; layer < layers; layer++) {
                double t = layer / (double) (layers - 1);
                double yOffset = -radius * 0.2 + t * radius * 0.5;
                int alpha = (int) (200 * Math.pow(1.0 - t, 1.6));
                if (alpha <= 2) continue;

                int c;
                if (t < 0.2) {
                    c = RenderUtil.lerpRgb(0xFF2200, sunsetRgb, (float) (t / 0.2));
                } else if (t < 0.45) {
                    c = RenderUtil.lerpRgb(sunsetRgb, 0xFFDD66, (float) ((t - 0.2) / 0.25));
                } else if (t < 0.7) {
                    c = RenderUtil.lerpRgb(0xFFDD66, 0xFFAACC, (float) ((t - 0.45) / 0.25));
                } else {
                    c = RenderUtil.lerpRgb(0xFFAACC, 0x6644AA, (float) ((t - 0.7) / 0.3));
                }
                int argb = (alpha << 24) | (c & 0xFFFFFF);

                for (int i = 0; i < seg; i++) {
                    double a1 = Math.PI * 2 * i / seg;
                    double a2 = Math.PI * 2 * (i + 1) / seg;
                    Vec3d p1 = cam.add(Math.cos(a1) * radius, yOffset, Math.sin(a1) * radius);
                    Vec3d p2 = cam.add(Math.cos(a2) * radius, yOffset, Math.sin(a2) * radius);
                    RenderUtil.tracer(matrices, consumers, p1, p2, cam, argb, 1.5f);
                }
            }

            // Солнечный диск
            Vec3d sunDir = new Vec3d(0.0, 0.0, 1.0).normalize();
            Vec3d sunPos = cam.add(sunDir.multiply(radius * 0.98));

            for (int i = 8; i >= 1; i--) {
                double glowR = alt * 0.04 * i;
                int a = (int) (90 / i);
                if (a <= 2) continue;
                int c = RenderUtil.lerpRgb(0xFF4400, 0xFFEECC, (float) (i - 1) / 7f);
                RenderUtil.ring(matrices, consumers, sunPos, cam, glowR, (a << 24) | c);
            }

            for (int i = 3; i >= 1; i--) {
                double coreR = alt * 0.012 * i;
                int c = (i == 1) ? 0xFFFFFF : RenderUtil.lerpRgb(0xFFFFFF, 0xFFEE88, (float) i / 3f);
                RenderUtil.ring(matrices, consumers, sunPos, cam, coreR, (0xFF << 24) | c);
            }

            int rays = 24;
            for (int r = 0; r < rays; r++) {
                double angle = Math.PI * 2 * r / rays + now / 3000.0;
                double rayLen = alt * 0.15 * (0.7 + 0.3 * Math.sin(now / 500.0 + r));
                Vec3d p1 = sunPos.add(Math.cos(angle) * alt * 0.02, Math.sin(angle) * alt * 0.02, 0);
                Vec3d p2 = sunPos.add(Math.cos(angle) * rayLen, Math.sin(angle) * rayLen, 0);
                int a = (int) (60 * Math.pow(Math.sin(now / 400.0 + r), 2));
                RenderUtil.tracer(matrices, consumers, p1, p2, cam, (a << 24) | 0xFFEE88, 2f);
            }
        }

        // === MOON ===
        if (doMoon) {
            Vec3d moonDir = new Vec3d(0.0, 0.4, -1.0).normalize();
            Vec3d moonPos = cam.add(moonDir.multiply(alt * 0.98));
            double moonR = alt * 0.04;

            for (int i = 6; i >= 1; i--) {
                double rr = moonR * (1 + i * 0.4);
                int a = (int) (50 / i);
                if (a <= 1) continue;
                RenderUtil.ring(matrices, consumers, moonPos, cam, rr, (a << 24) | 0xCCDDFF);
            }

            for (int i = 3; i >= 1; i--) {
                double rr = moonR * i / 3.0;
                int c = RenderUtil.lerpRgb(0xFFFFFF, 0xCCDDEE, (float) (3 - i) / 3f);
                RenderUtil.ring(matrices, consumers, moonPos, cam, rr, (0xFF << 24) | c);
            }

            Random mrng = new Random(777L);
            for (int i = 0; i < 12; i++) {
                double crAngle = mrng.nextDouble() * Math.PI * 2;
                double crDist = mrng.nextDouble() * moonR * 0.7;
                double crR = 0.3 + mrng.nextDouble() * 0.8;
                Vec3d crPos = moonPos.add(Math.cos(crAngle) * crDist, Math.sin(crAngle) * crDist, 0);
                RenderUtil.ring(matrices, consumers, crPos, cam, crR, (0x80 << 24) | 0x99AABB);
            }
        }

        // === MILKY WAY ===
        if (doMilkyWay) {
            Random rng = new Random(555L);
            Vec3d axis = new Vec3d(0.7, 0.2, 0.5).normalize();
            Vec3d perp1 = new Vec3d(-axis.z, 0, axis.x).normalize();
            Vec3d perp2 = axis.crossProduct(perp1).normalize();

            int mwStars = 600;
            for (int i = 0; i < mwStars; i++) {
                double t = (rng.nextDouble() * 2 - 1);
                double length = t * alt * 0.95;
                double spread = rng.nextGaussian() * alt * 0.12;
                double vertical = rng.nextGaussian() * alt * 0.04;

                Vec3d pos = axis.multiply(length)
                        .add(perp1.multiply(spread))
                        .add(perp2.multiply(vertical));

                double intensity = Math.exp(-(spread * spread + vertical * vertical) / (alt * alt * 0.025));
                float twinkle = 0.5f + 0.5f * (float) Math.sin(now / 600.0 + i * 0.5);
                int a = (int) (150 * intensity * twinkle);
                if (a <= 5) continue;

                int c = RenderUtil.lerpRgb(mwRgb, 0xFFFFFF, rng.nextFloat() * 0.4f);
                float sz = 0.6f + rng.nextFloat() * 1.2f;
                RenderUtil.tracerDepth(matrices, consumers,
                        pos.add(sz, 0, 0), pos.add(-sz, 0, 0), cam, (a << 24) | c, sz * 1.5f);
                RenderUtil.tracerDepth(matrices, consumers,
                        pos.add(0, sz, 0), pos.add(0, -sz, 0), cam, (a << 24) | c, sz * 1.5f);
            }

            Vec3d coreDir = axis.multiply(alt * 0.85);
            for (int i = 10; i >= 1; i--) {
                double rr = i * 3.5;
                int a = (int) (30 - i * 2.5);
                if (a <= 0) continue;
                RenderUtil.ring(matrices, consumers, coreDir, cam, rr, (a << 24) | mwRgb);
            }
        }

        // === STARS ===
        if (doStars) {
            int count = Math.min(m.starCount.getInt(), STARS.size());
            float baseSz = m.starSize.getFloat();
            boolean colorful = m.colorfulStars.get();
            for (int i = 0; i < count; i++) {
                Vec3d dir = rotateY(STARS.get(i), rotPhase);
                Vec3d pos = dir.multiply(alt);
                float mag = STAR_MAG.get(i);

                // Отсекаем тусклые — они выглядят как тёмные пятна на границе экрана
                if (mag > 0.55f) continue;

                float brightnessMul = 1.0f - mag * 0.6f;
                float twinkle = 0.75f + 0.25f * (float) Math.sin(now / 500.0 + i * 0.31);
                int a = (int) (255 * bright * twinkle * brightnessMul);
                a = Math.max(80, Math.min(255, a));

                int c = colorful ? STAR_COLORS.get(i) : starRgb;

                float s = baseSz * (0.4f + brightnessMul * 1.2f);

                RenderUtil.tracerDepth(matrices, consumers,
                        pos.add(s, 0, 0), pos.add(-s, 0, 0), cam, (a << 24) | c, s);
                RenderUtil.tracerDepth(matrices, consumers,
                        pos.add(0, s, 0), pos.add(0, -s, 0), cam, (a << 24) | c, s);
                if (brightnessMul > 0.6f) {
                    float s2 = s * 0.6f;
                    RenderUtil.tracerDepth(matrices, consumers,
                            pos.add(s2, s2, 0), pos.add(-s2, -s2, 0), cam, (a / 2 << 24) | c, s * 0.8f);
                    RenderUtil.tracerDepth(matrices, consumers,
                            pos.add(s2, -s2, 0), pos.add(-s2, s2, 0), cam, (a / 2 << 24) | c, s * 0.8f);
                }
            }
        }

        // === COMETS ===
        if (doComets) {
            Random crng = new Random(9999L);
            int cometCount = 3;
            for (int i = 0; i < cometCount; i++) {
                long cometPhase = (now / 18000L + i * 7) % 100;
                float cometT = cometPhase / 100f;
                if (cometT > 0.35f) continue;

                double startAngle = crng.nextDouble() * Math.PI * 2;
                double startHeight = 0.2 + crng.nextDouble() * 0.6;
                double travelDist = 0.8;

                double cx = Math.cos(startAngle) * (1 - cometT * travelDist);
                double cz = Math.sin(startAngle) * (1 - cometT * travelDist);
                double cy = startHeight - cometT * 0.3;
                Vec3d head = new Vec3d(cx, cy, cz).normalize().multiply(alt);

                int tailSteps = 24;
                int tailLen = (int) (alt * 0.2);
                for (int t = 1; t <= tailSteps; t++) {
                    double tailT = t / (double) tailSteps;
                    Vec3d tailPos = head.multiply(1.0 + tailT * 0.18)
                            .add(Math.cos(startAngle) * tailLen * tailT,
                                    -tailLen * tailT * 0.3,
                                    Math.sin(startAngle) * tailLen * tailT);
                    int a = (int) (220 * (1 - tailT) * (1 - tailT));
                    if (a <= 3) continue;
                    RenderUtil.tracerDepth(matrices, consumers, head, tailPos, cam,
                            (a << 24) | 0xFFFFFF, 2.5f);
                }
                RenderUtil.ring(matrices, consumers, head, cam, 1.5, (0xFF << 24) | 0xFFFFFF);
            }
        }

        // === CONSTELLATIONS ===
        if (doConst) {
            int constCount = Math.min(m.constellationCount.getInt(), CONSTELLATIONS.size());
            for (int c = 0; c < constCount; c++) {
                int[] indices = CONSTELLATIONS.get(c);
                for (int i = 0; i < indices.length - 1; i++) {
                    int i1 = indices[i];
                    int i2 = indices[i + 1];
                    if (i1 >= STARS.size() || i2 >= STARS.size()) continue;
                    Vec3d a1 = rotateY(STARS.get(i1), rotPhase);
                    Vec3d a2 = rotateY(STARS.get(i2), rotPhase);
                    if (a1.distanceTo(a2) > 0.6) continue;
                    Vec3d pa = a1.multiply(alt * 0.98);
                    Vec3d pb = a2.multiply(alt * 0.98);
                    RenderUtil.tracerDepth(matrices, consumers, pa, pb, cam,
                            (0x70 << 24) | accentRgb, 2.5f);
                }
            }
        }

        // === PLANETS ===
        if (doPlanets) {
            int n = Math.min(m.planetCount.getInt(), 6);
            int[][] specs = {
                    {14, 1, 0, 42, 0},
                    {22, 0, 1, 43, 1},
                    {10, 1, 0, 44, 2},
                    {18, 0, 1, 45, 3},
                    {8,  0, 0, 46, 4},
                    {28, 1, 1, 47, 5},
            };

            for (int i = 0; i < n; i++) {
                int[] spec = specs[i];
                double planetR = spec[0];
                boolean hasRings = spec[1] == 1;
                int c1 = (spec[2] == 0) ? planetRgb1 : planetRgb2;
                int c2 = (spec[2] == 0) ? planetRgb2 : planetRgb1;

                Random prng = new Random(spec[3]);
                double angle = (spec[4] / 6.0) * Math.PI * 2 + rotPhase * 0.3;
                double elev = 0.2 + prng.nextDouble() * 0.6;
                double x = Math.cos(angle) * (1 - elev * 0.5);
                double y = elev;
                double z = Math.sin(angle) * (1 - elev * 0.5);
                Vec3d dir = new Vec3d(x, y, z).normalize();
                Vec3d center = dir.multiply(alt * 0.92);

                int layers = 14;
                for (int l = 0; l < layers; l++) {
                    double t = (l + 0.5) / layers;
                    double yOff = (t - 0.5) * 2 * planetR;
                    double r = Math.sqrt(Math.max(0, 1 - (yOff / planetR) * (yOff / planetR))) * planetR;
                    if (r < 0.5) continue;

                    int col = RenderUtil.lerpRgb(c1, c2, (float) t);
                    int a = 220;
                    if (l == 0 || l == layers - 1) a = 130;

                    Vec3d ringCenter = center.add(0, yOff, 0);
                    RenderUtil.ring(matrices, consumers, ringCenter, cam, r, (a << 24) | col);
                }

                if (hasRings) {
                    double ringInner = planetR * 1.5;
                    double ringOuter = planetR * 2.4;
                    int ringCount = 8;
                    for (int ri = 0; ri < ringCount; ri++) {
                        double rt = ri / (double) ringCount;
                        double rr = ringInner + (ringOuter - ringInner) * rt;
                        int alpha = (int) (170 * (1 - rt * 0.5));
                        int ringCol = RenderUtil.lerpRgb(c1, c2, (float) rt);
                        RenderUtil.ring(matrices, consumers, center, cam, rr, (alpha << 24) | ringCol);
                    }
                }
            }
        }

        // === BLACK HOLE ===
        if (doBlackHole) {
            Vec3d dir = new Vec3d(0.55, 0.55, -0.55).normalize();
            dir = rotateY(dir, rotPhase * 0.2);
            Vec3d center = dir.multiply(alt * 1.02);

            double outerR = 25;
            double innerR = 10;

            for (int l = 0; l < 10; l++) {
                double t = l / 9.0;
                double rr = innerR + (outerR - innerR) * t;
                float hue = (float) ((now / 2000.0 + l * 0.05) % 1.0);
                int c = RenderUtil.hsbToRgb(hue, 0.85f, 1f);
                int a = (int) (200 * (1 - t * 0.7));
                RenderUtil.ring(matrices, consumers, center, cam, rr, (a << 24) | c);
            }
            for (int l = 0; l < 5; l++) {
                double rr = innerR * (1 - l * 0.2);
                RenderUtil.ring(matrices, consumers, center, cam, rr, 0xFF000000);
            }
            for (int l = 0; l < 6; l++) {
                double rr = outerR + 4 + l * 3;
                int a = 50 - l * 7;
                if (a <= 0) continue;
                RenderUtil.ring(matrices, consumers, center, cam, rr, (a << 24) | bhRgb);
            }
        }

        // === AURORA ===
        if (doAurora) {
            int lines = 8;
            for (int l = 0; l < lines; l++) {
                float phase = now / 2000f + l * 0.5f;
                double baseY = 0.25 + l * 0.06;
                double baseR = alt * 0.85;

                Vec3d prev = null;
                int seg = 80;
                for (int i = 0; i <= seg; i++) {
                    double t = i / (double) seg;
                    double angle = t * Math.PI * 2;
                    double wave = Math.sin(angle * 3 + phase) * 0.15
                            + Math.sin(angle * 5 + phase * 1.3) * 0.08;
                    double x = Math.cos(angle) * 0.4;
                    double z = Math.sin(angle) * 0.4;
                    double y = baseY + wave;
                    Vec3d dir = new Vec3d(x, y, z).normalize();
                    Vec3d pos = dir.multiply(baseR);

                    if (prev != null) {
                        int a = (int) (120 * (1 - l / (float) lines));
                        int c = RenderUtil.lerpRgb(auroraRgb, 0x8844FF, (float) l / lines);
                        RenderUtil.tracer(matrices, consumers, prev, pos, cam,
                                (a << 24) | c, 6f);
                    }
                    prev = pos;
                }
            }
        }

        // === NEBULA ===
        if (doNebula) {
            float intensity = m.nebulaIntensity.getFloat();
            Random rng = new Random(88);
            for (int i = 0; i < 50; i++) {
                double nx = (rng.nextDouble() * 2 - 1) * 0.85;
                double ny = 0.15 + rng.nextDouble() * 0.75;
                double nz = (rng.nextDouble() * 2 - 1) * 0.85;
                Vec3d dir = new Vec3d(nx, ny, nz).normalize();
                dir = rotateY(dir, rotPhase * 0.1);
                Vec3d center = dir.multiply(alt * 0.88);
                double r = 15 + rng.nextDouble() * 35;
                int a = (int) (intensity * 45);
                if (a <= 0) continue;

                int neb2 = RenderUtil.lerpRgb(nebRgb, 0xFF66AA, rng.nextFloat() * 0.5f);
                for (int l = 0; l < 8; l++) {
                    double rr = r * (l + 1) / 8.0;
                    int aa = (int) (a * (1 - l / 8.0));
                    RenderUtil.ring(matrices, consumers, center, cam, rr, (aa << 24) | neb2);
                }
            }
        }

        // === GALAXY ===
        if (doGalaxy) {
            Random rng = new Random(7);
            int arms = m.galaxyArms.getInt();
            Vec3d coreDir = new Vec3d(0.4, 0.9, 0.3).normalize();
            coreDir = rotateY(coreDir, rotPhase * 0.15);
            Vec3d coreCenter = coreDir.multiply(alt * 0.95);

            for (int l = 0; l < 5; l++) {
                double rr = 2 + l * 2;
                int a = 220 - l * 30;
                RenderUtil.ring(matrices, consumers, coreCenter, cam, rr, (a << 24) | galRgb);
            }

            int starPerArm = 80;
            for (int arm = 0; arm < arms; arm++) {
                double armBaseAngle = arm * (Math.PI * 2 / arms);
                for (int i = 0; i < starPerArm; i++) {
                    double t = i / (double) starPerArm;
                    double spiralAngle = armBaseAngle + t * Math.PI * 2.5;
                    double radius = 3 + t * 40;
                    double x = Math.cos(spiralAngle) * radius * 0.6;
                    double z = Math.sin(spiralAngle) * radius * 0.6;
                    double y = (rng.nextDouble() - 0.5) * 6;
                    Vec3d dir = coreDir.add(x, y, z).normalize();
                    Vec3d pos = dir.multiply(alt * 0.93);

                    float twinkle = 0.6f + 0.4f * (float) Math.sin(now / 400.0 + i * 0.7 + arm);
                    int a = (int) (180 * (1f - t * 0.5f) * twinkle);
                    if (a <= 5) continue;
                    int c = RenderUtil.lerpRgb(galRgb, 0xFFFFFF, (float) t * 0.5f);
                    float sz = m.starSize.getFloat();
                    Vec3d off1 = new Vec3d(sz, sz, 0);
                    Vec3d off2 = new Vec3d(-sz, -sz, 0);
                    RenderUtil.tracerDepth(matrices, consumers, pos.add(off1), pos.add(off2),
                            cam, (a << 24) | (c & 0xFFFFFF), sz * 3f);
                }
            }
        }
    }

    private static Vec3d rotateY(Vec3d v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vec3d(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }
}