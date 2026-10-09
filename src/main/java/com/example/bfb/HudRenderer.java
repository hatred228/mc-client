package com.example.bfb;

import com.example.bfb.modules.ArraylistModule;
import com.example.bfb.modules.AmbientGlowModule;
import com.example.bfb.modules.CooldownsModule;
import com.example.bfb.modules.CoordsModule;
import com.example.bfb.modules.CrossbowChargeModule;
import com.example.bfb.modules.CustomVisualsModule;
import com.example.bfb.modules.DamageIndicatorModule;
import com.example.bfb.modules.DurabilityModule;
import com.example.bfb.modules.EspModule;
import com.example.bfb.modules.HitRippleModule;
import com.example.bfb.modules.InfoModule;
import com.example.bfb.modules.KeybindsModule;
import com.example.bfb.modules.KeystrokesModule;
import com.example.bfb.modules.NametagsModule;
import com.example.bfb.modules.PointersModule;
import com.example.bfb.modules.PotionsModule;
import com.example.bfb.modules.RadarModule;
import com.example.bfb.modules.TargetHudModule;
import com.example.bfb.modules.ThemeModule;
import com.example.bfb.modules.WatermarkModule;
import com.example.bfb.modules.WaypointsModule;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HudRenderer {
    private HudRenderer() {}

    private static final net.minecraft.util.Identifier HEART_FULL = net.minecraft.util.Identifier.ofVanilla("hud/heart/full");
    private static final net.minecraft.util.Identifier HEART_HALF = net.minecraft.util.Identifier.ofVanilla("hud/heart/half");
    private static final net.minecraft.util.Identifier HEART_EMPTY = net.minecraft.util.Identifier.ofVanilla("hud/heart/container");

    private static final Map<Integer, Float> bfb$displayedHp = new HashMap<>();
    private static final Map<String, Float> bfb$arrayAnim = new HashMap<>();

    private static int hudAlpha() {
        ThemeModule theme = ThemeModule.get();
        if (theme == null) return 0xC0;
        return Math.max(0, Math.min(255, theme.bgAlpha.getInt()));
    }

    private static int hudBg() {
        return (hudAlpha() << 24) | 0x101424;
    }

    // ==================== TRACERS ====================

    private static void drawTracers(DrawContext context, MinecraftClient client) {
        EspModule esp = ModuleManager.get(EspModule.class);
        if (esp == null || !esp.isEnabled() || !esp.tracers.get() || client.world == null) return;
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int startX = screenW / 2;
        int startY = switch (esp.origin.get()) {
            case "Bottom" -> screenH - 1;
            case "Top" -> 0;
            default -> screenH / 2;
        };
        float tick = client.getRenderTickCounter().getTickProgress(true);
        double reach = esp.range.get();
        double reachSq = reach * reach;
        int thickness = Math.max(1, Math.min(3, Math.round(esp.width.getFloat())));
        boolean grad = esp.tracerGradient.get();
        for (LivingEntity entity : EntityCache.living()) {
            if (entity == client.player) continue;
            if (!CombatUtil.accept(entity, client.player, esp.players.get(), esp.mobs.get(), esp.animals.get())) continue;
            if (entity.squaredDistanceTo(client.player) > reachSq) continue;
            Vec3d world = entity.getLerpedPos(tick).add(0, entity.getHeight() * 0.5, 0);
            Vec3d screen = RenderUtil.toScreenFixedFov(world);
            if (screen == null) continue;
            if (screen.x < -20 || screen.x > screenW + 20 || screen.y < -20 || screen.y > screenH + 20) continue;
            int endX = (int) Math.max(0, Math.min(screenW - 1, screen.x));
            int endY = (int) Math.max(0, Math.min(screenH - 1, screen.y));
            int rgb = esp.color.getRgb();
            if (esp.health.get() && entity.getMaxHealth() > 0) {
                float ratio = Math.max(0f, Math.min(1f, entity.getHealth() / entity.getMaxHealth()));
                rgb = ((int) ((1f - ratio) * 255) << 16) | ((int) (ratio * 255) << 8);
            }
            if (grad) {
                int start = 0xCC000000;
                int end = 0xCC000000 | (rgb & 0xFFFFFF);
                RenderUtil.gradientLine(context, startX, startY, endX, endY, start, end, thickness);
            } else {
                RenderUtil.screenLine(context, startX, startY, endX, endY, 0xCC000000 | rgb, thickness);
            }
        }
    }

    // ==================== NAMETAGS ====================

    private static void drawNametags(DrawContext context, MinecraftClient client) {
        NametagsModule tags = ModuleManager.get(NametagsModule.class);
        if (tags == null || !tags.isEnabled() || client.world == null || client.player == null) return;
        var camera = client.gameRenderer.getCamera();
        boolean firstPerson = client.options.getPerspective().isFirstPerson();
        double range = tags.range.get();
        double limit = range * range;
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        float tickDelta = client.getRenderTickCounter().getTickProgress(true);
        for (LivingEntity entity : EntityCache.living()) {
            if (entity == client.player && firstPerson) continue;
            if (!tags.thirdPerson.get() && entity == camera.getFocusedEntity()) continue;
            if (entity.isInvisible() && !tags.invisible.get()) continue;
            if (!tags.players.get() && entity instanceof PlayerEntity) continue;
            if (!tags.mobs.get() && !(entity instanceof PlayerEntity) && !(entity instanceof AnimalEntity)) continue;
            if (!tags.animals.get() && entity instanceof AnimalEntity) continue;
            double distSq = entity.squaredDistanceTo(client.player);
            if (distSq > limit) continue;

            Vec3d anchor = entity.getLerpedPos(tickDelta).add(0, entity.getHeight() + 0.55, 0);
            Vec3d screen = RenderUtil.toScreenFixedFov(anchor);
            if (screen == null) continue;
            if (screen.x < -100 || screen.x > sw + 100 || screen.y < -80 || screen.y > sh + 80) continue;
            float sx = (float) Math.max(52, Math.min(sw - 52, screen.x));
            float sy = (float) Math.max(18, Math.min(sh - 70, screen.y));

            int alpha = 255;
            if (tags.fade.get()) {
                alpha = (int) (255 - 140 * Math.sqrt(distSq) / range);
                alpha = Math.max(100, Math.min(255, alpha));
            }
            drawCompactTag(context, client, tags, entity, sx, sy, alpha);
        }
    }

    private static void drawCompactTag(DrawContext context, MinecraftClient client, NametagsModule tags,
                                       LivingEntity entity, float sx, float sy, int alpha) {
        float size = tags.size.getFloat();
        if (tags.autoScale.get()) {
            double dist = entity.distanceTo(client.player);
            size *= (float) Math.max(0.55, Math.min(1.5, 12.0 / Math.max(4.0, dist)));
        }

        String name = entity instanceof PlayerEntity pe ? pe.getGameProfile().name() : entity.getName().getString();
        int nameW = LabelFont.width(name);
        int lineH = LabelFont.height();

        float hp = entity.getHealth();
        float hpMax = entity.getMaxHealth();
        float hpRatio = hpMax > 0 ? Math.max(0f, Math.min(1f, hp / hpMax)) : 1f;
        int hpColor = hpRatio > 0.66f ? 0xFF55FF55 : hpRatio > 0.33f ? 0xFFFFCC44 : 0xFFFF4444;

        boolean useHearts = tags.hearts.get();
        String hpStr = "";
        if (!useHearts && tags.health.get()) hpStr = Math.round(hp) + "  " + Math.round(hpMax);

        List<ItemStack> armorItems = new ArrayList<>();
        List<Integer> armorLevels = new ArrayList<>();
        if (entity instanceof PlayerEntity) {
            var slots = new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (var s : slots) {
                var st = entity.getEquippedStack(s);
                if (!st.isEmpty()) {
                    armorItems.add(st);
                    armorLevels.add(getProtectionLevel(st));
                }
            }
        }

        int armorSlotSize = tags.armorSize.getInt();
        int romanSize = tags.romanSize.getInt();
        int armorColH = armorItems.size() * armorSlotSize;
        int armorColW = armorItems.isEmpty() ? 0 : armorSlotSize;
        int romanWidth = 0;
        for (int lvl : armorLevels) if (lvl > 0) romanWidth = Math.max(romanWidth, romanSize + 2);

        int hearts = 0;
        int heartsW = 0;
        if (useHearts) {
            hearts = Math.min(10, Math.max(1, (int) Math.ceil(hpMax / 2f)));
            heartsW = hearts * 9;
        }
        int hpW = LabelFont.width(hpStr);

        int textRowW = Math.max(nameW, useHearts ? heartsW : hpW);
        int textRowH = lineH + ((tags.health.get() || useHearts) ? lineH + 1 : 0);

        int pad = 4;
        int gapX = 2;
        int totalW = pad * 2 + armorColW + romanWidth + (armorItems.isEmpty() ? 0 : gapX) + textRowW;
        int totalH = pad * 2 + Math.max(textRowH, armorColH);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(sx, sy);
        matrices.scale(size, size);

        if (tags.background.get()) {
            int bgAlpha = (int) (alpha * 0.72f);
            int bgRgb = tags.backgroundColor.getRgb() & 0xFFFFFF;
            context.fill(-totalW / 2, -totalH / 2, totalW / 2, totalH / 2, (bgAlpha << 24) | bgRgb);
        }

        if (tags.health.get() && tags.showBar.get()) {
            int barW = (int) (totalW * hpRatio);
            context.fill(-totalW / 2, totalH / 2 - 1, -totalW / 2 + barW, totalH / 2, (alpha << 24) | (hpColor & 0xFFFFFF));
        }

        int startX = -totalW / 2 + pad;
        int centerY = 0;

        if (!armorItems.isEmpty()) {
            int armorTopY = centerY - armorColH / 2;
            for (int i = 0; i < armorItems.size(); i++) {
                int y = armorTopY + i * armorSlotSize;
                int lvl = armorLevels.get(i);
                if (lvl > 0) {
                    String roman = toRoman(lvl);
                    if (tags.romanRubick.get()) {
                        var romanMatrices = context.getMatrices();
                        romanMatrices.pushMatrix();
                        romanMatrices.translate(startX + 4 - romanSize - 2, y + armorSlotSize / 2f);
                        float romanScale = romanSize / (float) LabelFont.height();
                        romanMatrices.scale(romanScale, romanScale);
                        LabelFont.draw(context, roman, 0, -LabelFont.height() / 2, (alpha << 24) | 0xFFAA00);
                        romanMatrices.popMatrix();
                    } else {
                        LabelFont.draw(context, roman, startX + 4 - LabelFont.width(roman) - 2, y + 4, (alpha << 24) | 0xFFAA00);
                    }
                }
                var armorMatrices = context.getMatrices();
                armorMatrices.pushMatrix();
                armorMatrices.translate(startX + 4, y);
                float armorScale = armorSlotSize / 16f;
                armorMatrices.scale(armorScale, armorScale);
                context.drawItem(armorItems.get(i), 0, 0);
                armorMatrices.popMatrix();
            }
        }

        int textX = startX + armorColW + romanWidth + (armorItems.isEmpty() ? 0 : gapX);
        int textTopY = centerY - textRowH / 2;

        int nameX = textX + (textRowW - nameW) / 2;
        LabelFont.draw(context, name, nameX, textTopY, (alpha << 24) | 0xFFFFFFFF);

        if (tags.health.get() || useHearts) {
            int row2Y = textTopY + lineH + 1;
            if (useHearts) {
                float totalHearts = hpRatio * hearts;
                int heartsStartX = textX + (textRowW - heartsW) / 2;
                int hx = heartsStartX;
                for (int i = 0; i < hearts; i++) {
                    float part = totalHearts - i;
                    var sprite = part >= 1f ? HEART_FULL : part >= 0.5f ? HEART_HALF : HEART_EMPTY;
                    context.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, sprite, hx, row2Y - 1, 9, 9);
                    hx += 9;
                }
            } else if (!hpStr.isEmpty()) {
                int hpX = textX + (textRowW - hpW) / 2;
                LabelFont.draw(context, hpStr, hpX, row2Y, (alpha << 24) | (hpColor & 0xFFFFFF));
            }
        }

        matrices.popMatrix();
    }

    private static int getProtectionLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var enchants = stack.getEnchantments();
        for (var entry : enchants.getEnchantments()) {
            if (entry.matchesKey(Enchantments.PROTECTION)) return enchants.getLevel(entry);
        }
        return 0;
    }

    private static String toRoman(int n) {
        if (n <= 0) return "";
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (n <= 10) return romans[n];
        return Integer.toString(n);
    }

    // ==================== ITEM TAGS ====================

    private static void drawItemTags(DrawContext context, MinecraftClient client) {
        EspModule esp = ModuleManager.get(EspModule.class);
        if (esp == null || !esp.isEnabled() || !esp.itemTags.get() || client.world == null) return;
        float tick = client.getRenderTickCounter().getTickProgress(true);
        double reach = Math.min(24, esp.range.get());
        Box area = client.player.getBoundingBox().expand(reach);
        int shown = 0;
        float tagScale = esp.itemTagSize.getFloat();
        for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class, area, candidate ->
                candidate.squaredDistanceTo(client.player) <= reach * reach
                        && (!esp.gear.get() || GearItems.lyingGear(candidate.getStack())))) {
            if (shown++ >= 20) break;
            Vec3d screen = RenderUtil.toScreenFixedFov(item.getLerpedPos(tick).add(0, 0.4, 0));
            if (screen == null) continue;
            if (screen.x < -100 || screen.x > context.getScaledWindowWidth() + 100
                    || screen.y < -100 || screen.y > context.getScaledWindowHeight() + 100) continue;
            String text = item.getStack().getName().getString();
            int count = item.getStack().getCount();
            if (count > 1) text = text + " #" + count;
            int width = LabelFont.width(text);
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate((float) screen.x, (float) screen.y);
            matrices.scale(tagScale, tagScale);
            LabelFont.draw(context, text, -width / 2 + 1, 1, 0x80000000);
            LabelFont.draw(context, text, -width / 2, 0, 0xFFF4F6FF);
            matrices.popMatrix();
        }
    }

    // ==================== ESP 2D / BARS ====================

    private static void drawFlatEsp(DrawContext context, MinecraftClient client) {
        EspModule esp = ModuleManager.get(EspModule.class);
        if (esp == null || !esp.isEnabled() || client.world == null) return;
        if (!esp.style.is("2D") && !esp.style.is("Bars")) return;
        double reach = esp.range.get();
        int shown = 0;
        int thickness = Math.max(1, (int) Math.round(esp.width.getFloat()));
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        boolean is2D = esp.style.is("2D");
        boolean doGlow = esp.glow.get();
        float glowT = esp.glowThickness.getFloat();

        for (LivingEntity entity : EntityCache.living()) {
            if (entity == client.player) continue;
            if (!CombatUtil.accept(entity, client.player, esp.players.get(), esp.mobs.get(), esp.animals.get())) continue;
            double distSq = entity.squaredDistanceTo(client.player);
            if (distSq > reach * reach) continue;
            if (shown++ >= 30) break;
            int[] rect = RenderUtil.screenRect(entity.getBoundingBox());
            if (rect == null) continue;
            int x1 = (int) Math.max(-30, Math.min(sw + 30, Math.min(rect[0], rect[2])));
            int y1 = (int) Math.max(-30, Math.min(sh + 30, Math.min(rect[1], rect[3])));
            int x2 = (int) Math.max(-30, Math.min(sw + 30, Math.max(rect[0], rect[2])));
            int y2 = (int) Math.max(-30, Math.min(sh + 30, Math.max(rect[1], rect[3])));
            if (x2 < 0 || y2 < 0 || x1 > sw || y1 > sh) continue;
            if (x2 - x1 < 3 || y2 - y1 < 3) continue;

            float ratio = entity.getMaxHealth() > 0
                    ? Math.max(0f, Math.min(1f, entity.getHealth() / entity.getMaxHealth())) : 1f;
            boolean flash = esp.damageFlash.get() && entity.hurtTime > 0;
            int rgb = flash ? 0xFF3B30 : (esp.health.get() ? healthColor(ratio) : 0xFF000000 | esp.color.getRgb());
            int alpha = esp.fade.get()
                    ? Math.max(80, (int) (255 * (1 - 0.5 * Math.sqrt(distSq) / reach))) << 24
                    : 0xFF000000;
            int col = alpha | (rgb & 0xFFFFFF);

            if (doGlow) {
                for (int i = (int) glowT; i >= 1; i--) {
                    int glowAlpha = (0x40 / (i + 1)) << 24;
                    int glowCol = glowAlpha | (rgb & 0xFFFFFF);
                    context.fill(x1 - i, y1 - i, x2 + i, y1 - i + 1, glowCol);
                    context.fill(x1 - i, y2 + i - 1, x2 + i, y2 + i, glowCol);
                    context.fill(x1 - i, y1 - i, x1 - i + 1, y2 + i, glowCol);
                    context.fill(x2 + i - 1, y1 - i, x2 + i, y2 + i, glowCol);
                }
            }
            if (is2D) context.fill(x1, y1, x2, y2, 0x20FFFFFF);
            context.fill(x1, y1, x2, y1 + thickness, col);
            context.fill(x1, y2 - thickness, x2, y2, col);
            context.fill(x1, y1, x1 + thickness, y2, col);
            context.fill(x2 - thickness, y1, x2, y2, col);

            Integer key = entity.getId();
            Float prev = bfb$displayedHp.get(key);
            float shownRatio = prev == null ? ratio : prev + (ratio - prev) * 0.18f;
            if (Math.abs(shownRatio - ratio) < 0.005f) shownRatio = ratio;
            bfb$displayedHp.put(key, shownRatio);

            int bar = y2 + 3;
            context.fill(x1 - 1, bar - 1, x2 + 1, bar + 4, 0xFF0A0A10);
            context.fill(x1, bar, x2, bar + 3, 0xFF23242E);
            int filled = Math.max(1, Math.round((x2 - x1) * shownRatio));
            int barColor = healthColor(shownRatio);
            context.fillGradient(x1, bar, x1 + filled, bar + 3, 0xFF000000 | barColor, barColor);

            if (is2D) {
                int iconSize = 16;
                int labelPadX = 0;
                if (entity instanceof AbstractClientPlayerEntity acp) {
                    SkinTextures skin = DefaultSkinHelper.getSkinTextures(acp.getGameProfile());
                    int hx = x1;
                    int hy = y1 - iconSize - 3;
                    context.fill(hx - 1, hy - 1, hx + iconSize + 1, hy + iconSize + 1, 0xFF1A0E1E);
                    PlayerSkinDrawer.draw(context, skin, hx, hy, iconSize, 0xFFFFFFFF);
                    labelPadX = iconSize + 3;
                }

                String label = entity.getName().getString();
                String hpText = Math.round(entity.getHealth()) + " HP";
                String distText = "[" + Math.round(Math.sqrt(distSq)) + "m]";
                int lw = SalFont.width(label);
                int hw = SalFont.width(hpText);
                int dw = SalFont.width(distText);
                int lx = x1 + labelPadX;
                int ly = y1 - 12;
                int totalW = labelPadX + Math.max(lw, hw + 4 + dw);
                context.fill(x1 - 2, y1 - 14, x1 + totalW + 4, y1 - 1, 0x90000000);
                SalFont.draw(context, label, lx, ly, 0xFFF4F6FF);
                SalFont.draw(context, hpText, lx, ly + SalFont.height() + 1, 0xFF000000 | barColor);
                SalFont.draw(context, distText, lx + hw + 4, ly + SalFont.height() + 1, 0xFFB9C4FF);
            }
        }

        if (bfb$displayedHp.size() > 128) bfb$displayedHp.clear();
    }

    private static int healthColor(float ratio) {
        int red = (int) ((1f - ratio) * 255);
        int green = (int) (ratio * 255);
        return 0xFF000000 | (red << 16) | (green << 8);
    }

    // ==================== CROSSHAIR ====================

    private static void drawCrosshair(DrawContext context, MinecraftClient client) {
        CustomVisualsModule mod = ModuleManager.get(CustomVisualsModule.class);
        if (mod == null || !mod.isEnabled() || !mod.customCrosshair.get()) return;
        if (client.options.hudHidden || client.currentScreen != null) return;
        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;
        int color = 0xFF000000 | mod.crosshairColor.getRgb();
        int length = mod.crosshairLength.getInt();
        int gap = mod.crosshairGap.getInt();
        int t = mod.crosshairWidth.getInt();
        int half = t / 2;
        int half2 = t - half;
        context.fill(cx - gap - length + 1, cy - half, cx - gap + 1, cy + half2, color);
        context.fill(cx + gap, cy - half, cx + gap + length, cy + half2, color);
        context.fill(cx - half, cy - gap - length + 1, cx + half2, cy - gap + 1, color);
        context.fill(cx - half, cy + gap, cx + half2, cy + gap + length, color);
        if (mod.crosshairDot.get()) {
            int ds = mod.crosshairDotSize.getInt();
            int dh = ds / 2;
            int dh2 = ds - dh;
            context.fill(cx - dh, cy - dh, cx + dh2, cy + dh2, color);
        }
    }

    // ==================== ARRAYLIST ====================

    private static void renderArraylist(DrawContext context, MinecraftClient client) {
        List<CheatModule> enabled = new ArrayList<>();
        for (CheatModule m : ModuleManager.getModules()) {
            if (m.getName().equals("Arraylist")) continue;
            if (m.isEnabled()) enabled.add(m);
        }
        enabled.sort((a, b) -> Integer.compare(
                SalFont.width(Lang.tr(b.getName())), SalFont.width(Lang.tr(a.getName()))));

        Set<String> alive = new HashSet<>();
        for (CheatModule m : enabled) {
            alive.add(m.getName());
            float cur = bfb$arrayAnim.getOrDefault(m.getName(), 0f);
            cur += (1f - cur) * 0.22f;
            bfb$arrayAnim.put(m.getName(), cur);
        }
        var it = bfb$arrayAnim.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (alive.contains(e.getKey())) continue;
            float cur = e.getValue() - 0.18f;
            if (cur <= 0.02f) it.remove();
            else e.setValue(cur);
        }

        int screenW = context.getScaledWindowWidth();
        int rowH = SalFont.height() + 4;
        int y = HudEditor.drawY("arraylist", 6, 6);

        for (CheatModule m : enabled) {
            float anim = bfb$arrayAnim.getOrDefault(m.getName(), 0f);
            if (anim < 0.02f) continue;

            String name = Lang.tr(m.getName());
            int textW = SalFont.width(name);
            int fullW = textW + 10;
            int curW = (int) (fullW * anim);
            int x2 = screenW - 3;
            int x1 = x2 - curW;

            int a = (int) (0xD0 * anim);
            int categoryColor = m.getCategory().getColor() & 0xFFFFFF;

            for (int i = 1; i <= 3; i++) {
                int gl = (a / (i + 2)) >> 1;
                if (gl <= 0) continue;
                context.fill(x1 - i, y, x1, y + rowH, (gl << 24) | categoryColor);
            }

            context.fill(x1, y, x2, y + rowH, (a << 24) | 0x0C0C14);
            context.fill(x1, y, x1 + 2, y + rowH, (a << 24) | categoryColor);

            int textAlpha = (int) (0xFF * anim);
            int textX = x1 + 8;
            SalFont.draw(context, name, textX + 1, y + 2, ((textAlpha / 3) << 24));
            SalFont.draw(context, name, textX, y + 1, (textAlpha << 24) | 0xF4F6FF);

            y += rowH + 1;
        }

        HudEditor.outline(context, "arraylist", screenW - 160, 6, 160, y - 6);
    }

    // ==================== REGISTRATION + MAIN RENDER ====================

    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
    }

    private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter counter) {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = counter.getTickProgress(true);
        if (client.player == null || client.options.hudHidden) return;
        if (PanicManager.hideVisuals()) return;

        HudEditor.tick();

        AmbientGlowModule.draw(context, client, tickDelta);

        RadarModule radar = ModuleManager.get(RadarModule.class);
        if (radar != null && radar.isEnabled()) RadarModule.draw(context, client, tickDelta);

        drawTracers(context, client);

        DurabilityModule.draw(context, client);

        drawFlatEsp(context, client);
        drawNametags(context, client);
        drawItemTags(context, client);

        TargetHudModule targetHud = ModuleManager.get(TargetHudModule.class);
        if (targetHud != null && targetHud.isEnabled()) {
            TargetHudModule.draw(context, client, targetHud);
        }

        KeystrokesModule keys = ModuleManager.get(KeystrokesModule.class);
        if (keys != null && keys.isEnabled()) {
            KeystrokesModule.draw(context, client, keys.mouse.get(), keys.space.get());
        }

        PointersModule ptr = ModuleManager.get(PointersModule.class);
        if (ptr != null && ptr.isEnabled()) {
            PointersModule.draw(context, client, tickDelta,
                    ptr.radius.get(), ptr.range.get(), ptr.fade.get(),
                    ptr.glow.get(), ptr.smooth.get(),
                    ptr.arrowSize.get(), 2.0, 0xFFFFFFFF);
        }

        WaypointsModule.drawText(context, client, tickDelta);

        DamageIndicatorModule.draw(context, client, tickDelta);
        HitRippleModule.draw(context, client, tickDelta);

        CoordsModule coords = ModuleManager.get(CoordsModule.class);
        if (coords != null && coords.isEnabled()) {
            List<String> lines = CoordsModule.lines(client.player);
            int lineHeight = SalFont.height();
            int width = 0;
            for (String line : lines) width = Math.max(width, SalFont.width(line));
            int block = lineHeight * lines.size() + 6;
            int defX = 6, defY = 6;
            int baseX = HudEditor.drawX("coords", defX, defY);
            int baseY = HudEditor.drawY("coords", defX, defY);
            context.fill(baseX, baseY, baseX + 10 + width, baseY + 2 + block, hudBg());
            context.fill(baseX, baseY, baseX + 3, baseY + 2 + block, 0xFFB14DFF);
            int textY = baseY + 3;
            for (String line : lines) {
                SalFont.draw(context, line, baseX + 7, textY, 0xFFE8EAF6);
                textY += lineHeight + 1;
            }
            HudEditor.outline(context, "coords", baseX, baseY, 10 + width, 2 + block);
        }

        PotionsModule.draw(context, client);
        CooldownsModule.draw(context, client);
        KeybindsModule.draw(context, client);
        WatermarkModule.draw(context, client);

        int screen = context.getScaledWindowWidth();
        int lineHeight = SalFont.height();

        InfoModule info = ModuleManager.get(InfoModule.class);
        if (info != null && info.isEnabled()) {
            String title = "Undetected";
            String fps = client.getCurrentFps() + " FPS  " + ping(client) + " ms";
            int titleWidth = SalFont.width(title);
            int fpsWidth = SalFont.width(fps);
            int blockWidth = Math.max(titleWidth, fpsWidth) + 16;
            int blockHeight = lineHeight * 2 + 8;
            int defX = screen - blockWidth - 4;
            int defY = 6;
            int baseX = HudEditor.drawX("info", defX, defY);
            int baseY = HudEditor.drawY("info", defX, defY);
            context.fill(baseX, baseY, baseX + blockWidth, baseY + blockHeight, hudBg());
            context.fill(baseX + blockWidth - 3, baseY, baseX + blockWidth, baseY + blockHeight, 0xFF7C5CFF);
            SalFont.draw(context, title, baseX + blockWidth - 8 - titleWidth, baseY + 3, 0xFFF4F6FF);
            SalFont.draw(context, fps, baseX + blockWidth - 8 - fpsWidth, baseY + 5 + lineHeight, 0xFF4CC3FF);
            HudEditor.outline(context, "info", baseX, baseY, blockWidth, blockHeight);
        }

        ArraylistModule arraylist = ModuleManager.get(ArraylistModule.class);
        if (arraylist != null && arraylist.isEnabled()) {
            renderArraylist(context, client);
        }

        CrossbowChargeModule.draw(context, client);

        drawCrosshair(context, client);

        NotificationSystem.render(context, client);
    }

    private static int ping(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }
}