package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.mixin.KeyBindingAccessor;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class AutoEatModule extends CheatModule {
    private static final int OFFHAND = -2;
    public final NumberSetting hunger = add(new NumberSetting("Hunger", 16, 1, 20, 1));
    private boolean holding;
    private int previousSlot = -1;
    private int swappedFrom = -1;

    public AutoEatModule() {
        super("AutoEat", "Eats food from hotbar and inventory", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onStartTick() {
        MinecraftClient client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) {
            release(client, player);
            return;
        }
        if (player.getHungerManager().getFoodLevel() >= hunger.getInt() || !player.getHungerManager().isNotFull()) {
            release(client, player);
            return;
        }
        if (!edible(player.getMainHandStack(), player)) {
            int slot = findFood(player);
            if (slot < 0) {
                release(client, player);
                return;
            }
            moveToHand(client, player, slot);
        }
        client.options.useKey.setPressed(true);
        holding = true;
    }

    @Override
    public void onDisable() {
        release(BfbMod.getClient(), BfbMod.getClient().player);
    }

    private void moveToHand(MinecraftClient client, ClientPlayerEntity player, int slot) {
        PlayerInventory inventory = player.getInventory();
        if (slot >= 0 && slot < 9) {
            if (previousSlot < 0) previousSlot = inventory.getSelectedSlot();
            com.example.bfb.CombatUtil.syncSlot(player, slot);
            return;
        }
        int selected = inventory.getSelectedSlot();
        if (previousSlot < 0) previousSlot = selected;
        int screenSlot = slot == OFFHAND ? 45 : slot;
        if (swappedFrom != slot) {
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId, screenSlot, selected, SlotActionType.SWAP, player);
            swappedFrom = slot;
        }
    }

    private void release(MinecraftClient client, ClientPlayerEntity player) {
        if (!holding && previousSlot < 0 && swappedFrom < 0) return;
        if (client != null && client.options != null && !physicallyDown(client)) {
            client.options.useKey.setPressed(false);
            if (player != null && client.interactionManager != null && player.isUsingItem()) {
                client.interactionManager.stopUsingItem(player);
            }
        }
        if (player != null && client != null && client.interactionManager != null && swappedFrom >= 0) {
            int screenSlot = swappedFrom == OFFHAND ? 45 : swappedFrom;
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId, screenSlot, player.getInventory().getSelectedSlot(), SlotActionType.SWAP, player);
        } else if (player != null && previousSlot >= 0) {
            com.example.bfb.CombatUtil.syncSlot(player, previousSlot);
        }
        holding = false;
        previousSlot = -1;
        swappedFrom = -1;
    }

    private static int findFood(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (edible(inventory.getStack(i), player)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (edible(inventory.getStack(i), player)) return i;
        }
        if (edible(player.getOffHandStack(), player)) return OFFHAND;
        return -1;
    }

    private static boolean edible(ItemStack stack, ClientPlayerEntity player) {
        if (stack.isEmpty()) return false;
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) return player.canConsume(food.canAlwaysEat());
        ConsumableComponent consumable = stack.get(DataComponentTypes.CONSUMABLE);
        return consumable != null && consumable.useAction() == UseAction.EAT && player.canConsume(false);
    }

    private static boolean physicallyDown(MinecraftClient client) {
        InputUtil.Key key = ((KeyBindingAccessor) client.options.useKey).bfb$boundKey();
        long handle = client.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, key.getCode()) == GLFW.GLFW_PRESS;
    }
}
