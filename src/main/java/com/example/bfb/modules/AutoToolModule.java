package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.BooleanSetting;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class AutoToolModule extends CheatModule {
    public final BooleanSetting back = add(new BooleanSetting("Switch back", false));
    private int returnSlot = -1;

    public AutoToolModule() {
        super("AutoTool", "Picks the best tool in hotbar", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        restore();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.interactionManager == null || client.currentScreen != null) return;
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState state = player.getEntityWorld().getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        int selected = player.getInventory().getSelectedSlot();
        int bestSlot = selected;
        float best = player.getInventory().getStack(selected).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            float speed = player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (speed > best + 0.05f) {
                best = speed;
                bestSlot = i;
            }
        }
        if (bestSlot == selected) return;
        if (returnSlot < 0) returnSlot = selected;
        CombatUtil.syncSlot(player, bestSlot);
    }

    private void restore() {
        if (!back.get() || returnSlot < 0) {
            returnSlot = -1;
            return;
        }
        var player = BfbMod.getClient().player;
        if (player != null) CombatUtil.syncSlot(player, returnSlot);
        returnSlot = -1;
    }
}
