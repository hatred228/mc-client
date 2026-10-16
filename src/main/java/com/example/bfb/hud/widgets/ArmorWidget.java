package com.example.bfb.hud.widgets;

import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.hud.Notifications;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ArmorWidget extends HudWidget {

    private static final Map<EquipmentSlot, Boolean> WARNED = new HashMap<>();

    public ArmorWidget() {
        super("Armor", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null) { width = height = 0; return; }

        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        String[] names = { "Helmet", "Chestplate", "Leggings", "Boots" };

        float iconSize = s(13f);
        float gap = s(3f);
        float pad = s(4f);
        float barH = s(1.5f);
        float barGap = s(1.5f);

        float w = pad * 2 + iconSize * 4 + gap * 3;
        float h = pad * 2 + iconSize + barGap + barH;

        float vw = ctx.getScaledWindowWidth();
        float vh = ctx.getScaledWindowHeight();
        if (!customized) {
            x = (vw - w) * 0.5f;
            y = vh - h - s(52f);
        }
        place(ctx, w, h);

        ui.roundedRect(x, y, w, h, s(3f),
                HudTheme.withAlpha(HudTheme.background(), 160));

        var matrices = ctx.getMatrices();

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(slots[i]);
            float ix = x + pad + i * (iconSize + gap);
            float iy = y + pad;

            matrices.pushMatrix();
            matrices.translate(ix, iy);
            float iconScale = iconSize / 16f;
            matrices.scale(iconScale, iconScale);
            ctx.drawItem(stack, 0, 0);
            matrices.popMatrix();

            float barY = iy + iconSize + barGap;
            float barX = ix;
            float barW = iconSize;

            if (!stack.isEmpty() && stack.isDamageable()) {
                float ratio = Math.max(0f, Math.min(1f,
                        1f - stack.getDamage() / (float) Math.max(1, stack.getMaxDamage())));
                int col = ratio > 0.6f ? 0xFF55E055
                        : ratio > 0.3f ? 0xFFE0C050
                        : 0xFFE05050;

                // warning при <10%
                if (ratio < 0.1f && ratio > 0f) {
                    if (!Boolean.TRUE.equals(WARNED.get(slots[i]))) {
                        Notifications.push(names[i] + " broken soon", 0xFFFF5050);
                        WARNED.put(slots[i], true);
                    }
                } else if (ratio > 0.15f) {
                    WARNED.put(slots[i], false);
                }

                ui.rect(Math.round(barX), Math.round(barY),
                        Math.round(barW), Math.round(barH), 0x50000000);
                int fillW = Math.round(barW * ratio);
                if (fillW > 0) {
                    ui.rect(Math.round(barX), Math.round(barY),
                            fillW, Math.round(barH), col);
                }
            } else {
                ui.rect(Math.round(barX), Math.round(barY),
                        Math.round(barW), Math.round(barH),
                        stack.isEmpty() ? 0x25FFFFFF : 0xFF55E055);
            }
        }

        width = w;
        height = h;
    }
}