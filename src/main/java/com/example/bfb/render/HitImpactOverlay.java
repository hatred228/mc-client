package com.example.bfb.render;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.HitImpactModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Flash при ударе — рисуем полноэкранный слой поверх HUD.
 * Вызывается из HudRenderCallback (регистрация в BfbMod).
 */
public final class HitImpactOverlay {

    private HitImpactOverlay() {}

    public static void render(DrawContext ctx) {
        HitImpactModule mod = ModuleManager.get(HitImpactModule.class);
        if (mod == null || !mod.isEnabled() || !mod.flash.get()) return;

        int color = HitImpactModule.flashColorArgb();
        if ((color >>> 24) == 0) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // рисуем рамку-виньетку по краям, не заливая весь экран
        int border = 8;
        ctx.fill(0, 0, w, border, color);
        ctx.fill(0, h - border, w, h, color);
        ctx.fill(0, border, border, h - border, color);
        ctx.fill(w - border, border, w, h - border, color);
    }
}