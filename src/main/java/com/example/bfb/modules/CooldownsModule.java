package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.HudStyle;
import com.example.bfb.Lang;
import com.example.bfb.SalFont;
import com.example.bfb.gui.RingTextureCache;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CooldownsModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "TopLeft",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.1));
    public final NumberSetting iconSize = add(new NumberSetting("Icon size", 20, 14, 32, 1));
    public final NumberSetting spacing = add(new NumberSetting("Spacing", 4, 1, 12, 1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 4, 0, 10, 1));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 2, 0, 5, 1));
    public final NumberSetting headerH = add(new NumberSetting("Header height", 14, 10, 24, 1));

    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0xE0100A14));
    public final ColorSetting nameColor = add(new ColorSetting("Name color", 0xFFFFFF));
    public final ColorSetting timeColor = add(new ColorSetting("Time color", 0xD8C0A0));
    public final ColorSetting readyColor = add(new ColorSetting("Ready color", 0x55FF55));
    public final ColorSetting ringColor = add(new ColorSetting("Ring color", 0xFFFFFF));

    public final BooleanSetting alwaysShow = add(new BooleanSetting("Always show slot", true));
    public final BooleanSetting showPearl = add(new BooleanSetting("Ender Pearl", true));
    public final BooleanSetting showChorus = add(new BooleanSetting("Chorus Fruit", true));
    public final BooleanSetting showGapple = add(new BooleanSetting("Golden Apple", true));
    public final BooleanSetting showTotem = add(new BooleanSetting("Totem", true));
    public final BooleanSetting showEnderEye = add(new BooleanSetting("Eye of Ender", false));
    public final BooleanSetting showTrap = add(new BooleanSetting("Trap", false));

    public CooldownsModule() {
        super("Cooldowns", "Item cooldowns", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }

    public static void draw(DrawContext context, MinecraftClient client) {
        CooldownsModule mod = com.example.bfb.ModuleManager.get(CooldownsModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;

        PlayerEntity player = client.player;
        List<ItemStack> items = new ArrayList<>();
        addIf(items, player, Items.ENDER_PEARL, mod.showPearl.get(), mod.alwaysShow.get());
        addIf(items, player, Items.CHORUS_FRUIT, mod.showChorus.get(), mod.alwaysShow.get());
        addIf(items, player, Items.GOLDEN_APPLE, mod.showGapple.get(), mod.alwaysShow.get());
        addIf(items, player, Items.TOTEM_OF_UNDYING, mod.showTotem.get(), mod.alwaysShow.get());
        addIf(items, player, Items.ENDER_EYE, mod.showEnderEye.get(), mod.alwaysShow.get());
        addIf(items, player, Items.TRAPPED_CHEST, mod.showTrap.get(), mod.alwaysShow.get());
        if (items.isEmpty()) return;

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
        int readyArgb = 0xFF000000 | (mod.readyColor.getRgb() & 0xFFFFFF);
        int ringRgb = mod.ringColor.getRgb() & 0xFFFFFF;

        int lineH = SalFont.height();
        int textW = 90;
        int rowH = iconSize + spacing;
        int contentW = 4 + iconSize + 6 + textW + 4;
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
        int baseX = HudEditor.getX("cooldowns", defX);
        int baseY = HudEditor.getY("cooldowns", defY);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);

        HudStyle.panel(context, 0, 0, boxW, boxH, radius, edgeW, c1, c2, bg, true);
        HudStyle.header(context, 0, 0, boxW, headerH, c1, c2, Lang.tr("Cooldowns"), 0xFFFFFF);

        long now = System.currentTimeMillis();
        int y = headerH + 2;
        for (ItemStack stack : items) {
            int iconX = 4;
            int iconY = y;

            context.drawItem(stack, iconX, iconY);

            float progress = player.getItemCooldownManager().getCooldownProgress(stack, 0f);
            boolean cooling = progress < 1.0f;
            boolean inInventory = hasItem(player, stack.getItem());

            // === круговой прогресс вокруг иконки ===
            int cx = iconX + iconSize / 2;
            int cy = iconY + iconSize / 2;
            int cr = iconSize / 2 + 2;

            if (cooling) {
                // background ring + заполнение
                drawArc(context, cx, cy, cr, 1f, 0x30FFFFFF);
                drawArc(context, cx, cy, cr, progress, 0xFF000000 | ringRgb);
            } else if (inInventory) {
                // пульсирующий зелёный контур
                float pulse = 0.5f + 0.5f * (float) Math.sin(now / 400.0);
                int a = (int) (140 + 115 * pulse);
                drawArc(context, cx, cy, cr, 1f, (a << 24) | (mod.readyColor.getRgb() & 0xFFFFFF));
            }

            String name = stack.getName().getString();
            String time;
            if (cooling) {
                time = String.format("%.1fs", estimateTotal(stack.getItem()) * (1f - progress));
            } else if (!inInventory) {
                time = "нет";
            } else {
                time = "ready";
            }

            SalFont.draw(context, name, 4 + iconSize + 6, y + 1,
                    inInventory ? nameArgb : 0xFF808080);
            SalFont.draw(context, time, 4 + iconSize + 6, y + 1 + lineH,
                    cooling ? timeArgb : (inInventory ? readyArgb : 0xFF808080));

            y += rowH;
        }

        matrices.popMatrix();
        HudEditor.outline(context, "cooldowns", baseX, baseY, totalW, totalH);
    }

    /**
     * Рисует дугу через запечённую текстуру.
     * progress: 1.0 = полный круг, 0.0 = пусто.
     */
    private static void drawArc(DrawContext ctx, int cx, int cy, int r, float progress, int color) {
        if (r < 3) return;
        int size = r * 2 + 2;
        int thickness = Math.max(2, r / 5);
        Identifier[] arcs = RingTextureCache.getArcs(size, thickness);
        int idx = (int) Math.round(Math.max(0f, Math.min(1f, progress)) * RingTextureCache.ARC_STEPS);
        if (idx <= 0) return;
        int x = cx - r - 1;
        int y = cy - r - 1;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, arcs[idx],
                x, y, 0f, 0f, size, size, size, size, size, size, color);
    }

    private static void addIf(List<ItemStack> list, PlayerEntity player, Item item,
                              boolean enabled, boolean alwaysShow) {
        if (!enabled) return;
        if (!hasItem(player, item) && !alwaysShow) return;
        list.add(new ItemStack(item));
    }

    private static boolean hasItem(PlayerEntity player, Item item) {
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).isOf(item)) return true;
        }
        return false;
    }

    private static float estimateTotal(Item item) {
        if (item == Items.ENDER_PEARL) return 1.0f;
        if (item == Items.CHORUS_FRUIT) return 1.0f;
        if (item == Items.GOLDEN_APPLE) return 1.5f;
        if (item == Items.TOTEM_OF_UNDYING) return 0.5f;
        if (item == Items.ENDER_EYE) return 1.0f;
        return 1.0f;
    }
}