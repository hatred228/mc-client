package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.HudStyle;
import com.example.bfb.SalFont;
import com.example.bfb.gui.RingTextureCache;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class CrossbowChargeModule extends CheatModule {
    public final NumberSetting offsetX = add(new NumberSetting("Offset X", 0, -100, 100, 1));
    public final NumberSetting offsetY = add(new NumberSetting("Offset Y", 20, -50, 100, 1));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 24, 12, 60, 1));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", 4, 2, 8, 1));

    public final ColorSetting bgColor = add(new ColorSetting("Background", 0x60000000));
    public final ColorSetting barColor = add(new ColorSetting("Bar color", 0xFFFFE14D));
    public final ColorSetting readyColor = add(new ColorSetting("Ready color", 0xFF22FF44));

    public final BooleanSetting showPercent = add(new BooleanSetting("Show percent", true));
    public final BooleanSetting showMultishot = add(new BooleanSetting("Show multishot dots", true));

    public CrossbowChargeModule() {
        super("CrossbowCharge", "Crossbow charge indicator", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client) {
        CrossbowChargeModule mod = com.example.bfb.ModuleManager.get(CrossbowChargeModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;

        // только когда держим арбалет
        ItemStack stack = client.player.getMainHandStack();
        if (!(stack.getItem() instanceof CrossbowItem)) stack = client.player.getOffHandStack();
        if (!(stack.getItem() instanceof CrossbowItem)) return;

        // прогресс заряда 0..1
        float progress;
        boolean charged = CrossbowItem.isCharged(stack);
        if (charged) {
            progress = 1f;
        } else if (client.player.isUsingItem() && client.player.getActiveItem().getItem() instanceof CrossbowItem) {
            int total = CrossbowItem.getPullTime(stack, client.player);
            int used = client.player.getItemUseTime();
            progress = total <= 0 ? 0f : Math.min(1f, used / (float) total);
        } else {
            progress = 0f;
        }

        int cx = context.getScaledWindowWidth() / 2 + mod.offsetX.getInt();
        int cy = context.getScaledWindowHeight() / 2 + mod.offsetY.getInt();
        int r = mod.radius.getInt();
        int thick = mod.thickness.getInt();

        // фон — круг
        HudStyle.circle(context, cx, cy, r + 4, mod.bgColor.getRgb());

        // прогресс-дуга через запечённую текстуру
        int arcSize = r * 2 + 2;
        int arcThickness = Math.max(2, thick);
        Identifier[] arcs = RingTextureCache.getArcs(arcSize, arcThickness);
        int arcX = cx - r - 1;
        int arcY = cy - r - 1;

        int barRgb = charged
                ? (mod.readyColor.getRgb() & 0xFFFFFF)
                : (mod.barColor.getRgb() & 0xFFFFFF);

        // фоновая дуга — полный круг тёмным
        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                arcs[RingTextureCache.ARC_STEPS],
                arcX, arcY, 0f, 0f, arcSize, arcSize, arcSize, arcSize, arcSize, arcSize,
                0xFF2A2A38);

        // заполненная часть
        int arcIdx = (int) Math.round(progress * RingTextureCache.ARC_STEPS);
        if (arcIdx > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED,
                    arcs[arcIdx],
                    arcX, arcY, 0f, 0f, arcSize, arcSize, arcSize, arcSize, arcSize, arcSize,
                    0xFF000000 | barRgb);
        }

        // процент
        if (mod.showPercent.get()) {
            String text = Math.round(progress * 100) + "%";
            int w = SalFont.width(text);
            SalFont.draw(context, text, cx - w / 2, cy - SalFont.height() / 2,
                    0xFF000000 | (charged ? barRgb : 0xFFFFFFFF));
        }

        // 3 точки для мультишота
        if (mod.showMultishot.get() && hasMultishot(client.player)) {
            int dotR = 3;
            int dotSpread = r + 10;
            for (int i = -1; i <= 1; i++) {
                int dx = (int) (Math.sin(Math.toRadians(i * 12)) * dotSpread);
                int px = cx + dx;
                int py = cy + dotSpread;
                context.fill(px - dotR, py - dotR, px + dotR, py + dotR,
                        (charged ? barRgb : 0x80FFFFFF));
            }
        }
    }

    private static boolean hasMultishot(net.minecraft.client.network.ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof CrossbowItem)) stack = player.getOffHandStack();
        if (!(stack.getItem() instanceof CrossbowItem)) return false;
        try {
            var registry = player.getEntityWorld().getRegistryManager()
                    .getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
            int level = net.minecraft.enchantment.EnchantmentHelper.getLevel(
                    registry.getOrThrow(net.minecraft.enchantment.Enchantments.MULTISHOT), stack);
            return level > 0;
        } catch (Exception e) {
            return false;
        }
    }
}