package com.example.bfb;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class GearItems {
    private GearItems() {
    }

    public static boolean lyingGear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) return true;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.contains("sword") || path.contains("_axe") || path.contains("trident")
                || path.contains("mace") || path.contains("spear");
    }
}
