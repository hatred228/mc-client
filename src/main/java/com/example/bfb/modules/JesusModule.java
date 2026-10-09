package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class JesusModule extends CheatModule {
    public final BooleanSetting water = add(new BooleanSetting("Water", true));
    public final BooleanSetting lava = add(new BooleanSetting("Lava", false));

    public JesusModule() {
        super("Jesus", "Walk on water and lava", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null || player.isSneaking()) return;
        if (player.networkHandler == null) return;
        BlockPos below = BlockPos.ofFloored(player.getX(), player.getY() - 0.2, player.getZ());
        boolean onWater = water.get() && (player.isTouchingWater() || world.getFluidState(below).isIn(FluidTags.WATER));
        boolean onLava = lava.get() && (player.isInLava() || world.getFluidState(below).isIn(FluidTags.LAVA));
        if (!onWater && !onLava) return;
        var velocity = player.getVelocity();
        double newY = velocity.y > 0 ? 0.04 : 0.0;
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(), player.getY() + (newY > 0 ? 0.01 : 0), player.getZ(), true, player.horizontalCollision));
        player.setVelocity(velocity.x, newY, velocity.z);
        player.fallDistance = 0;
    }
}