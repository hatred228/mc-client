package com.example.bfb.hud.widgets;

import com.example.bfb.CombatUtil;
import com.example.bfb.EntityCache;
import com.example.bfb.RenderUtil;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.modules.VisualsModule;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class TracersWidget extends HudWidget {

    public TracersWidget() {
        super("Tracers", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null || mc.world == null) { width = height = 0; return; }

        VisualsModule vm = VisualsModule.get();
        if (vm != null && !vm.tracers.bool("Enabled")) { width = height = 0; return; }

        double RANGE = vm != null ? vm.tracers.num("Range") : 64.0;
        float WIDTH = vm != null ? vm.tracers.num("Width") : 1.5f;
        String ORIGIN = vm != null ? vm.tracers.mode("Origin") : "Crosshair";
        boolean GRADIENT = vm == null || vm.tracers.bool("Gradient");
        int COLOR_FROM = vm != null ? vm.tracers.colorRgb("Color from") : 0x9C5CFF;
        int COLOR_TO = vm != null ? vm.tracers.colorRgb("Color to") : 0x4CC3FF;
        boolean FILT_P = vm == null || vm.tracers.bool("Players");
        boolean FILT_M = vm == null || vm.tracers.bool("Mobs");
        boolean FILT_A = vm != null && vm.tracers.bool("Animals");

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int startX = sw / 2;
        int startY = switch (ORIGIN) {
            case "Bottom" -> sh - 1;
            case "Top" -> 0;
            default -> sh / 2;
        };

        float tick = mc.getRenderTickCounter().getTickProgress(true);
        double reachSq = RANGE * RANGE;

        for (LivingEntity entity : EntityCache.living()) {
            if (entity == mc.player) continue;
            if (!CombatUtil.accept(entity, mc.player, FILT_P, FILT_M, FILT_A)) continue;
            if (entity.squaredDistanceTo(mc.player) > reachSq) continue;

            Vec3d world = entity.getLerpedPos(tick).add(0, entity.getHeight() * 0.5, 0);
            Vec3d screen = RenderUtil.toScreenFixedFov(world);
            if (screen == null) continue;
            if (screen.x < -50 || screen.x > sw + 50) continue;
            if (screen.y < -50 || screen.y > sh + 50) continue;

            int endX = (int) Math.max(0, Math.min(sw - 1, screen.x));
            int endY = (int) Math.max(0, Math.min(sh - 1, screen.y));

            int rgb = COLOR_FROM;
            if (entity.getMaxHealth() > 0) {
                float ratio = Math.max(0f, Math.min(1f,
                        entity.getHealth() / entity.getMaxHealth()));
                int red = (int) ((1f - ratio) * 255);
                int green = (int) (ratio * 255);
                rgb = (red << 16) | (green << 8);
            }

            if (GRADIENT) {
                int start = 0xCC000000 | COLOR_FROM;
                int end = 0xCC000000 | rgb;
                RenderUtil.gradientLine(ctx, startX, startY, endX, endY, start, end, 1);
            } else {
                RenderUtil.screenLine(ctx, startX, startY, endX, endY, 0xCC000000 | rgb, 1);
            }
        }

        width = 0;
        height = 0;
    }
}