package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

/** Автоматически ест золотое яблоко при низком ХП (как Auto GApple в Celestial). */
public class AutoGapModule extends CheatModule {
    public final NumberSetting health = add(new NumberSetting("Health", 10, 1, 20, 1));
    public final BooleanSetting enchantedFirst = add(new BooleanSetting("Enchanted First", true));
    private boolean eating;
    private int previousSlot = -1;

    public AutoGapModule() {
        super("AutoGap", "Eats golden apple when low hp", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onStartTick() {
        MinecraftClient client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) {
            release(client, player);
            return;
        }
        boolean gapInHand = isGap(player.getMainHandStack());
        if (player.getHealth() > health.getInt() && !eating) {
            release(client, player);
            return;
        }
        if (player.getHealth() > health.getInt() && eating && gapInHand && !player.isUsingItem()) {
            release(client, player);
            return;
        }
        if (!gapInHand) {
            int slot = findGap(player);
            if (slot < 0) {
                release(client, player);
                return;
            }
            if (previousSlot < 0) previousSlot = player.getInventory().getSelectedSlot();
            com.example.bfb.CombatUtil.syncSlot(player, slot);
        }
        eating = true;
        client.options.useKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        release(BfbMod.getClient(), BfbMod.getClient().player);
    }

    private void release(MinecraftClient client, ClientPlayerEntity player) {
        if (client.options != null) client.options.useKey.setPressed(false);
        if (player != null && previousSlot >= 0) {
            com.example.bfb.CombatUtil.syncSlot(player, previousSlot);
        }
        previousSlot = -1;
        eating = false;
    }

    private boolean isGap(ItemStack stack) {
        return stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private int findGap(ClientPlayerEntity player) {
        int normal = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) && enchantedFirst.get()) return i;
            if (stack.isOf(Items.GOLDEN_APPLE) && normal < 0) normal = i;
        }
        return normal;
    }
}
