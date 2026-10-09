package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.HudStyle;
import com.example.bfb.Lang;
import com.example.bfb.SalFont;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DurabilityModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "TopLeft",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));
    public final NumberSetting warn = add(new NumberSetting("Warn %", 25, 1, 99, 1));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 4, 0, 10, 1));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 2, 0, 5, 1));
    public final NumberSetting headerH = add(new NumberSetting("Header height", 14, 10, 24, 1));
    public final BooleanSetting showBar = add(new BooleanSetting("Show bar", true));
    public final BooleanSetting showPercent = add(new BooleanSetting("Show percent", true));

    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0xE0100A14));
    public final ColorSetting nameColor = add(new ColorSetting("Name color", 0xFFFFFF));
    public final ColorSetting goodColor = add(new ColorSetting("Good color", 0x55FF55));
    public final ColorSetting warnColor = add(new ColorSetting("Warn color", 0xFFC14D));
    public final ColorSetting lowColor = add(new ColorSetting("Low color", 0xFF5A6A));

    public DurabilityModule() {
        super("Durability", "Armor and tool durability with icons", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }

    public static void draw(DrawContext context, MinecraftClient client) {
        DurabilityModule mod = com.example.bfb.ModuleManager.get(DurabilityModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;

        PlayerEntity player = client.player;
        List<ItemStack> items = new ArrayList<>();
        addIf(items, player.getMainHandStack());
        addIf(items, player.getEquippedStack(EquipmentSlot.HEAD));
        addIf(items, player.getEquippedStack(EquipmentSlot.CHEST));
        addIf(items, player.getEquippedStack(EquipmentSlot.LEGS));
        addIf(items, player.getEquippedStack(EquipmentSlot.FEET));
        addIf(items, player.getEquippedStack(EquipmentSlot.OFFHAND));
        if (items.isEmpty()) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        float scale = mod.scale.getFloat();
        int iconSize = 18;
        int radius = mod.radius.getInt();
        int edgeW = mod.edgeWidth.getInt();
        int headerH = mod.headerH.getInt();
        int c1 = mod.edgeColor1.getRgb() & 0xFFFFFF;
        int c2 = mod.edgeColor2.getRgb() & 0xFFFFFF;
        int bg = mod.bgColor.getRgb();
        int nameArgb = 0xFF000000 | (mod.nameColor.getRgb() & 0xFFFFFF);

        int lineH = SalFont.height();
        int rowH = iconSize + 6;
        int contentW = 6 + iconSize + 6 + 100 + 6;
        int boxW = contentW;
        int boxH = headerH + items.size() * rowH + 4;
        int totalW = (int) (boxW * scale);
        int totalH = (int) (boxH * scale);

        int defX, defY;
        switch (mod.position.get()) {
            case "TopRight" -> { defX = sw - totalW - 8; defY = 8; }
            case "BottomLeft" -> { defX = 8; defY = sh - totalH - 8; }
            case "BottomRight" -> { defX = sw - totalW - 8; defY = sh - totalH - 8; }
            default -> { defX = 8; defY = 8; }
        }
        int baseX = HudEditor.getX("durability", defX);
        int baseY = HudEditor.getY("durability", defY);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);

        HudStyle.panel(context, 0, 0, boxW, boxH, radius, edgeW, c1, c2, bg, true);
        HudStyle.header(context, 0, 0, boxW, headerH, c1, c2, Lang.tr("Durability"), 0xFFFFFF);

        int y = headerH + 2;
        for (ItemStack stack : items) {
            if (stack.isEmpty() || stack.getMaxDamage() <= 0) {
                y += rowH;
                continue;
            }

            int left = stack.getMaxDamage() - stack.getDamage();
            int max = stack.getMaxDamage();
            float ratio = max > 0 ? (float) left / max : 1f;
            float percent = ratio * 100f;

            int color;
            if (percent <= mod.warn.get()) color = mod.lowColor.getRgb() & 0xFFFFFF;
            else if (percent <= mod.warn.get() * 2) color = mod.warnColor.getRgb() & 0xFFFFFF;
            else color = mod.goodColor.getRgb() & 0xFFFFFF;

            // === иконка предмета ===
            context.drawItem(stack, 6, y + 1);

            // === имя + процент ===
            String name = stack.getName().getString();
            int percentW = mod.showPercent.get() ? SalFont.width(Math.round(percent) + "%") : 0;
            int nameMaxW = boxW - 12 - iconSize - 8 - percentW;
            name = SalFont.fit(name, nameMaxW);
            SalFont.draw(context, name, 6 + iconSize + 6, y + 1, nameArgb);
            if (mod.showPercent.get()) {
                String pct = Math.round(percent) + "%";
                int pw = SalFont.width(pct);
                SalFont.draw(context, pct, boxW - 6 - pw, y + 1, 0xFF000000 | color);
            }

            // === полоса прогресса ===
            if (mod.showBar.get()) {
                int barX = 6 + iconSize + 6;
                int barY = y + 1 + lineH + 2;
                int barW = boxW - barX - 6;
                int barH = 2;
                // фон
                context.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0E1E);
                // заполнение с градиентом
                int fillW = (int) (barW * ratio);
                if (fillW > 0) {
                    context.fillGradient(barX, barY, barX + fillW, barY + barH,
                            0xFF000000 | HudStyle.darken(color, 0.55f),
                            0xFF000000 | color);
                }
            }

            y += rowH;
        }

        matrices.popMatrix();
        HudEditor.outline(context, "durability", baseX, baseY, totalW, totalH);
    }

    private static void addIf(List<ItemStack> list, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (stack.getMaxDamage() <= 0) return;
        list.add(stack);
    }
}