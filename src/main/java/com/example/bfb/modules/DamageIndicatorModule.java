package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.SalFont;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DamageIndicatorModule extends CheatModule {
    public final NumberSetting lifetime = add(new NumberSetting("Lifetime", 30, 10, 80, 1));
    public final NumberSetting riseSpeed = add(new NumberSetting("Rise speed", 0.6, 0.1, 2.0, 0.1));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.1));
    public final NumberSetting spawnOffset = add(new NumberSetting("Spawn Y offset", 0.3, 0.0, 1.5, 0.1));

    public final ColorSetting colorNormal = add(new ColorSetting("Normal color", 0xFFFFFF));
    public final ColorSetting colorMedium = add(new ColorSetting("Medium color", 0xFFB347));
    public final ColorSetting colorCrit = add(new ColorSetting("Crit color", 0xFF3B30));
    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));

    public final BooleanSetting showDecimals = add(new BooleanSetting("Decimals", true));
    public final BooleanSetting critColor = add(new BooleanSetting("Damage colors", true));

    private static final List<Indicator> INDICATORS = new ArrayList<>();
    private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();

    public DamageIndicatorModule() {
        super("DamageIndicator", "Floating damage numbers", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        INDICATORS.clear();
        LAST_HEALTH.clear();
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        if (client.world == null) return;

        for (LivingEntity entity : com.example.bfb.EntityCache.living()) {
            if (entity == client.player) continue;
            float hp = entity.getHealth();
            Float prev = LAST_HEALTH.get(entity.getId());
            if (prev != null && hp < prev) {
                float dmg = prev - hp;
                if (dmg > 0.05f) {
                    INDICATORS.add(new Indicator(entity, dmg));
                }
            }
            LAST_HEALTH.put(entity.getId(), hp);
        }

        LAST_HEALTH.keySet().removeIf(id -> client.world.getEntityById(id) == null);

        int life = lifetime.getInt();
        Iterator<Indicator> it = INDICATORS.iterator();
        while (it.hasNext()) {
            Indicator ind = it.next();
            ind.age++;
            if (ind.age >= life) it.remove();
        }
    }

    public static void draw(DrawContext context, MinecraftClient client, float tickDelta) {
        DamageIndicatorModule mod = com.example.bfb.ModuleManager.get(DamageIndicatorModule.class);
        if (mod == null || !mod.isEnabled() || client.world == null || client.player == null) return;

        int life = Math.max(1, mod.lifetime.getInt());
        float baseScale = mod.scale.getFloat();
        for (Indicator ind : INDICATORS) {
            if (ind.entity.isRemoved() || !ind.entity.isAlive()) continue;
            float t = ind.age / (float) life;
            double rise = t * mod.riseSpeed.get();

            Vec3d base = ind.entity.getLerpedPos(tickDelta)
                    .add(0, ind.entity.getHeight() + mod.spawnOffset.get() + rise, 0);
            Vec3d screen = com.example.bfb.RenderUtil.toScreenFixedFov(base);
            if (screen == null) continue;
            if (screen.x < -100 || screen.x > context.getScaledWindowWidth() + 100) continue;
            if (screen.y < -100 || screen.y > context.getScaledWindowHeight() + 100) continue;

            int alpha = (int) (255 * (1 - t));
            alpha = Math.max(0, Math.min(255, alpha));

            int rgb = mod.colorNormal.getRgb() & 0xFFFFFF;
            if (mod.critColor.get()) {
                if (ind.damage > 4.0f) rgb = mod.colorCrit.getRgb() & 0xFFFFFF;
                else if (ind.damage > 2.0f) rgb = mod.colorMedium.getRgb() & 0xFFFFFF;
            }

            String text = mod.showDecimals.get()
                    ? "-" + String.format("%.1f", ind.damage)
                    : "-" + Math.round(ind.damage);

            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate((float) screen.x, (float) screen.y);
            matrices.scale(baseScale, baseScale);

            int w = SalFont.width(text);

            if (mod.shadow.get()) {
                SalFont.draw(context, text, -w / 2 + 1, 1, (alpha / 2) << 24);
            }
            SalFont.draw(context, text, -w / 2, 0, (alpha << 24) | rgb);

            matrices.popMatrix();
        }
    }

    private static final class Indicator {
        final LivingEntity entity;
        final float damage;
        int age;
        Indicator(LivingEntity entity, float damage) {
            this.entity = entity;
            this.damage = damage;
        }
    }
}