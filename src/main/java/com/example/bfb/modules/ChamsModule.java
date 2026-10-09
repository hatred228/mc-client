package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.BfbRenderLayers;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.RenderUtil;
import com.example.bfb.mixin.AgeableModelAccessor;
import com.example.bfb.mixin.LivingRendererAccessor;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ChamsModule extends CheatModule {
    public final ModeSetting style = add(new ModeSetting("Style", "Gradient",
            "Model", "Flat", "Shell", "Glow", "Pulse", "Glass", "Rainbow", "Wireframe",
            "Gradient", "Aurora", "Wave", "Scanline", "Fire", "Hologram", "Neon"));

    public final ColorSetting color = add(new ColorSetting("Color", 0x7C5CFF));

    // === 3-stop gradient ===
    public final ColorSetting colorTop = add(new ColorSetting("Gradient top", 0xFF5CA8));
    public final ColorSetting colorMid = add(new ColorSetting("Gradient middle", 0x9C5CFF));
    public final ColorSetting colorBottom = add(new ColorSetting("Gradient bottom", 0x4C2EFF));
    public final NumberSetting midPos = add(new NumberSetting("Middle position", 0.5, 0.0, 1.0, 0.05));
    public final ModeSetting curve = add(new ModeSetting("Curve", "Smooth",
            "Linear", "Smooth", "Sine", "Sqrt", "Square"));
    public final BooleanSetting gradientInvert = add(new BooleanSetting("Gradient invert", false));

    // === animated gradient ===
    public final NumberSetting shiftAmount = add(new NumberSetting("Gradient shift", 0.0, 0.0, 0.5, 0.02));
    public final NumberSetting noiseAmount = add(new NumberSetting("Gradient noise", 0.0, 0.0, 0.3, 0.01));
    public final BooleanSetting glowPass = add(new BooleanSetting("Glow pass", false));

    public final NumberSetting animSpeed = add(new NumberSetting("Animation speed", 1.0, 0.2, 4.0, 0.1));

    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false));
    public final NumberSetting speed = add(new NumberSetting("Rainbow speed", 1.0, 0.2, 4.0, 0.1));
    public final NumberSetting alpha = add(new NumberSetting("Alpha", 220, 30, 255, 1));

    public final NumberSetting glowThickness = add(new NumberSetting("Glow thickness", 2.5, 1.0, 6.0, 0.5));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 96, 1));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting hurt = add(new BooleanSetting("Hurt flash", true));
    public final BooleanSetting fade = add(new BooleanSetting("Distance fade", false));

    private static final int MAX_MODELS = 16;
    private static final List<LivingEntity> VISIBLE = new ArrayList<>();
    private static final Map<EntityRenderer<?, ?>, EntityRenderState> STATES = new IdentityHashMap<>();

    private enum Anim { AURORA, WAVE, SCANLINE, FIRE, HOLOGRAM }
    private enum Curve {
        LINEAR { public float apply(float t) { return t; } },
        SMOOTH { public float apply(float t) { return t * t * (3f - 2f * t); } },
        SINE { public float apply(float t) { return (float) (0.5 - Math.cos(t * Math.PI) * 0.5); } },
        SQRT { public float apply(float t) { return (float) Math.sqrt(t); } },
        SQUARE { public float apply(float t) { return t * t; } };
        public abstract float apply(float t);
    }

    public ChamsModule() {
        super("Chams", "Renders entity models through walls", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.world == null || client.player == null) return;
        float tick = client.getRenderTickCounter().getTickProgress(false);

        long time = System.currentTimeMillis();
        int baseRgb = style.is("Rainbow") || rainbow.get() ? RenderUtil.rainbow(speed.get()) : color.getRgb();
        int baseAmount = alpha.getInt();
        boolean pulseStyle = style.is("Pulse");
        float pulseMul = 1f;
        if (pulseStyle) {
            pulseMul = 0.45f + 0.55f * (float) Math.sin(time / 300.0);
            baseAmount = Math.max(50, Math.min(255, Math.round(baseAmount * pulseMul)));
        }

        double reach = range.get();
        double reachSq = reach * reach;
        Camera view = client.gameRenderer.getCamera();
        var player = client.player;

        VISIBLE.clear();
        Box area = player.getBoundingBox().expand(reach);
        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, area, candidate ->
                candidate != player
                        && CombatUtil.accept(candidate, player, players.get(), mobs.get(), animals.get())
                        && candidate.squaredDistanceTo(player) <= reachSq
                        && RenderUtil.inFront(view, candidate.getEntityPos()))) {
            VISIBLE.add(entity);
        }
        if (VISIBLE.size() > MAX_MODELS) {
            VISIBLE.sort(Comparator.comparingDouble((LivingEntity e) -> e instanceof PlayerEntity ? 0 : 1)
                    .thenComparingDouble(e -> e.squaredDistanceTo(player)));
            VISIBLE.subList(MAX_MODELS, VISIBLE.size()).clear();
        }

        var dispatcher = client.getEntityRenderDispatcher();
        float aSpeed = animSpeed.getFloat();
        Curve curveMode = switch (curve.get()) {
            case "Linear" -> Curve.LINEAR;
            case "Sine" -> Curve.SINE;
            case "Sqrt" -> Curve.SQRT;
            case "Square" -> Curve.SQUARE;
            default -> Curve.SMOOTH;
        };

        for (LivingEntity living : VISIBLE) {
            EntityRenderer<?, ?> renderer = dispatcher.getRenderer(living);
            if (!(renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer)) continue;
            EntityRenderState raw = stateFor(renderer, living, tick);
            if (!(raw instanceof LivingEntityRenderState state)) continue;
            Identifier skin = ((LivingRendererAccessor) livingRenderer).bfb$texture(state);

            int amount = baseAmount;
            if (fade.get()) {
                double d = Math.sqrt(living.squaredDistanceTo(player));
                amount = Math.max(28, (int) (baseAmount * (1.0 - 0.75 * d / reach)));
            }
            int rgbE = baseRgb;
            boolean hurtNow = hurt.get() && living.hurtTime > 0;
            if (hurtNow) rgbE = 0xFF3B30;

            if (style.is("Wireframe")) {
                int tint = (amount << 24) | (rgbE & 0xFFFFFF);
                RenderUtil.outline(matrices, consumers, living.getBoundingBox().expand(0.02), camera, tint, 1.5f);
                Vec3d center = living.getBoundingBox().getCenter();
                Box box = living.getBoundingBox();
                RenderUtil.tracer(matrices, consumers, new Vec3d(box.minX, box.minY, box.minZ), center, camera, tint, 1.0f);
                RenderUtil.tracer(matrices, consumers, new Vec3d(box.maxX, box.maxY, box.maxZ), center, camera, tint, 1.0f);
                RenderUtil.tracer(matrices, consumers, new Vec3d(box.minX, box.maxY, box.minZ), center, camera, tint, 1.0f);
                RenderUtil.tracer(matrices, consumers, new Vec3d(box.maxX, box.minY, box.maxZ), center, camera, tint, 1.0f);
                continue;
            }

            if (style.is("Gradient")) {
                int topArgb = (amount << 24) | (colorTop.getRgb() & 0xFFFFFF);
                int midArgb = (amount << 24) | (colorMid.getRgb() & 0xFFFFFF);
                int bottomArgb = (amount << 24) | (colorBottom.getRgb() & 0xFFFFFF);
                if (hurtNow) {
                    topArgb = (amount << 24) | 0xFF3B30;
                    midArgb = (amount << 24) | 0x8A0000;
                    bottomArgb = (amount << 24) | 0x4A0000;
                }
                drawGradient(matrices, consumers, camera, living, state, livingRenderer, tick,
                        topArgb, midArgb, bottomArgb,
                        midPos.getFloat(), gradientInvert.get(), curveMode,
                        shiftAmount.getFloat(), noiseAmount.getFloat(), aSpeed,
                        glowPass.get() ? amount : 0);
                continue;
            }

            if (style.is("Aurora")) {
                drawAnimated(matrices, consumers, camera, living, state, livingRenderer, tick,
                        Anim.AURORA, amount, colorTop.getRgb(), colorBottom.getRgb(), aSpeed, hurtNow);
                continue;
            }
            if (style.is("Wave")) {
                drawAnimated(matrices, consumers, camera, living, state, livingRenderer, tick,
                        Anim.WAVE, amount, colorTop.getRgb(), colorBottom.getRgb(), aSpeed, hurtNow);
                continue;
            }
            if (style.is("Scanline")) {
                drawAnimated(matrices, consumers, camera, living, state, livingRenderer, tick,
                        Anim.SCANLINE, amount, color.getRgb(), 0xFFFFFF, aSpeed, hurtNow);
                continue;
            }
            if (style.is("Fire")) {
                drawAnimated(matrices, consumers, camera, living, state, livingRenderer, tick,
                        Anim.FIRE, amount, 0xFF6A00, 0x4A0000, aSpeed, hurtNow);
                continue;
            }
            if (style.is("Hologram")) {
                drawAnimated(matrices, consumers, camera, living, state, livingRenderer, tick,
                        Anim.HOLOGRAM, (int) (amount * 0.75f), color.getRgb(), 0xFFFFFF, aSpeed, hurtNow);
                continue;
            }
            if (style.is("Neon")) {
                int solid = (Math.min(245, amount) << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, solid, 1.0f);
                Box b = living.getBoundingBox();
                double t = (time % 1400L) / 1400.0;
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2.0);
                int layers = 4;
                for (int i = 1; i <= layers; i++) {
                    int a = (int) ((200 - i * 40) * pulse);
                    if (a <= 0) continue;
                    RenderUtil.outline(matrices, consumers, b.expand(0.03 * i),
                            camera, (a << 24) | (rgbE & 0xFFFFFF), 1.8f);
                }
                continue;
            }

            if (style.is("Model")) {
                if (skin != null) {
                    drawTextured(matrices, consumers, camera, living, state, livingRenderer, tick,
                            skin, (amount << 24) | 0xFFFFFF, 1.0f);
                }
                continue;
            }

            if (style.is("Rainbow")) {
                int rb = RenderUtil.rainbow(speed.get());
                int c = (amount << 24) | (rb & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, c, 1.0f);
                if (skin != null) {
                    drawTextured(matrices, consumers, camera, living, state, livingRenderer, tick,
                            skin, (Math.min(120, amount / 2) << 24) | 0xFFFFFF, 1.0f);
                }
                continue;
            }

            if (style.is("Flat")) {
                int flatColor = (Math.min(180, amount) << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, flatColor, 1.0f);
                continue;
            }

            if (style.is("Shell")) {
                int shellColor = (Math.min(140, amount) << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, shellColor, 1.08f);
                int innerColor = (Math.min(90, amount) << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, innerColor, 1.0f);
                if (skin != null) {
                    drawTextured(matrices, consumers, camera, living, state, livingRenderer, tick,
                            skin, (Math.min(200, amount) << 24) | 0xFFFFFF, 1.0f);
                }
                continue;
            }

            if (style.is("Glass")) {
                int glassColor = (60 << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, glassColor, 1.02f);
                if (skin != null) {
                    drawTextured(matrices, consumers, camera, living, state, livingRenderer, tick,
                            skin, (Math.min(230, amount) << 24) | 0xFFFFFF, 1.0f);
                }
                continue;
            }

            if (style.is("Glow")) {
                int glowColor = (Math.min(230, amount) << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, glowColor, 1.0f);
                Box b = living.getBoundingBox();
                int layers = Math.max(1, (int) glowThickness.getFloat() * 2);
                for (int i = 1; i <= layers; i++) {
                    int gAlpha = Math.max(15, amount / 8 / i);
                    RenderUtil.outline(matrices, consumers, b.expand(0.03 * i),
                            camera, (gAlpha << 24) | (rgbE & 0xFFFFFF), 1.5f);
                }
                continue;
            }

            if (pulseStyle) {
                int pulseColor = (amount << 24) | (rgbE & 0xFFFFFF);
                drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, pulseColor, 1.0f);
                Box b = living.getBoundingBox();
                int gAlpha = Math.max(20, (int) (amount * pulseMul / 3));
                RenderUtil.outline(matrices, consumers, b.expand(0.05), camera, (gAlpha << 24) | (rgbE & 0xFFFFFF), 2.5f);
                continue;
            }

            int fallback = (Math.min(200, amount) << 24) | (rgbE & 0xFFFFFF);
            drawColored(matrices, consumers, camera, living, state, livingRenderer, tick, fallback, 1.0f);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void drawGradient(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                     LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer,
                                     float tick, int topArgb, int midArgb, int bottomArgb,
                                     float midPosition, boolean invert, Curve curve,
                                     float shiftAmount, float noiseAmount, float animSpeed,
                                     int glowAlpha) {
        EntityModel model = renderer.getModel();
        boolean babyMesh = false;
        if (state.baby && renderer instanceof AgeableModelAccessor access && access.bfb$babyModel() != null) {
            model = access.bfb$babyModel();
            babyMesh = true;
        }
        if (model == null) return;
        model.setAngles(state);
        Vec3d pos = entity.getLerpedPos(tick);
        float scale = state.baseScale;
        if (!babyMesh && state.ageScale > 0.01f && state.ageScale < 0.99f) scale *= state.ageScale;
        if (scale < 0.01f || Float.isNaN(scale) || Float.isInfinite(scale)) return;

        float feetY = (float) (pos.y - camera.y);
        float headY = feetY + entity.getHeight();

        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.scale(scale, scale, scale);
        if (!state.isInPose(EntityPose.SLEEPING)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.bodyYaw));
        }
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0f, -1.501f, 0f);

        if (glowAlpha > 0) {
            MatrixStack glowMatrices = new MatrixStack();
            glowMatrices.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
            glowMatrices.translate(0, -0.02f, 0);
            int gAlpha = Math.max(20, glowAlpha / 4);
            VertexConsumer glowUnderlying = consumers.getBuffer(BfbRenderLayers.chamsColor());
            VertexConsumer glowConsumer = new GradientVertexConsumer(glowUnderlying,
                    (gAlpha << 24) | (topArgb & 0xFFFFFF),
                    (gAlpha << 24) | (midArgb & 0xFFFFFF),
                    (gAlpha << 24) | (bottomArgb & 0xFFFFFF),
                    feetY, headY, midPosition, invert, curve, shiftAmount, noiseAmount, animSpeed);
            model.render(glowMatrices, glowConsumer, 0xF000F0, LivingEntityRenderer.getOverlay(state, 0f), 0xFFFFFFFF);
        }

        VertexConsumer underlying = consumers.getBuffer(BfbRenderLayers.chamsColor());
        VertexConsumer gradient = new GradientVertexConsumer(underlying,
                topArgb, midArgb, bottomArgb,
                feetY, headY, midPosition, invert, curve, shiftAmount, noiseAmount, animSpeed);
        int overlay = LivingEntityRenderer.getOverlay(state, 0f);
        model.render(matrices, gradient, 0xF000F0, overlay, 0xFFFFFFFF);
        matrices.pop();
    }

    private static final class GradientVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int topColor;
        private final int midColor;
        private final int bottomColor;
        private final float feetY;
        private final float headY;
        private final float midPosition;
        private final boolean invert;
        private final Curve curve;
        private final float shiftAmount;
        private final float noiseAmount;
        private final float animSpeed;
        private final long time;
        private float currentRel;

        GradientVertexConsumer(VertexConsumer delegate, int topColor, int midColor, int bottomColor,
                               float feetY, float headY, float midPosition, boolean invert, Curve curve,
                               float shiftAmount, float noiseAmount, float animSpeed) {
            this.delegate = delegate;
            this.topColor = topColor;
            this.midColor = midColor;
            this.bottomColor = bottomColor;
            this.feetY = feetY;
            this.headY = headY;
            this.midPosition = MathHelper.clamp(midPosition, 0.001f, 0.999f);
            this.invert = invert;
            this.curve = curve;
            this.shiftAmount = shiftAmount;
            this.noiseAmount = noiseAmount;
            this.animSpeed = animSpeed;
            this.time = System.currentTimeMillis();
        }

        private int blend() {
            float t = currentRel;
            if (invert) t = 1f - t;
            t = MathHelper.clamp(t, 0f, 1f);

            if (shiftAmount > 0f) {
                t += (float) Math.sin(time * 0.0015 * animSpeed) * shiftAmount;
                t = MathHelper.clamp(t, 0f, 1f);
            }

            t = curve.apply(t);

            if (t < midPosition) {
                float k = t / midPosition;
                return lerpArgb3(topColor, midColor, k);
            } else {
                float k = (t - midPosition) / (1f - midPosition);
                return lerpArgb3(midColor, bottomColor, k);
            }
        }

        private int lerpArgb3(int a, int b, float k) {
            k = MathHelper.clamp(k, 0f, 1f);
            int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
            int r = (int) (ar + (br - ar) * k);
            int g = (int) (ag + (bg - ag) * k);
            int bl = (int) (ab + (bb - ab) * k);
            int al = (int) (aa + (ba - aa) * k);
            return (al << 24) | (r << 16) | (g << 8) | bl;
        }

        private void captureY(float x, float y, float z) {
            float range = headY - feetY;
            if (Math.abs(range) < 0.001f) range = 1f;
            float t = (headY - y) / range;

            if (noiseAmount > 0f) {
                int hx = Float.floatToIntBits(x);
                int hy = Float.floatToIntBits(y);
                int hz = Float.floatToIntBits(z);
                int h = (hx * 73856093) ^ (hy * 19349663) ^ (hz * 83492791);
                float n = ((h & 0xFFFF) / 65535f) - 0.5f;
                t += n * noiseAmount;
            }

            this.currentRel = t;
        }

        @Override
        public VertexConsumer vertex(MatrixStack.Entry entry, float x, float y, float z) {
            captureY(x, y, z);
            delegate.vertex(entry, x, y, z);
            return this;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            captureY(x, y, z);
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            int c = blend();
            delegate.color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF);
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            delegate.color(blend());
            return this;
        }

        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(MatrixStack.Entry entry, float x, float y, float z) { delegate.normal(entry, x, y, z); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public VertexConsumer lineWidth(float w) { delegate.lineWidth(w); return this; }

        @Override
        public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                           float nx, float ny, float nz) {
            captureY(x, y, z);
            delegate.vertex(x, y, z, blend(), u, v, overlay, light, nx, ny, nz);
        }
    }

    // ================ ANIMATED ================

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void drawAnimated(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                     LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer,
                                     float tick, Anim anim, int alpha, int colorA, int colorB,
                                     float animSpeed, boolean hurtNow) {
        EntityModel model = renderer.getModel();
        boolean babyMesh = false;
        if (state.baby && renderer instanceof AgeableModelAccessor access && access.bfb$babyModel() != null) {
            model = access.bfb$babyModel();
            babyMesh = true;
        }
        if (model == null) return;
        model.setAngles(state);
        Vec3d pos = entity.getLerpedPos(tick);
        float scale = state.baseScale;
        if (!babyMesh && state.ageScale > 0.01f && state.ageScale < 0.99f) scale *= state.ageScale;
        if (scale < 0.01f || Float.isNaN(scale) || Float.isInfinite(scale)) return;

        float feetY = (float) (pos.y - camera.y);
        float headY = feetY + entity.getHeight();

        int a = alpha;
        int cA = colorA;
        int cB = colorB;
        if (hurtNow) { a = Math.max(a, 200); cA = 0xFF3B30; cB = 0x4A0000; }

        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.scale(scale, scale, scale);
        if (!state.isInPose(EntityPose.SLEEPING)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.bodyYaw));
        }
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0f, -1.501f, 0f);

        VertexConsumer underlying = consumers.getBuffer(BfbRenderLayers.chamsColor());
        VertexConsumer animated = new AnimatedVertexConsumer(underlying, anim,
                feetY, headY, cA, cB, a, animSpeed);
        int overlay = LivingEntityRenderer.getOverlay(state, 0f);
        model.render(matrices, animated, 0xF000F0, overlay, 0xFFFFFFFF);
        matrices.pop();
    }

    private static final class AnimatedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final Anim anim;
        private final float feetY;
        private final float headY;
        private final int colorA;
        private final int colorB;
        private final int baseAlpha;
        private final float animSpeed;
        private final long time;
        private int currentColor;

        AnimatedVertexConsumer(VertexConsumer delegate, Anim anim, float feetY, float headY,
                               int colorA, int colorB, int baseAlpha, float animSpeed) {
            this.delegate = delegate;
            this.anim = anim;
            this.feetY = feetY;
            this.headY = headY;
            this.colorA = colorA;
            this.colorB = colorB;
            this.baseAlpha = baseAlpha;
            this.animSpeed = animSpeed;
            this.time = System.currentTimeMillis();
        }

        private void computeColor(float x, float y, float z) {
            float range = Math.max(0.05f, headY - feetY);
            float t = (y - feetY) / range;
            t = MathHelper.clamp(t, 0f, 1f);

            switch (anim) {
                case AURORA -> {
                    float hue = (t * 0.6f + (time % 6000L) * 0.0001f * animSpeed) % 1f;
                    if (hue < 0f) hue += 1f;
                    int rgb = RenderUtil.hsbToRgb(hue, 0.85f, 1.0f);
                    currentColor = (baseAlpha << 24) | (rgb & 0xFFFFFF);
                }
                case WAVE -> {
                    float phase = (float) Math.sin(t * Math.PI * 3.0 + time * 0.004 * animSpeed);
                    float k = (phase + 1f) * 0.5f;
                    currentColor = lerpArgbWithAlpha(colorA, colorB, k, baseAlpha);
                }
                case SCANLINE -> {
                    float speed = 0.0025f * animSpeed;
                    float pos = ((time * speed) % 1f + 1f) % 1f;
                    float scan = (t - pos);
                    scan = scan - (float) Math.round(scan);
                    float bright = 1f - Math.abs(scan) * 2.5f;
                    bright = MathHelper.clamp(bright, 0f, 1f);
                    bright = bright * bright;
                    int a = (int) (baseAlpha * (0.55f + 0.45f * bright));
                    int rgb = RenderUtil.lerpRgb(colorA, colorB, bright);
                    currentColor = (a << 24) | (rgb & 0xFFFFFF);
                }
                case FIRE -> {
                    float flicker = (float) Math.sin(time * 0.01 * animSpeed + y * 8.0) * 0.15f;
                    float heat = MathHelper.clamp(t + flicker, 0f, 1f);
                    int cold = 0x2A0000, mid = 0xFF6A00, hot = 0xFFFFEE;
                    int c = heat < 0.5f
                            ? RenderUtil.lerpRgb(cold, mid, heat * 2f)
                            : RenderUtil.lerpRgb(mid, hot, (heat - 0.5f) * 2f);
                    int a = (int) (baseAlpha * (0.7f + 0.3f * heat));
                    currentColor = (a << 24) | (c & 0xFFFFFF);
                }
                case HOLOGRAM -> {
                    float grid = (float) Math.abs(Math.sin(y * 22.0 + time * 0.002 * animSpeed));
                    float aMul = 0.35f + 0.65f * grid;
                    int a = (int) (baseAlpha * aMul);
                    int base = colorA & 0xFFFFFF;
                    int rgb = RenderUtil.lerpRgb(base, 0xFFFFFF, grid * 0.35f);
                    currentColor = (a << 24) | (rgb & 0xFFFFFF);
                }
            }
        }

        private static int lerpArgbWithAlpha(int a, int b, float k, int alpha) {
            return (alpha << 24) | RenderUtil.lerpRgb(a & 0xFFFFFF, b & 0xFFFFFF, k);
        }

        @Override public VertexConsumer vertex(MatrixStack.Entry entry, float x, float y, float z) { delegate.vertex(entry, x, y, z); return this; }
        @Override public VertexConsumer vertex(float x, float y, float z) { delegate.vertex(x, y, z); return this; }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            if (currentColor != 0) {
                int c = currentColor;
                delegate.color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF);
            } else {
                delegate.color(r, g, b, a);
            }
            return this;
        }

        @Override public VertexConsumer color(int argb) { delegate.color(currentColor != 0 ? currentColor : argb); return this; }
        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(MatrixStack.Entry entry, float x, float y, float z) { delegate.normal(entry, x, y, z); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public VertexConsumer lineWidth(float w) { delegate.lineWidth(w); return this; }

        @Override
        public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                           float nx, float ny, float nz) {
            computeColor(x, y, z);
            delegate.vertex(x, y, z, currentColor, u, v, overlay, light, nx, ny, nz);
        }
    }

    // ================ helpers ================

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EntityRenderState stateFor(EntityRenderer renderer, Entity entity, float tick) {
        EntityRenderState state = STATES.get(renderer);
        if (state == null) {
            state = renderer.createRenderState();
            STATES.put(renderer, state);
        }
        renderer.updateRenderState(entity, state, tick);
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void drawColored(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                    LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer,
                                    float tick, int tint, float grow) {
        EntityModel model = renderer.getModel();
        boolean babyMesh = false;
        if (state.baby && renderer instanceof AgeableModelAccessor access && access.bfb$babyModel() != null) {
            model = access.bfb$babyModel();
            babyMesh = true;
        }
        if (model == null) return;
        model.setAngles(state);
        Vec3d pos = entity.getLerpedPos(tick);
        float scale = state.baseScale * grow;
        if (!babyMesh && state.ageScale > 0.01f && state.ageScale < 0.99f) scale *= state.ageScale;
        if (scale < 0.01f || Float.isNaN(scale) || Float.isInfinite(scale)) return;
        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.scale(scale, scale, scale);
        if (!state.isInPose(EntityPose.SLEEPING)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.bodyYaw));
        }
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0f, -1.501f, 0f);
        VertexConsumer buffer = consumers.getBuffer(BfbRenderLayers.chamsColor());
        int overlay = LivingEntityRenderer.getOverlay(state, 0f);
        model.render(matrices, buffer, 0xF000F0, overlay, tint);
        matrices.pop();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void drawTextured(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                     LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer,
                                     float tick, Identifier texture, int tint, float grow) {
        EntityModel model = renderer.getModel();
        boolean babyMesh = false;
        if (state.baby && renderer instanceof AgeableModelAccessor access && access.bfb$babyModel() != null) {
            model = access.bfb$babyModel();
            babyMesh = true;
        }
        if (model == null || texture == null) return;
        model.setAngles(state);
        Vec3d pos = entity.getLerpedPos(tick);
        float scale = state.baseScale * grow;
        if (!babyMesh && state.ageScale > 0.01f && state.ageScale < 0.99f) scale *= state.ageScale;
        if (scale < 0.01f || Float.isNaN(scale) || Float.isInfinite(scale)) return;
        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.scale(scale, scale, scale);
        if (!state.isInPose(EntityPose.SLEEPING)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.bodyYaw));
        }
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0f, -1.501f, 0f);
        VertexConsumer buffer = consumers.getBuffer(BfbRenderLayers.chams(texture));
        int overlay = LivingEntityRenderer.getOverlay(state, 0f);
        model.render(matrices, buffer, 0xF000F0, overlay, tint);
        matrices.pop();
    }
}