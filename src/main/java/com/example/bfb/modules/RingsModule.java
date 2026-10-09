package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RingsModule extends CheatModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", 0.5, 0.2, 1.5, 0.05));
    public final NumberSetting lifetime = add(new NumberSetting("Lifetime", 25, 5, 80, 1));
    public final NumberSetting stepDist = add(new NumberSetting("Step Distance", 0.5, 0.1, 2.0, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", 0xB04AFF));

    /** Кольца висят даже после выключения модуля, пока не дотухнут. */
    private static final List<Ring> rings = new ArrayList<>();
    private double lastX;
    private double lastZ;
    private boolean placed;

    public RingsModule() {
        super("Rings", "Footstep rings that expand and fade smoothly", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        rings.clear();
        placed = false;
    }

    @Override
    public void onDisable() {
        // кольца НЕ сбрасываем — пусть догорают, выглядит гораздо приятнее
        placed = false;
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null) return;
        int life = lifetime.getInt();
        Iterator<Ring> it = rings.iterator();
        while (it.hasNext()) {
            Ring ring = it.next();
            ring.age++;
            if (ring.age >= life) it.remove();
        }
        if (!isEnabled()) return;
        if (!player.isOnGround() || player.isSneaking()) {
            placed = false; // после прыжка новая цепочка не тянется от старого места
            return;
        }
        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        if (!placed || Math.hypot(dx, dz) >= stepDist.get()) {
            rings.add(new Ring(player.getX(), player.getY(), player.getZ()));
            lastX = player.getX();
            lastZ = player.getZ();
            placed = true;
        }
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        int life = Math.max(1, lifetime.getInt());
        for (Ring ring : rings) {
            float t = Math.min(1f, ring.age / (float) life);
            double r = radius.get() * (0.55 + t * 0.9);
            // быстрое появление + плавный ease-out отлёт (квадратичный)
            double fadeIn = Math.min(1.0, t * 6.0);
            double fadeOut = (1.0 - t) * (1.0 - t);
            int alpha = (int) (220 * fadeIn * fadeOut);
            if (alpha <= 1) continue;
            int argb = (alpha << 24) | (color.getRgb() & 0xFFFFFF);
            Vec3d base = new Vec3d(ring.x, ring.y + 0.05, ring.z);
            int seg = 24;
            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg;
                double a2 = Math.PI * 2 * (i + 1) / seg;
                RenderUtil.bone(matrices, consumers, camera,
                        base.add(Math.cos(a1) * r, 0, Math.sin(a1) * r),
                        base.add(Math.cos(a2) * r, 0, Math.sin(a2) * r), argb, 2.0f);
            }
        }
    }

    private static final class Ring {
        private final double x, y, z;
        private int age;

        private Ring(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
