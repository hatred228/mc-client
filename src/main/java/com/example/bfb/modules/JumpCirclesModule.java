package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JumpCirclesModule extends CheatModule {
    public final NumberSetting lifetime = add(new NumberSetting("Lifetime", 20, 5, 60, 1));
    public final NumberSetting startRadius = add(new NumberSetting("Start radius", 0.2, 0.05, 1.0, 0.05));
    public final NumberSetting endRadius = add(new NumberSetting("End radius", 1.6, 0.5, 3.0, 0.1));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", 2.0, 1.0, 4.0, 0.5));
    public final ColorSetting color = add(new ColorSetting("Color", 0x7C5CFF));
    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow speed", 1.0, 0.2, 4.0, 0.1));
    public final BooleanSetting self = add(new BooleanSetting("Self", true));

    private static final List<Circle> CIRCLES = new ArrayList<>();
    private final Map<Integer, Boolean> onGroundState = new HashMap<>();

    public JumpCirclesModule() {
        super("JumpCircles", "Rings appear when players jump", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        CIRCLES.clear();
        onGroundState.clear();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (client.world == null || client.player == null) return;

        for (PlayerEntity p : client.world.getPlayers()) {
            if (!self.get() && p == client.player) continue;
            boolean onGround = p.isOnGround();
            Boolean prev = onGroundState.get(p.getId());
            if (prev != null && prev && !onGround) {
                CIRCLES.add(new Circle(p.getX(), p.getY(), p.getZ()));
            }
            onGroundState.put(p.getId(), onGround);
        }

        int life = lifetime.getInt();
        Iterator<Circle> it = CIRCLES.iterator();
        while (it.hasNext()) {
            Circle c = it.next();
            c.age++;
            if (c.age >= life) it.remove();
        }
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        int life = Math.max(1, lifetime.getInt());
        double sr = startRadius.get();
        double er = endRadius.get();
        float th = thickness.getFloat();
        int rgb = rainbow.get() ? RenderUtil.rainbow(rainbowSpeed.get()) : color.getRgb();

        for (Circle c : CIRCLES) {
            float t = Math.min(1f, c.age / (float) life);
            double r = sr + (er - sr) * t;
            double fade = (1 - t) * (1 - t);
            int alpha = (int) (220 * fade);
            if (alpha <= 5) continue;

            Vec3d base = new Vec3d(c.x, c.y + 0.05, c.z);

            // внешнее тонкое кольцо чуть больше — создаёт эффект glow
            int glowAlpha = alpha / 3;
            if (glowAlpha > 3) {
                drawRing(matrices, consumers, camera, base, r * 1.08, (glowAlpha << 24) | rgb, 64, th * 0.7f);
            }
            // внутреннее основное кольцо толще
            drawRing(matrices, consumers, camera, base, r, (alpha << 24) | rgb, 64, th);
        }
    }

    private static void drawRing(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                                 Vec3d base, double radius, int argb, int segments, float width) {
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            RenderUtil.bone(matrices, consumers, camera,
                    base.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius),
                    base.add(Math.cos(a2) * radius, 0, Math.sin(a2) * radius),
                    argb, width);
        }
    }

    private static final class Circle {
        final double x, y, z;
        int age;
        Circle(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }
}