package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class XRayModule extends CheatModule {
    public static XRayModule instance;
    public final BooleanSetting diamond = add(new BooleanSetting("Diamond", true));
    public final BooleanSetting emerald = add(new BooleanSetting("Emerald", true));
    public final BooleanSetting gold = add(new BooleanSetting("Gold", true));
    public final BooleanSetting iron = add(new BooleanSetting("Iron", true));
    public final BooleanSetting coal = add(new BooleanSetting("Coal", false));
    public final BooleanSetting copper = add(new BooleanSetting("Copper", false));
    public final BooleanSetting redstone = add(new BooleanSetting("Redstone", true));
    public final BooleanSetting lapis = add(new BooleanSetting("Lapis", true));
    public final BooleanSetting netherite = add(new BooleanSetting("Netherite", true));
    public final BooleanSetting ancient = add(new BooleanSetting("Ancient debris", true));
    public final BooleanSetting quartz = add(new BooleanSetting("Quartz", false));
    public final BooleanSetting spawners = add(new BooleanSetting("Spawners", true));
    public final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    public final BooleanSetting caveOnly = add(new BooleanSetting("Cave only", true));
    public final BooleanSetting hideStone = add(new BooleanSetting("Hide stone", true));
    public final BooleanSetting proximityOnly = add(new BooleanSetting("Proximity only", true));

    private String signature = "";

    public XRayModule() {
        super("XRay", "Ores and chests through stone", Category.WORLD, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    public boolean wants(BlockState state) {
        Block block = state.getBlock();
        if (diamond.get() && state.isIn(BlockTags.DIAMOND_ORES)) return true;
        if (emerald.get() && state.isIn(BlockTags.EMERALD_ORES)) return true;
        if (gold.get() && (state.isIn(BlockTags.GOLD_ORES) || block == Blocks.NETHER_GOLD_ORE)) return true;
        if (iron.get() && state.isIn(BlockTags.IRON_ORES)) return true;
        if (coal.get() && state.isIn(BlockTags.COAL_ORES)) return true;
        if (copper.get() && state.isIn(BlockTags.COPPER_ORES)) return true;
        if (redstone.get() && state.isIn(BlockTags.REDSTONE_ORES)) return true;
        if (lapis.get() && state.isIn(BlockTags.LAPIS_ORES)) return true;
        if (netherite.get() && (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK)) return true;
        if (ancient.get() && block == Blocks.ANCIENT_DEBRIS) return true;
        if (quartz.get() && block == Blocks.NETHER_QUARTZ_ORE) return true;
        if (spawners.get() && (block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER)) return true;
        return chests.get() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST
                || block == Blocks.ENDER_CHEST || block == Blocks.BARREL || state.isIn(BlockTags.SHULKER_BOXES));
    }

    /** Проверяет, видна ли руда (есть ли рядом воздух с любой стороны). */
    public boolean isVisible(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState st = client.world.getBlockState(neighbor);
            if (st.isAir() || !st.isOpaqueFullCube()) return true;
        }
        return false;
    }

    /** Проверяет, находится ли игрок рядом с рудой. */
    public boolean isNearby(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        return client.player.getBlockPos().isWithinDistance(pos, 16);
    }

    /** Если Cave only — рисуем только "видимые" руды. */
    public boolean shouldDraw(BlockPos pos) {
        if (caveOnly.get() && !isVisible(pos)) return false;
        if (proximityOnly.get() && !isNearby(pos)) return false;
        return true;
    }

    @Override
    public void onTick() {
        String now = signature();
        if (now.equals(signature)) return;
        signature = now;
        reload();
    }

    @Override
    public void onDisable() {
        signature = "";
        reload();
    }

    private String signature() {
        StringBuilder builder = new StringBuilder();
        for (var setting : getSettings()) builder.append(setting.serialize()).append('|');
        return builder.toString();
    }

    private static void reload() {
        var client = BfbMod.getClient();
        if (client.worldRenderer != null) client.worldRenderer.reload();
    }
}