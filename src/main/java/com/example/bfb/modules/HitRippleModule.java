package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudStyle;
import com.example.bfb.RenderUtil;
import com.example.bfb.gui.CircleTextureCache;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HitRippleModule extends CheatModule {
    public final NumberSetting lifetime = add(new NumberSetting("Lifetime", 20, 8, 60, 1));
    public final NumberSetting startSize = add(new NumberSetting("Start size", 0.2, 0.05, 1.0, 0.05));
    public final NumberSetting endSize = add(new NumberSetting("End size", 1.8, 0.5, 3.0, 0.1));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", 2.0, 1.0, 5.0, 0.5));
    public final NumberSetting killSizeMul = add(new NumberSetting("Kill size x", 1.6, 1.0, 3.0, 0.1));

    public final ColorSetting colorHit = add(new ColorSetting("Hit color", 0xFFFF5A6A));
    public final ColorSetting colorKill = add(new ColorSetting("Kill color", 0xFFFF2233));

    public final BooleanSetting onKill = add(new BooleanSetting("Kill bigger", true));
    public final BooleanSetting centerDot = add(new BooleanSetting("Center dot on kill", true));
    public final BooleanSetting gradient = add(new BooleanSetting("Gradient ring", true));

    private static final List<Ripple> RIPPLES = new ArrayList<>();
    private static final Map<Integer, Float> LAST_HP = new HashMap<>();

    public HitRippleModule() {
        super("HitRipple", "Ring wave at hit point", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        RIPPLES.clear();
        LAST_HP.clear();
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        if (client.world == null || client.player == null) return;

        for (LivingEntity entity : com.example.bfb.EntityCache.living()) {
            if (entity == client.player) continue;
            float hp = entity.getHealth();
            Float prev = LAST_HP.get(entity.getId());
            if (prev != null && hp < prev) {
                float dmg = prev - hp;
                if (dmg > 0.05f) {
                    boolean kill = !entity.isAlive() || hp <= 0;
                    RIPPLES.add(new Ripple(entity, kill));
                }
            }
            LAST_HP.put(entity.getId(), hp);
        }

        LAST_HP.keySet().removeIf(id -> client.world.getEntityById(id) == null);

        int life = lifetime.getInt();
        Iterator<Ripple> it = RIPPLES.iterator();
        while (it.hasNext()) {
            Ripple r = it.next();
            r.age++;
            if (r.age >= life) it.remove();
        }
    }

    public static void draw(DrawContext context, MinecraftClient client, float tickDelta) {
        HitRippleModule mod = com.example.bfb.ModuleManager.get(HitRippleModule.class);
        if (mod == null || !mod.isEnabled()) return;

        int life = Math.max(1, mod.lifetime.getInt());
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        for (Ripple r : RIPPLES) {
            if (r.entity.isRemoved()) continue;
            float t = r.age / (float) life;
            float sizeMul = (r.kill && mod.onKill.get()) ? mod.killSizeMul.getFloat() : 1f;

            double radius = (mod.startSize.get() + (mod.endSize.get() - mod.startSize.get()) * t) * sizeMul;
            int alpha = (int) (220 * (1 - t) * (1 - t));
            if (alpha < 5) continue;

            Vec3d centerWorld = r.entity.getLerpedPos(tickDelta)
                    .add(0, r.entity.getHeight() * 0.5, 0);
            Vec3d screen = RenderUtil.toScreenFixedFov(centerWorld);
            if (screen == null) continue;
            if (screen.x < -100 || screen.x > sw + 100 || screen.y < -100 || screen.y > sh + 100) continue;

            // проекция радиуса в пиксели
            Vec3d rightWorld = centerWorld.add(radius, 0, 0);
            Vec3d rightScreen = RenderUtil.toScreenFixedFov(rightWorld);
            if (rightScreen == null) continue;
            double pixelRadius = Math.abs(rightScreen.x - screen.x);
            int diameter = (int) (pixelRadius * 2);
            if (diameter < 6) continue;

            int baseColor = (r.kill ? mod.colorKill.getRgb() : mod.colorHit.getRgb()) & 0xFFFFFF;
            int thick = Math.max(1, (int) Math.round(mod.thickness.getFloat() * (1 - t * 0.5)));

            int cx = (int) screen.x;
            int cy = (int) screen.y;
            int drawX = cx - diameter / 2;
            int drawY = cy - diameter / 2;

            int ringColor;
            if (mod.gradient.get()) {
                int blended = HudStyle.lerpColor(baseColor, 0xFFFFFF, 0.3f);
                ringColor = (alpha << 24) | (blended & 0xFFFFFF);
            } else {
                ringColor = (alpha << 24) | baseColor;
            }

            Identifier ring = CircleTextureCache.outline(diameter, thick);
            context.drawTexture(RenderPipelines.GUI_TEXTURED,
                    ring, drawX, drawY, 0f, 0f, diameter, diameter, diameter, diameter, diameter, diameter,
                    ringColor);

            if (r.kill && mod.onKill.get() && mod.centerDot.get()) {
                int dotColor = (alpha << 24) | baseColor;
                context.fill(cx - 3, cy - 3, cx + 3, cy + 3, dotColor);
                context.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
            }
        }
    }

    private static final class Ripple {
        final LivingEntity entity;
        final boolean kill;
        int age;
        Ripple(LivingEntity entity, boolean kill) {
            this.entity = entity;
            this.kill = kill;
        }
    }
}