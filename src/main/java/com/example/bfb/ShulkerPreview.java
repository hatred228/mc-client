package com.example.bfb;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public final class ShulkerPreview {
    private ShulkerPreview() {
    }

    public static void draw(net.minecraft.client.gui.DrawContext context, Slot slot, int mouseX, int mouseY) {
        if (slot == null) return;
        ItemStack stack = slot.getStack();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return;
        int columns = 9;
        int count = 0;
        for (ItemStack ignored : container.iterateNonEmpty()) count++;
        if (count == 0 && container.stream().findAny().isEmpty()) return;
        java.util.List<ItemStack> items = container.stream().toList();
        int rows = Math.max(1, (items.size() + columns - 1) / columns);
        int width = columns * 18 + 8;
        int height = rows * 18 + 22;
        int screenW = context.getScaledWindowWidth();
        int x = mouseX + 14;
        int y = mouseY - height - 18;
        if (x + width > screenW - 4) x = mouseX - width - 14;
        if (x < 4) x = 4;
        if (y < 4) y = 4;
        context.fill(x, y, x + width, y + height, 0xF0101018);
        context.fill(x, y, x + width, y + 2, 0xFF7C5CFF);
        SalFont.draw(context, SalFont.fit(stack.getName().getString(), width - 8), x + 4, y + 5, 0xFFE8EAF6);
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item.isEmpty()) continue;
            int ix = x + 4 + (i % columns) * 18;
            int iy = y + 18 + (i / columns) * 18;
            context.drawItem(item, ix, iy);
            context.drawStackOverlay(net.minecraft.client.MinecraftClient.getInstance().textRenderer, item, ix, iy);
        }
    }
}
