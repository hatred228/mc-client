package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.glfw.GLFW;

public class BlockOverlayModule extends CheatModule {
    public final ColorSetting color = add(new ColorSetting("Color", 0x7C5CFF));
    public final BooleanSetting fill = add(new BooleanSetting("Fill", true));

    public BlockOverlayModule() {
        super("BlockOverlay", "Outlines the block under the crosshair", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        var state = client.world.getBlockState(pos);
        if (state.isAir()) return;
        VoxelShape shape = state.getOutlineShape(client.world, pos);
        Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos).expand(0.002);
        int rgb = color.getRgb();
        if (fill.get()) RenderUtil.fill(matrices, consumers, box, camera, 0x33000000 | rgb);
        RenderUtil.outline(matrices, consumers, box, camera, 0xEE000000 | rgb, 2.2f);
    }
}
