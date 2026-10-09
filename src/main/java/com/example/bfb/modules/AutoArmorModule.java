package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class AutoArmorModule extends CheatModule {
    public final NumberSetting delay = add(new NumberSetting("Delay", 5, 4, 12, 1));
    private static final Random RNG = new Random();
    private int wait;

    // phase-машина для трёх clickSlot
    private int armorPhase = 0;  // 0=idle, 1=pickup, 2=put, 3=finish
    private int armorFrom = -1;
    private int armorTo = -1;
    private int armorSync = -1;
    private int armorTtl = 0;

    public AutoArmorModule() {
        super("AutoArmor", "Swaps to stronger armor", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        resetArmor();
        wait = 0;
    }

    private void resetArmor() {
        armorPhase = 0;
        armorFrom = -1;
        armorTo = -1;
        armorSync = -1;
        armorTtl = 0;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null) {
            resetArmor();
            return;
        }
        // если инвентарь не playerScreenHandler — отмена
        if (player.currentScreenHandler != player.playerScreenHandler) {
            resetArmor();
            return;
        }

        // ведём phase до конца
        if (armorPhase != 0) {
            tickArmor(client, player);
            return;
        }

        if (wait > 0) { wait--; return; }

        int from = -1;
        EquipmentSlot equip = null;
        double best = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            double worn = score(player.getEquippedStack(slot), slot);
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                double value = score(stack, slot);
                double gain = value - Math.max(worn, 0);
                if (value >= 0 && gain > best) { best = gain; from = i; equip = slot; }
            }
        }
        if (from < 0 || equip == null || best <= 0) return;

        int sync = player.playerScreenHandler.syncId;
        int inventorySlot = from < 9 ? from + 36 : from;
        int armorSlot = switch (equip) {
            case HEAD -> 5; case CHEST -> 6; case LEGS -> 7; case FEET -> 8; default -> -1;
        };
        if (armorSlot < 0) return;

        armorFrom = inventorySlot;
        armorTo = armorSlot;
        armorSync = sync;
        armorTtl = 6;  // 6 тиков на всю операцию, иначе откат

        client.interactionManager.clickSlot(sync, armorFrom, 0, SlotActionType.PICKUP, player);
        armorPhase = 1;
    }

    private void tickArmor(net.minecraft.client.MinecraftClient client,
                           net.minecraft.client.network.ClientPlayerEntity player) {
        if (--armorTtl <= 0) {
            resetArmor();
            return;
        }
        // валидация: тот же handler?
        if (player.currentScreenHandler != player.playerScreenHandler
                || player.playerScreenHandler.syncId != armorSync) {
            resetArmor();
            return;
        }

        if (armorPhase == 1) {
            client.interactionManager.clickSlot(armorSync, armorTo, 0, SlotActionType.PICKUP, player);
            armorPhase = 2;
            return;
        }
        if (armorPhase == 2) {
            client.interactionManager.clickSlot(armorSync, armorFrom, 0, SlotActionType.PICKUP, player);
            armorPhase = 0;
            wait = 10 + RNG.nextInt(6);
            resetArmor();
        }
    }

    private static double score(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return -1;
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot) return -1;
        double value = 0;
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            double[] found = {0};
            modifiers.applyModifiers(slot, (attribute, modifier) -> {
                if (attribute.matches(EntityAttributes.ARMOR)) found[0] += modifier.value();
            });
            value = found[0];
        }
        if (value <= 0) value = material(stack);
        return value;
    }

    private static double material(ItemStack stack) {
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        if (path.contains("netherite")) return 8.5;
        if (path.contains("diamond")) return 7;
        if (path.contains("iron")) return 5;
        if (path.contains("chainmail")) return 4;
        if (path.contains("gold")) return 3;
        if (path.contains("leather") || path.contains("copper")) return 2;
        return 1;
    }
}