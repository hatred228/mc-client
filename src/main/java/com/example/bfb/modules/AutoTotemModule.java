package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class AutoTotemModule extends CheatModule {
    public final NumberSetting health = add(new NumberSetting("Health", 16, 1, 20, 1));
    public final BooleanSetting alwaysHold = add(new BooleanSetting("Always hold", false));

    private static final Random RNG = new Random();
    private int cooldown;

    public AutoTotemModule() {
        super("AutoTotem", "Totem with dynamic 250ms+ delay", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        boolean low = player.getHealth() <= health.get();
        if (!low && !alwaysHold.get() && !player.getOffHandStack().isEmpty()) return;

        // 30% шанс пропустить если HP > 10
        if (!low && player.getHealth() > 10 && RNG.nextFloat() < 0.3f) return;

        for (int i = 0; i < 36; i++) {
            if (!player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) continue;
            int slot = i < 9 ? i + 36 : i;
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, player);

            // 250-400ms динамическая задержка (гауссово)
            double mean = 13; // 325ms
            double std = 3;
            int ticks = (int) Math.round(mean + RNG.nextGaussian() * std);
            cooldown = Math.max(10, Math.min(20, ticks)); // 250-500ms
            return;
        }
    }
}