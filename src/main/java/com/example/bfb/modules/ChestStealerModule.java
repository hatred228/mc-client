package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class ChestStealerModule extends CheatModule {
    public final NumberSetting delay = add(new NumberSetting("Delay", 4, 3, 10, 1));
    public final BooleanSetting close = add(new BooleanSetting("Close", true));
    private static final Random RNG = new Random();
    private int wait;

    public ChestStealerModule() {
        super("ChestStealer", "Takes items from chests", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return;
        if (client.player == null || client.interactionManager == null) return;
        if (wait > 0) { wait--; return; }
        boolean space = false;
        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getStack(slot).isEmpty()) { space = true; break; }
        }
        if (!space) {
            if (close.get()) client.player.closeHandledScreen();
            return;
        }
        var handler = screen.getScreenHandler();
        int size = handler.getRows() * 9;
        for (int i = 0; i < size; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) continue;
            client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
            wait = Math.max(3, delay.getInt()) + RNG.nextInt(2);
            return;
        }
        if (close.get()) client.player.closeHandledScreen();
    }
}