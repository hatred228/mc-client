package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static final class Projection {
        public final int screenW;
        public final int screenH;
        public final Vec3d camPos;
        public final Quaternionf cameraRot;
        public final double fovScale;
        public final boolean valid;

        private Projection(int screenW, int screenH, Vec3d camPos, Quaternionf cameraRot, double fovScale, boolean valid) {
            this.screenW = screenW;
            this.screenH = screenH;
            this.camPos = camPos;
            this.cameraRot = cameraRot;
            this.fovScale = fovScale;
            this.valid = valid;
        }
    }

    private static java.lang.reflect.Method fovMethod;

    public static void invalidateProjection() {
    }

    public static Projection snapshot() {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        if (!camera.isReady()) return null;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        Vec3d camPos = camera.getCameraPos();
        Quaternionf q = new Quaternionf(camera.getRotation()).conjugate();
        double fov = fov(camera);
        double scale = screenH / (2.0 * Math.tan(Math.toRadians(fov) / 2.0));
        return new Projection(screenW, screenH, camPos, q, scale, true);
    }

    public static Vec3d project(Projection p, Vec3d world) {
        if (p == null || !p.valid) return null;
        Vector3f delta = new Vector3f(
                (float) (world.x - p.camPos.x),
                (float) (world.y - p.camPos.y),
                (float) (world.z - p.camPos.z));
        delta.rotate(p.cameraRot);
        if (delta.z >= 0f) return null;
        double depth = -delta.z;
        double sx = p.screenW / 2.0 + delta.x * p.fovScale / depth;
        double sy = p.screenH / 2.0 - delta.y * p.fovScale / depth;
        return new Vec3d(sx, sy, depth);
    }

    public static Vec3d toScreen(Vec3d world) {
        return project(snapshot(), world);
    }

    /**
     * Проекция через матрицы вида и проекции — не дрожит при зуме и спринте.
     */
    public static Vec3d toScreenFixedFov(Vec3d world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;
        Camera camera = client.gameRenderer.getCamera();
        if (!camera.isReady()) return null;

        Vec3d camPos = camera.getCameraPos();

        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.rotateX((float) Math.toRadians(camera.getPitch()));
        viewMatrix.rotateY((float) Math.toRadians(camera.getYaw() + 180.0f));
        viewMatrix.translate(
                (float) -camPos.x,
                (float) -camPos.y,
                (float) -camPos.z
        );

        Matrix4f projectionMatrix = client.gameRenderer.getBasicProjectionMatrix(
                client.options.getFov().getValue()
        );

        Matrix4f combined = new Matrix4f(projectionMatrix).mul(viewMatrix);

        Vector4f vec = new Vector4f(
                (float) world.x,
                (float) world.y,
                (float) world.z,
                1.0f
        );
        vec.mul(combined);

        if (vec.w <= 0.0f) return null;
        float ndcX = vec.x / vec.w;
        float ndcY = vec.y / vec.w;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        double sx = (ndcX + 1.0) * 0.5 * screenW;
        double sy = (1.0 - ndcY) * 0.5 * screenH;

        return new Vec3d(sx, sy, vec.w);
    }

    private static double fov(Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            if (fovMethod == null) {
                var m = net.minecraft.client.render.GameRenderer.class.getDeclaredMethod(
                        "getFov", Camera.class, float.class, boolean.class);
                m.setAccessible(true);
                fovMethod = m;
            }
            return (double) fovMethod.invoke(client.gameRenderer, camera,
                    client.getRenderTickCounter().getTickProgress(false), true);
        } catch (Exception e) {
            return client.options.getFov().getValue();
        }
    }

    public static int rainbow(double speed) {
        float hue = (System.currentTimeMillis() % 8000L) / 8000f * (float) Math.max(0.2, speed);
        hue -= (float) Math.floor(hue);
        return java.awt.Color.HSBtoRGB(hue, 0.82f, 1f) & 0xFFFFFF;
    }

    // ===================== OUTLINE / CORNERS =====================

    public static void outline(MatrixStack matrices, VertexConsumerProvider consumers, Box box, Vec3d camera, int color, float width) {
        double x1 = box.minX - camera.x;
        double y1 = box.minY - camera.y;
        double z1 = box.minZ - camera.z;
        double x2 = box.maxX - camera.x;
        double y2 = box.maxY - camera.y;
        double z2 = box.maxZ - camera.z;
        line(matrices, consumers, x1, y1, z1, x2, y1, z1, color, width);
        line(matrices, consumers, x2, y1, z1, x2, y1, z2, color, width);
        line(matrices, consumers, x2, y1, z2, x1, y1, z2, color, width);
        line(matrices, consumers, x1, y1, z2, x1, y1, z1, color, width);
        line(matrices, consumers, x1, y2, z1, x2, y2, z1, color, width);
        line(matrices, consumers, x2, y2, z1, x2, y2, z2, color, width);
        line(matrices, consumers, x2, y2, z2, x1, y2, z2, color, width);
        line(matrices, consumers, x1, y2, z2, x1, y2, z1, color, width);
        line(matrices, consumers, x1, y1, z1, x1, y2, z1, color, width);
        line(matrices, consumers, x2, y1, z1, x2, y2, z1, color, width);
        line(matrices, consumers, x2, y1, z2, x2, y2, z2, color, width);
        line(matrices, consumers, x1, y1, z2, x1, y2, z2, color, width);
    }

    /** Стандартные углы — длина фиксированная (0.28 от ребра). */
    public static void corners(MatrixStack matrices, VertexConsumerProvider consumers, Box box, Vec3d camera, int color, float width) {
        corners(matrices, consumers, box, camera, color, width, 0.28f);
    }

    /** Углы с настраиваемой длиной: fraction ∈ [0.02..0.5] — доля от ребра. */
    public static void corners(MatrixStack matrices, VertexConsumerProvider consumers, Box box, Vec3d camera,
                               int color, float width, float fraction) {
        double x1 = box.minX - camera.x;
        double y1 = box.minY - camera.y;
        double z1 = box.minZ - camera.z;
        double x2 = box.maxX - camera.x;
        double y2 = box.maxY - camera.y;
        double z2 = box.maxZ - camera.z;
        double[][] corners = {
                {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
                {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        float frac = Math.max(0.02f, Math.min(0.5f, fraction));
        for (int[] edge : edges) {
            double[] a = corners[edge[0]];
            double[] b = corners[edge[1]];
            stub(matrices, consumers, a, b, color, width, frac);
            stub(matrices, consumers, b, a, color, width, frac);
        }
    }

    /** Старый stub — фиксированная доля 0.28 (для обратной совместимости). */
    private static void stub(MatrixStack matrices, VertexConsumerProvider consumers, double[] from, double[] to, int color, float width) {
        stub(matrices, consumers, from, to, color, width, 0.28f);
    }

    private static void stub(MatrixStack matrices, VertexConsumerProvider consumers, double[] from, double[] to,
                             int color, float width, float fraction) {
        double x = from[0] + (to[0] - from[0]) * fraction;
        double y = from[1] + (to[1] - from[1]) * fraction;
        double z = from[2] + (to[2] - from[2]) * fraction;
        line(matrices, consumers, from[0], from[1], from[2], x, y, z, color, width);
    }

    // ===================== SCREEN-SPACE =====================

    public static int[] screenRect(Box box) {
        Projection p = snapshot();
        if (p == null) return null;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        int seen = 0;
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    Vec3d screen = project(p, new Vec3d(
                            xi == 0 ? box.minX : box.maxX,
                            yi == 0 ? box.minY : box.maxY,
                            zi == 0 ? box.minZ : box.maxZ));
                    if (screen == null) continue;
                    seen++;
                    minX = Math.min(minX, screen.x);
                    minY = Math.min(minY, screen.y);
                    maxX = Math.max(maxX, screen.x);
                    maxY = Math.max(maxY, screen.y);
                }
            }
        }
        if (seen < 4) return null;
        return new int[]{(int) minX, (int) minY, (int) maxX, (int) maxY};
    }

    public static boolean inFront(Camera camera, Vec3d target) {
        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        Vec3d origin = camera.getCameraPos();
        return (target.x - origin.x) * fx + (target.y - origin.y) * fy + (target.z - origin.z) * fz > 0.0;
    }

    public static void screenLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.5f) return;
        int half = Math.max(1, thickness);
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x1, y1);
        matrices.rotate((float) Math.atan2(dy, dx));
        context.fill(0, -half / 2, (int) Math.ceil(length), half / 2 + 1, color);
        matrices.popMatrix();
    }

    /** Линия с линейным градиентом цвета от start к end. */
    public static void gradientLine(DrawContext context, int x1, int y1, int x2, int y2,
                                    int colorStart, int colorEnd, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.5f) return;
        int half = Math.max(1, thickness);
        int steps = Math.max(4, Math.min(24, (int) (length / 40f)));
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x1, y1);
        matrices.rotate((float) Math.atan2(dy, dx));
        float stepLen = length / steps;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            int color = lerpArgb(colorStart, colorEnd, t);
            int sx = (int) (i * stepLen);
            int ex = (int) ((i + 1) * stepLen + 1);
            context.fill(sx, -half / 2, ex, half / 2 + 1, color);
        }
        matrices.popMatrix();
    }

    // ===================== WORLD-SPACE =====================

    public static void ring(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d center, Vec3d camera, double radius, int color) {
        int segments = 28;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            line(matrices, consumers,
                    center.x + Math.cos(a1) * radius - camera.x, center.y - camera.y, center.z + Math.sin(a1) * radius - camera.z,
                    center.x + Math.cos(a2) * radius - camera.x, center.y - camera.y, center.z + Math.sin(a2) * radius - camera.z,
                    color, 2f);
        }
    }

    public static void tracer(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d from, Vec3d to, Vec3d camera, int color, float width) {
        line(matrices, consumers,
                from.x - camera.x, from.y - camera.y, from.z - camera.z,
                to.x - camera.x, to.y - camera.y, to.z - camera.z,
                color, width);
    }

    public static void bone(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera, Vec3d from, Vec3d to, int color, float width) {
        tracer(matrices, consumers, from, to, camera, color, width);
    }

    public static void fill(MatrixStack matrices, VertexConsumerProvider consumers, Box box, Vec3d camera, int color) {
        float x1 = (float) (box.minX - camera.x);
        float y1 = (float) (box.minY - camera.y);
        float z1 = (float) (box.minZ - camera.z);
        float x2 = (float) (box.maxX - camera.x);
        float y2 = (float) (box.maxY - camera.y);
        float z2 = (float) (box.maxZ - camera.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(BfbRenderLayers.QUAD_LAYER);
        face(vc, mat, color, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        face(vc, mat, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        face(vc, mat, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        face(vc, mat, color, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        face(vc, mat, color, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        face(vc, mat, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    public static void triangle(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                Vec3d a, Vec3d b, Vec3d c, int color) {
        Matrix4f mat = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(BfbRenderLayers.QUAD_LAYER);
        float x1 = (float) (a.x - camera.x), y1 = (float) (a.y - camera.y), z1 = (float) (a.z - camera.z);
        float x2 = (float) (b.x - camera.x), y2 = (float) (b.y - camera.y), z2 = (float) (b.z - camera.z);
        float x3 = (float) (c.x - camera.x), y3 = (float) (c.y - camera.y), z3 = (float) (c.z - camera.z);
        vc.vertex(mat, x1, y1, z1).color(color);
        vc.vertex(mat, x2, y2, z2).color(color);
        vc.vertex(mat, x3, y3, z3).color(color);
        vc.vertex(mat, x3, y3, z3).color(color);
    }

    // ===================== INTERNALS =====================

    private static void line(MatrixStack matrices, VertexConsumerProvider consumers,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             int color, float width) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-4f) return;
        nx /= len;
        ny /= len;
        nz /= len;
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer vc = consumers.getBuffer(BfbRenderLayers.LINE_LAYER);
        vc.vertex(entry, (float) x1, (float) y1, (float) z1).color(color).normal(entry, nx, ny, nz).lineWidth(width);
        vc.vertex(entry, (float) x2, (float) y2, (float) z2).color(color).normal(entry, nx, ny, nz).lineWidth(width);
    }

    private static void face(VertexConsumer vc, Matrix4f mat, int color,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4) {
        vc.vertex(mat, x1, y1, z1).color(color);
        vc.vertex(mat, x2, y2, z2).color(color);
        vc.vertex(mat, x3, y3, z3).color(color);
        vc.vertex(mat, x4, y4, z4).color(color);
    }

    // ===================== COLOR UTILS =====================

    /** Линейная интерполяция ARGB по компонентам (не premultiplied). */
    public static int lerpArgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    /** Линейная интерполяция RGB (альфа отбрасывается). */
    public static int lerpRgb(int a, int b, float t) {
        return lerpArgb(0xFF000000 | (a & 0xFFFFFF), 0xFF000000 | (b & 0xFFFFFF), t) & 0xFFFFFF;
    }

    /** HSB → RGB без аллокаций. h,s,b ∈ [0,1]. Возвращает 0xRRGGBB. */
    public static int hsbToRgb(float h, float s, float b) {
        h = h - (float) Math.floor(h);
        float c = b * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = b - c;
        float r, g, bl;
        int sector = (int) (h * 6f);
        switch (sector) {
            case 0 -> { r = c; g = x; bl = 0f; }
            case 1 -> { r = x; g = c; bl = 0f; }
            case 2 -> { r = 0f; g = c; bl = x; }
            case 3 -> { r = 0f; g = x; bl = c; }
            case 4 -> { r = x; g = 0f; bl = c; }
            default -> { r = c; g = 0f; bl = x; }
        }
        int ri = Math.max(0, Math.min(255, (int) ((r + m) * 255f)));
        int gi = Math.max(0, Math.min(255, (int) ((g + m) * 255f)));
        int bi = Math.max(0, Math.min(255, (int) ((bl + m) * 255f)));
        return (ri << 16) | (gi << 8) | bi;
    }

    /** Утилита: применить альфу к rgb и получить argb. */
    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}