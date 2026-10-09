package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudStyle;
import com.example.bfb.SalFont;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.example.bfb.Lang;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PotionsModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "TopLeft",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));

    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.1));
    public final NumberSetting iconSize = add(new NumberSetting("Icon size", 18, 12, 32, 1));
    public final NumberSetting spacing = add(new NumberSetting("Spacing", 3, 1, 12, 1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 4, 0, 10, 1));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 2, 0, 5, 1));
    public final NumberSetting headerH = add(new NumberSetting("Header height", 14, 10, 24, 1));

    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0xE0100A14));
    public final ColorSetting nameColor = add(new ColorSetting("Name color", 0xFFFFFF));
    public final ColorSetting timeColor = add(new ColorSetting("Time color", 0xD8C0A0));

    public PotionsModule() {
        super("Potions", "Celestial-style potion HUD", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client) {
        PotionsModule mod = com.example.bfb.ModuleManager.get(PotionsModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;

        List<StatusEffectInstance> effects = new ArrayList<>(client.player.getStatusEffects());
        if (effects.isEmpty()) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        float scale = mod.scale.getFloat();
        int iconSize = mod.iconSize.getInt();
        int spacing = mod.spacing.getInt();
        int radius = mod.radius.getInt();
        int edgeW = mod.edgeWidth.getInt();
        int headerH = mod.headerH.getInt();
        int c1 = mod.edgeColor1.getRgb() & 0xFFFFFF;
        int c2 = mod.edgeColor2.getRgb() & 0xFFFFFF;
        int bg = mod.bgColor.getRgb();
        int nameArgb = 0xFF000000 | (mod.nameColor.getRgb() & 0xFFFFFF);
        int timeArgb = 0xFF000000 | (mod.timeColor.getRgb() & 0xFFFFFF);

        int lineH = SalFont.height();
        int textW = 90;
        int rowH = iconSize + spacing;
        int contentW = 4 + iconSize + 6 + textW + 4;
        int boxW = contentW;
        int boxH = headerH + effects.size() * rowH + 4;
        int totalW = (int) (boxW * scale);
        int totalH = (int) (boxH * scale);

        int defX, defY;
        switch (mod.position.get()) {
            case "TopRight" -> { defX = sw - totalW - 8; defY = 8; }
            case "BottomLeft" -> { defX = 8; defY = sh - totalH - 8; }
            case "BottomRight" -> { defX = sw - totalW - 8; defY = sh - totalH - 8; }
            default -> { defX = 8; defY = 8; }
        }
        int baseX = com.example.bfb.HudEditor.getX("potions", defX);
        int baseY = com.example.bfb.HudEditor.getY("potions", defY);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);

        HudStyle.panel(context, 0, 0, boxW, boxH, radius, edgeW, c1, c2, bg, true);
        HudStyle.header(context, 0, 0, boxW, headerH, c1, c2, Lang.tr("Potions"), 0xFFFFFF);

        int y = headerH + 2;
        for (StatusEffectInstance effect : effects) {
            Identifier id = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
            if (id != null) {
                Identifier sprite = Identifier.of(id.getNamespace(), "mob_effect/" + id.getPath());
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, 4, y, iconSize, iconSize);
            }

            String name = effect.getEffectType().value().getName().getString();
            int amp = effect.getAmplifier() + 1;
            if (amp > 1) name = name + " " + roman(amp);
            String time = effect.isInfinite() ? "∞" : formatTime(effect.getDuration());

            SalFont.draw(context, name, 4 + iconSize + 6, y + 1, nameArgb);
            SalFont.draw(context, time, 4 + iconSize + 6, y + 1 + lineH, timeArgb);

            // === полоса оставшегося времени (против 30 сек максимум) ===
            if (!effect.isInfinite()) {
                float ratio = Math.min(1f, effect.getDuration() / (30f * 20f));
                int barY2 = y + iconSize - 1;
                context.fill(4, barY2, 4 + iconSize, barY2 + 2, 0xFF1A0E1E);
                int fill = (int) (iconSize * ratio);
                if (fill > 0) {
                    context.fillGradient(4, barY2, 4 + fill, barY2 + 2,
                            0xFF000000 | HudStyle.darken(c2, 0.4f), 0xFF000000 | c2);
                }
            }

            y += rowH;
        }

        matrices.popMatrix();
        com.example.bfb.HudEditor.outline(context, "potions", baseX, baseY, totalW, totalH);
    }

    private static String formatTime(int ticks) {
        int sec = ticks / 20;
        int min = sec / 60;
        int s = sec % 60;
        return min > 0 ? String.format("%d:%02d", min, s) : s + "s";
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n <= 10 ? r[n] : Integer.toString(n);
    }
}