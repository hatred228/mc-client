package com.example.bfb.hud.widgets;

import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.ListHudWidget;
import com.example.bfb.render.ui.Icons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CooldownsWidget extends ListHudWidget<CooldownsWidget.Row> {

    public CooldownsWidget(HudCompactStyle.Options options) {
        super("Cooldowns", Icons.TIMER, 0.02f, 0.55f, options);
    }

    @Override
    protected List<Row> entries() {
        if (mc.player == null) return List.of();

        var inv = mc.player.getInventory();
        var manager = mc.player.getItemCooldownManager();

        List<Row> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            if (!manager.isCoolingDown(stack)) continue;

            String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
            if (!seen.add(id)) continue;

            float progress = manager.getCooldownProgress(stack, 0f);
            if (progress <= 0f) continue;
            result.add(new Row(stack.copy(), progress));
        }
        return result;
    }

    @Override
    protected Object key(Row entry) {
        return net.minecraft.registry.Registries.ITEM.getId(entry.stack.getItem()).toString();
    }

    @Override protected String left(Row entry) { return entry.stack.getName().getString(); }

    @Override
    protected String right(Row entry) {
        int percent = Math.round((1f - entry.progress) * 100f);
        return percent + "%";
    }

    @Override protected String valueOption() { return "Duration"; }

    @Override protected boolean nativeIcon(Row entry) { return true; }

    @Override
    protected void drawNativeIcon(DrawContext ctx, Row entry,
                                   float iconX, float iconY, float iconSize, int alpha) {
        // Иконка таймера
        com.example.bfb.render.ui.IconFont.draw(ctx, Icons.TIMER,
                iconX, iconY, iconSize, (alpha << 24) | 0xFFFFFFFF);
    }

    public static final class Row {
        public final ItemStack stack;
        public final float progress;
        public Row(ItemStack stack, float progress) {
            this.stack = stack;
            this.progress = progress;
        }
    }
}