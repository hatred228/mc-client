package com.example.bfb.hud.widgets;

import com.example.bfb.ModuleManager;
import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.modules.KillAuraModule;
import com.example.bfb.modules.VisualsModule;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

public class TargetInfoWidget extends HudWidget {

    private LivingEntity displayed;
    private float health;
    private long previousFrame;
    private long displayedUntil = 0L;

    public TargetInfoWidget() {
        super("TargetHud", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null) { displayed = null; width = height = 0; return; }

        VisualsModule vm = VisualsModule.get();
        if (vm != null && !vm.targetHud.bool("Enabled")) { width = height = 0; return; }

        boolean showArmor = vm == null || vm.targetHud.bool("Show armor");
        boolean showHealth = vm == null || vm.targetHud.bool("Show health");
        boolean showHand = vm == null || vm.targetHud.bool("Show hand");
        boolean showHotbar = vm == null || vm.targetHud.bool("Show hotbar");

        boolean inChat = mc.currentScreen instanceof ChatScreen;
        LivingEntity target = pickTarget();

        if (target != null) {
            if (displayed != target) health = target.getHealth();
            displayed = target;
            displayedUntil = System.currentTimeMillis() + 2000L;
        }

        boolean holdActive = displayed != null && System.currentTimeMillis() < displayedUntil;
        boolean show = target != null || holdActive || inChat;

        if (!show) {
            displayed = null;
            width = height = 0;
            return;
        }

        LivingEntity renderTarget;
        if (target != null) renderTarget = target;
        else if (displayed != null) renderTarget = displayed;
        else renderTarget = mc.player;

        float renderHp = target != null ? health : renderTarget.getHealth();
        float renderMaxHp = renderTarget.getMaxHealth();

        long now = System.nanoTime();
        float dt = previousFrame == 0 ? 1f / 60f
                : Math.min(.05f, (now - previousFrame) / 1_000_000_000f);
        previousFrame = now;

        if (target != null) {
            health = HudCompactStyle.approach(health, Math.max(0, displayed.getHealth()), dt, 14);
            renderHp = health;
        }

        visibility.run(show ? 1 : 0, 220);
        visibility.update();
        float reveal = visibility.value();
        if (reveal < 0.01f) { width = height = 0; return; }

        float pad = s(4f);
        float portraitSize = s(28f);

        ItemStack[] armor = new ItemStack[4];
        int armorCount = 0;
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        if (showArmor && target != null) {
            for (int i = 0; i < 4; i++) {
                armor[i] = renderTarget.getEquippedStack(armorSlots[i]);
                if (!armor[i].isEmpty()) armorCount++;
            }
        } else {
            for (int i = 0; i < 4; i++) armor[i] = ItemStack.EMPTY;
        }

        ItemStack mainHand = ItemStack.EMPTY;
        if (showHand && target != null) {
            mainHand = renderTarget.getMainHandStack();
            if (mainHand.isEmpty()) mainHand = renderTarget.getOffHandStack();
        }

        List<ItemStack> hotbar = new ArrayList<>();
        if (showHotbar && target != null && renderTarget instanceof PlayerEntity pe) {
            for (int i = 0; i < 9; i++) {
                ItemStack st = pe.getInventory().getStack(i);
                if (!st.isEmpty()) hotbar.add(st);
            }
        }

        int iconCount = armorCount + (mainHand.isEmpty() ? 0 : 1) + hotbar.size();
        float iconSize = iconCount > 6 ? s(10f) : s(12f);
        float iconStride = iconSize + s(1f);

        float infoW = s(78f);
        float iconsRowW = iconCount * iconStride;
        float w = Math.max(portraitSize + infoW + pad * 3, s(130f));
        if (iconCount > 0) {
            w = Math.max(w, pad * 2 + iconsRowW);
        }

        float topH = Math.max(portraitSize, s(18f));
        float bottomH = iconCount > 0 ? iconSize + s(4f) : 0f;
        float h = pad * 2 + topH + bottomH;

        anchor(ctx, w, h, 0.5f, 1f);
        if (!customized) y -= s(46f);

        float drawY = y + s(3f) * (1 - reveal);
        int alpha = Math.round(reveal * 255);

        float radius = s(4f);
        ui.roundedRect(x, drawY, w, h, radius,
                HudTheme.withAlpha(0x1A1A24, alpha));

        // Портрет
        float px = x + pad;
        float py = drawY + pad;

        boolean drewPortrait = false;
        if (renderTarget instanceof AbstractClientPlayerEntity player) {
            try {
                SkinTextures skin = DefaultSkinHelper.getSkinTextures(player.getGameProfile());
                PlayerSkinDrawer.draw(ctx, skin,
                        Math.round(px), Math.round(py),
                        Math.round(portraitSize));
                drewPortrait = true;
            } catch (Throwable ignored) {
                drewPortrait = false;
            }
        }

        if (!drewPortrait) {
            int fallbackColor = HudTheme.accent();
            ui.roundedRect(px, py, portraitSize, portraitSize, s(3f),
                    HudTheme.withAlpha(fallbackColor, 220));

            String letter = "?";
            String nm = renderTarget.getName().getString();
            if (!nm.isEmpty()) letter = nm.substring(0, 1).toUpperCase();

            float letterW = textWidth(letter, 14f);
            ui.text(px + portraitSize * 0.5f - letterW * 0.5f,
                    py + portraitSize * 0.5f - 5f,
                    14f,
                    HudTheme.withAlpha(0xFFFFFFFF, alpha),
                    letter);
        }

        float textX = px + portraitSize + pad;
        float textRight = x + w - pad;

        String name = target != null ? renderTarget.getName().getString() : "No Target";
        String hpText = Math.round(Math.max(0, renderHp)) + " / " + Math.round(renderMaxHp);

        float fontPx = 8f;
        float hpPx = 8f;

        float hpW = textWidth(hpText, hpPx);
        float nameMaxW = (textRight - textX) - hpW - s(6f);
        String fittedName = fit(name, nameMaxW);

        float nameY = drawY + pad + s(1f);
        int nameColor = target != null
                ? HudTheme.withAlpha(0xFFFFFFFF, alpha)
                : HudTheme.withAlpha(HudTheme.muted(), alpha);

        ui.text(textX, nameY, fontPx, nameColor, fittedName);
        if (showHealth) {
            ui.rightText(textRight, nameY, hpPx,
                    HudTheme.withAlpha(0xFFFFFFFF, alpha), hpText, false);
        }

        if (showHealth) {
            float barX = textX;
            float barY = drawY + pad + s(12f);
            float barW = textRight - textX;
            float barH = s(5f);

            float maxHp = Math.max(1f, renderMaxHp);
            float ratio = Math.max(0f, Math.min(1f, renderHp / maxHp));

            int hpColor;
            if (ratio > 0.66f) hpColor = HudTheme.positive();
            else if (ratio > 0.33f) hpColor = HudTheme.middle();
            else hpColor = HudTheme.negative();

            ui.roundedRect(barX, barY, barW, barH,
                    barH / 2, HudTheme.withAlpha(0x80000000, alpha));

            float fillW = barW * ratio;
            if (fillW > 0) {
                ui.roundedRect(barX, barY, fillW, barH,
                        Math.min(barH / 2, fillW / 2),
                        HudTheme.withAlpha(hpColor, alpha));
            }
        }

        if (iconCount > 0) {
            float bottomY = drawY + pad + topH + s(2f);
            float rowW = iconCount * iconStride - s(1f);
            float ix = x + (w - rowW) * 0.5f;

            for (int i = 0; i < 4; i++) {
                if (armor[i].isEmpty()) continue;
                drawItem(armor[i], ix, bottomY, iconSize);
                ix += iconStride;
            }

            if (!mainHand.isEmpty()) {
                ui.roundedRect(ix - s(1f), bottomY - s(1f),
                        iconSize + s(2f), iconSize + s(2f), s(2f),
                        HudTheme.withAlpha(HudTheme.accent(), 60));
                drawItem(mainHand, ix, bottomY, iconSize);
                ix += iconStride;
            }

            for (ItemStack st : hotbar) {
                drawItem(st, ix, bottomY, iconSize);
                ix += iconStride;
            }
        }

        width = w;
        height = h;
    }

    private void drawItem(ItemStack stack, float ix, float iy, float size) {
        if (stack.isEmpty()) return;
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(ix, iy);
        float sc = size / 16f;
        matrices.scale(sc, sc);
        ctx.drawItem(stack, 0, 0);
        matrices.popMatrix();
    }

    private LivingEntity pickTarget() {
        var killAura = ModuleManager.get(KillAuraModule.class);
        if (killAura != null && killAura.isEnabled()
                && KillAuraModule.target instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        return null;
    }
}