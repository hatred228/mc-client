package com.example.bfb.hud.widgets;

import com.example.bfb.EntityCache;
import com.example.bfb.FriendManager;
import com.example.bfb.RenderUtil;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.modules.ChatUtilsModule;
import com.example.bfb.modules.VisualsModule;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;

public class NametagsWidget extends HudWidget {

    public NametagsWidget() {
        super("Nametags", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null || mc.world == null) { width = height = 0; return; }

        VisualsModule vm = VisualsModule.get();
        if (vm != null && !vm.nametags.bool("Enabled")) { width = height = 0; return; }

        double RANGE = vm != null ? vm.nametags.num("Range") : 48.0;
        float SIZE = vm != null ? vm.nametags.num("Scale") : 1.0f;
        boolean SHOW_HP = vm == null || vm.nametags.bool("Show health");
        boolean SHOW_ARMOR = vm == null || vm.nametags.bool("Show armor");
        boolean SHOW_ENCHANT = vm == null || vm.nametags.bool("Show enchant");
        boolean SHOW_BG = vm == null || vm.nametags.bool("Show background");
        boolean FADE = vm == null || vm.nametags.bool("Distance fade");
        boolean FILT_P = vm == null || vm.nametags.bool("Players");
        boolean FILT_M = vm == null || vm.nametags.bool("Mobs");
        boolean FILT_A = vm == null || vm.nametags.bool("Animals");
        boolean FILT_I = vm != null && vm.nametags.bool("Invisible");

        double limitSq = RANGE * RANGE;
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        for (LivingEntity entity : EntityCache.living()) {
            if (entity == mc.player) continue;
            if (entity.isInvisible() && !FILT_I) continue;
            if (entity instanceof PlayerEntity && !FILT_P) continue;
            if (entity instanceof AnimalEntity && !FILT_A) continue;
            if (!(entity instanceof PlayerEntity) && !(entity instanceof AnimalEntity) && !FILT_M) continue;

            double distSq = entity.squaredDistanceTo(mc.player);
            if (distSq > limitSq) continue;

            Vec3d anchor = entity.getLerpedPos(tickDelta).add(0, entity.getHeight() + 0.5, 0);
            Vec3d screen = RenderUtil.toScreenFixedFov(anchor);
            if (screen == null) continue;
            if (screen.x < -100 || screen.x > ctx.getScaledWindowWidth() + 100) continue;
            if (screen.y < -80 || screen.y > ctx.getScaledWindowHeight() + 80) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;
            int alpha = 255;
            if (FADE) {
                alpha = (int) (255 - 120 * Math.sqrt(distSq) / RANGE);
                alpha = Math.max(120, Math.min(255, alpha));
            }

            drawTag(entity, sx, sy, alpha, SIZE, SHOW_HP, SHOW_ARMOR, SHOW_ENCHANT, SHOW_BG);
        }
    }

    private void drawTag(LivingEntity entity, float sx, float sy, int alpha, float size,
                         boolean showHp, boolean showArmor, boolean showEnchant, boolean showBg) {
        // Имя
        String name = entity instanceof PlayerEntity pe
                ? pe.getGameProfile().name()
                : entity.getName().getString();
        ChatUtilsModule cu = ChatUtilsModule.get();
        if (cu != null && cu.shouldProtectInNametags()) {
            name = cu.protectName(name);
        }

        // ХП
        float health = entity.getHealth();
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        int hearts = (int) Math.ceil(health / 2f);
        int maxHearts = (int) Math.ceil(maxHealth / 2f);

        int heartColor = health / maxHealth > 0.66f ? 0xFFFF5555
                : health / maxHealth > 0.33f ? 0xFFFFAA44
                : 0xFFFF3333;

        // Броня
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        ItemStack[] armor = new ItemStack[4];
        int armorCount = 0;
        for (int i = 0; i < 4; i++) {
            armor[i] = entity.getEquippedStack(armorSlots[i]);
            if (!armor[i].isEmpty()) armorCount++;
        }
        ItemStack mainHand = entity.getMainHandStack();
        if (mainHand.isEmpty()) mainHand = entity.getOffHandStack();

        // Размеры
        int nameW = mc.textRenderer.getWidth(name);
        int heartsW = maxHearts * 5 + (maxHearts - 1);
        int itemsRowW = (armorCount + (mainHand.isEmpty() ? 0 : 1)) * 13;
        int totalW = Math.max(nameW, Math.max(heartsW, itemsRowW)) + 8;
        totalW = Math.max(totalW, 50);

        int lineH = 10;
        int totalH = 4 + lineH;
        if (showHp) totalH += lineH;
        // ряд брони + подписи под ней
        boolean hasArmorRow = showArmor && (armorCount > 0 || !mainHand.isEmpty());
        if (hasArmorRow) {
            totalH += 15; // иконки
            if (showEnchant) totalH += 8; // подписи с зачарованием
        }
        totalH += 4;

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(sx, sy);
        matrices.scale(size, size);

        int bgAlpha = (int) (alpha * 0.6f);
        if (showBg) {
            ctx.fill(-totalW / 2, -totalH, totalW / 2, 0, (bgAlpha << 24) | 0x101018);
        }

        int y = -totalH + 4;

        // Имя
        ctx.drawText(mc.textRenderer, name, -nameW / 2, y, (alpha << 24) | 0xFFFFFF, true);
        y += lineH;

        // Сердечки
        if (showHp) {
            int heartX = -heartsW / 2;
            for (int i = 0; i < maxHearts; i++) {
                int c = i < hearts ? heartColor : 0x60FFFFFF;
                drawHeart(ctx, heartX + i * 6, y, (alpha << 24) | c);
            }
            y += lineH;
        }

        // Броня + рука в 1 ряд
        if (hasArmorRow) {
            int rowW = (armorCount + (mainHand.isEmpty() ? 0 : 1)) * 13 - 1;
            int startX = -rowW / 2;
            int ax = startX;
            var armorMatrices = ctx.getMatrices();

            // иконки
            int iconRowY = y;
            for (int i = 0; i < 4; i++) {
                if (armor[i].isEmpty()) continue;
                armorMatrices.pushMatrix();
                armorMatrices.translate(ax, iconRowY);
                armorMatrices.scale(12f / 16f, 12f / 16f);
                ctx.drawItem(armor[i], 0, 0);
                armorMatrices.popMatrix();
                ax += 13;
            }
            if (!mainHand.isEmpty()) {
                armorMatrices.pushMatrix();
                armorMatrices.translate(ax, iconRowY);
                armorMatrices.scale(12f / 16f, 12f / 16f);
                ctx.drawItem(mainHand, 0, 0);
                armorMatrices.popMatrix();
            }

            // подписи с Protection римскими
            if (showEnchant) {
                int textY = iconRowY + 13;
                ax = startX;
                for (int i = 0; i < 4; i++) {
                    if (armor[i].isEmpty()) continue;
                    int protLvl = getProtectionLevel(armor[i]);
                    if (protLvl > 0) {
                        String roman = toRoman(protLvl);
                        int tw = mc.textRenderer.getWidth(roman);
                        // цвет — золотой как в чате
                        ctx.drawText(mc.textRenderer, roman,
                                ax + (12 - tw) / 2, textY,
                                (alpha << 24) | 0xFFAA00, true);
                    }
                    ax += 13;
                }
                // под рукой пустое место — не пишем ничего
            }
        }

        matrices.popMatrix();
    }

    /**
     * Возвращает уровень Protection (Защита) на стеке, 0 если нет.
     */
    private int getProtectionLevel(ItemStack stack) {
        if (stack.isEmpty() || mc.world == null) return 0;
        try {
            var registry = mc.world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT);
            RegistryEntry<Enchantment> prot = registry.getOrThrow(Enchantments.PROTECTION);
            return EnchantmentHelper.getLevel(prot, stack);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Римские цифры 1..10, потом арабские.
     */
    private static String toRoman(int n) {
        if (n <= 0) return "";
        if (n > 10) return String.valueOf(n);
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[n];
    }

    /** Настоящее сердечко 5×4 пикселя. */
    private void drawHeart(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 2, y + 1, color);
        ctx.fill(x + 3, y, x + 5, y + 1, color);
        ctx.fill(x, y + 1, x + 5, y + 3, color);
        ctx.fill(x + 1, y + 3, x + 4, y + 4, color);
    }
}