package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScaffoldModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));
    public final NumberSetting delay = add(new NumberSetting("Delay", 4, 3, 8, 1));
    private static final Random RNG = new Random();
    private int wait;
    private boolean armed;

    public ScaffoldModule() {
        super("Scaffold", "Builds under you (no rotation)", Category.WORLD, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        armed = false;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null || client.interactionManager == null || player.networkHandler == null) return;
        if (client.currentScreen != null) return;
        if (!holdBlocks(player)) return;
        boolean rage = mode.is("Rage");
        if (wait > 0) { wait--; return; }

        BlockPos below = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ()).down();
        BlockPos target = null;
        BlockHitResult hit = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : spots(player, below, rage)) {
            if (!world.getBlockState(pos).isReplaceable()) continue;
            if (player.getBoundingBox().intersects(new Box(pos))) continue;
            if (!rage && !legitReach(player, pos)) continue;
            BlockHitResult support = support(world, pos);
            if (support == null) continue;
            if (player.getEyePos().squaredDistanceTo(support.getPos()) > 17.64) continue;
            double score = player.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (pos.equals(below)) score -= 8;
            if (score < best) {
                best = score;
                target = pos;
                hit = support;
            }
        }
        if (target == null || hit == null) return;

        // БЕЗ rotation — сервер получит только interact
        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);
        wait = delay.getInt() + RNG.nextInt(2);
    }

    private boolean holdBlocks(net.minecraft.client.network.ClientPlayerEntity player) {
        var inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        ItemStack held = inventory.getStack(selected);
        if (isBlock(held)) {
            armed = true;
            return true;
        }
        if (!held.isEmpty() || !armed) {
            armed = false;
            return false;
        }
        int next = nextBlock(inventory, selected);
        if (next < 0) {
            armed = false;
            return false;
        }
        CombatUtil.syncSlot(player, next);
        return true;
    }

    private static int nextBlock(net.minecraft.entity.player.PlayerInventory inventory, int selected) {
        for (int step = 1; step < 9; step++) {
            int slot = (selected + step) % 9;
            if (isBlock(inventory.getStack(slot))) return slot;
        }
        return -1;
    }

    private static boolean isBlock(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem block && block.getBlock().getDefaultState().isSolid();
    }

    private static boolean legitReach(net.minecraft.client.network.ClientPlayerEntity player, BlockPos pos) {
        if (player.getVelocity().y > 0.08 && !player.isOnGround()) return false;
        return player.getY() - (pos.getY() + 1.0) <= 0.75;
    }

    private static List<BlockPos> spots(net.minecraft.client.network.ClientPlayerEntity player, BlockPos below, boolean rage) {
        Direction facing = moving(player);
        List<BlockPos> spots = new ArrayList<>();
        spots.add(below);
        spots.add(below.offset(facing));
        spots.add(below.offset(player.getHorizontalFacing()));
        if (!rage) return spots;
        for (Direction direction : Direction.Type.HORIZONTAL) spots.add(below.offset(direction));
        spots.add(below.up());
        return spots;
    }

    private static Direction moving(net.minecraft.client.network.ClientPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        if (velocity.x * velocity.x + velocity.z * velocity.z < 1.0e-4) return player.getHorizontalFacing();
        if (Math.abs(velocity.x) > Math.abs(velocity.z)) return velocity.x > 0 ? Direction.EAST : Direction.WEST;
        return velocity.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockHitResult support(net.minecraft.world.World world, BlockPos air) {
        for (Direction direction : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            BlockPos neighbor = air.offset(direction);
            BlockState state = world.getBlockState(neighbor);
            if (!state.isSolid()) continue;
            Direction face = direction.getOpposite();
            Vec3d point = Vec3d.ofCenter(neighbor).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            return new BlockHitResult(point, face, neighbor, false);
        }
        return null;
    }
}