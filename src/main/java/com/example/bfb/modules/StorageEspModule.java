package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class StorageEspModule extends CheatModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", 16, 8, 32, 1));
    public final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    public final BooleanSetting ender = add(new BooleanSetting("Ender", true));
    public final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", true));
    public final BooleanSetting spawners = add(new BooleanSetting("Spawners", true));
    private final List<Box> boxes = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private int cooldown;

    public StorageEspModule() {
        super("StorageESP", "Chests, shulkers and spawners", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (cooldown-- > 0) return;
        cooldown = 20;
        boxes.clear();
        colors.clear();
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;
        int r = radius.getInt();
        BlockPos center = player.getBlockPos();
        int bottom = world.getBottomY();
        int top = bottom + world.getHeight();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                int wy = center.getY() + y;
                if (wy < bottom || wy >= top) continue;
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);
                    int color = colorOf(world.getBlockState(pos));
                    if (color == 0) continue;
                    boxes.add(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1));
                    colors.add(color);
                }
            }
        }
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        for (int i = 0; i < boxes.size(); i++) {
            RenderUtil.outline(matrices, consumers, boxes.get(i), camera, colors.get(i), 1.5f);
        }
    }

    private int colorOf(BlockState state) {
        Block block = state.getBlock();
        if (chests.get() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL)) return 0xFFFFC857;
        if (ender.get() && block == Blocks.ENDER_CHEST) return 0xFFB388FF;
        if (shulkers.get() && state.isIn(BlockTags.SHULKER_BOXES)) return 0xFFFF6AD5;
        if (spawners.get() && (block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER)) return 0xFFFF5A5A;
        return 0;
    }
}
