package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.EntityCache;
import com.example.bfb.ModuleManager;
import com.example.bfb.RenderUtil;
import com.example.bfb.gui.GlowTextureCache;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AmbientGlowModule extends CheatModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", 40, 10, 120, 1));
    public final NumberSetting size = add(new NumberSetting("Glow size", 50, 10, 120, 2));
    public final NumberSetting alpha = add(new NumberSetting("Alpha", 100, 20, 220, 5));

    public final ColorSetting playerInner = add(new ColorSetting("Player inner", 0xFF9C5CFF));
    public final ColorSetting playerOuter = add(new ColorSetting("Player outer", 0xFF3A1F5C));
    public final ColorSetting mobInner = add(new ColorSetting("Mob inner", 0xFFFF3B30));
    public final ColorSetting mobOuter = add(new ColorSetting("Mob outer", 0xFF5C1F1F));

    public final BooleanSetting pulse = add(new BooleanSetting("Pulse", true));
    public final NumberSetting pulseSpeed = add(new NumberSetting("Pulse speed", 1.0, 0.2, 4.0, 0.1));

    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    public AmbientGlowModule() {
        super("AmbientGlow", "Soft glow around entities", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client, float tickDelta) {
        AmbientGlowModule mod = ModuleManager.get(AmbientGlowModule.class);
        if (mod == null || !mod.isEnabled() || client.world == null || client.player == null) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int baseAlpha = mod.alpha.getInt();
        double reach = mod.radius.get();
        double reachSq = reach * reach;
        float size = mod.size.getFloat();

        float pulseMul = 1f;
        if (mod.pulse.get()) {
            pulseMul = 0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() / (400.0 / mod.pulseSpeed.get()));
        }

        for (LivingEntity entity : EntityCache.living()) {
            if (entity == client.player) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isAnimal = entity instanceof net.minecraft.entity.passive.AnimalEntity;
            boolean isMob = !isPlayer && !isAnimal;

            if (isPlayer && !mod.players.get()) continue;
            if (isAnimal && !mod.animals.get()) continue;
            if (isMob && !mod.mobs.get()) continue;

            if (entity.squaredDistanceTo(client.player) > reachSq) continue;

            Vec3d anchor = entity.getLerpedPos(tickDelta).add(0, entity.getHeight() * 0.5, 0);
            Vec3d screen = RenderUtil.toScreenFixedFov(anchor);
            if (screen == null) continue;
            if (screen.x < -size || screen.x > sw + size || screen.y < -size || screen.y > sh + size) continue;

            double dist = Math.sqrt(entity.squaredDistanceTo(client.player));
            float fade = (float) Math.max(0.15, 1.0 - dist / reach);
            int a = (int) (baseAlpha * fade * pulseMul);
            if (a < 8) continue;

            int inner = (isPlayer ? mod.playerInner : mod.mobInner).getRgb() & 0xFFFFFF;
            int outer = (isPlayer ? mod.playerOuter : mod.mobOuter).getRgb() & 0xFFFFFF;

            int cx = (int) screen.x;
            int cy = (int) screen.y;
            int diameter = (int) size;
            if (diameter < 8) continue;

            int drawX = cx - diameter / 2;
            int drawY = cy - diameter / 2;

            // внешний слой — outer цвет, слабая альфа
            Identifier glow = GlowTextureCache.get(diameter);
            context.drawTexture(RenderPipelines.GUI_TEXTURED,
                    glow, drawX, drawY, 0f, 0f, diameter, diameter, diameter, diameter, diameter, diameter,
                    ((int) (a * 0.55f) << 24) | outer);

            // внутренний слой — inner цвет, чуть меньше
            int d2 = (int) (diameter * 0.7);
            int drawX2 = cx - d2 / 2;
            int drawY2 = cy - d2 / 2;
            context.drawTexture(RenderPipelines.GUI_TEXTURED,
                    glow, drawX2, drawY2, 0f, 0f, d2, d2, d2, d2, d2, d2,
                    (a << 24) | inner);
        }
    }
}