package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.BooleanSetting;
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

/**
 * Scaffold — строит под ногами.
 *
 * Фазовая машина:
 *   Phase 0 = idle (поиск spot)
 *   Phase 1 = syncSlot (если блок не в руке)
 *   Phase 2 = interactBlock
 *
 * Каждая фаза = 1 тик. Без двойных пакетов.
 */
public class ScaffoldModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));
    public final NumberSetting delay = add(new NumberSetting("Delay", 4, 3, 8, 1));
    public final BooleanSetting silentRotate = add(new BooleanSetting("Silent rotate", true));
    public final BooleanSetting autoSwitch = add(new BooleanSetting("Auto switch", true));

    private static final Random RNG = new Random();
    private int wait;
    private boolean armed;

    private int phase = 0;
    private int phaseTtl = 0;
    private BlockHitResult pendingHit = null;
    private int savedSlot = -1;

    public ScaffoldModule() {
        super("Scaffold", "Builds under you", Category.WORLD, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        armed = false;
        reset();
    }

    private void reset() {
        phase = 0;
        phaseTtl = 0;
        pendingHit = null;
        savedSlot = -1;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null || client.interactionManager == null) {
            reset();
            return;
        }
        if (client.currentScreen != null) {
            reset();
            return;
        }

        if (phase != 0) {
            tickPhase(client, player);
            return;
        }

        if (wait > 0) { wait--; return; }

        // ищем spot для установки
        BlockPos below = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ()).down();
        BlockPos target = null;
        BlockHitResult hit = null;
        double best = Double.MAX_VALUE;

        for (BlockPos pos : spots(player, below, mode.is("Rage"))) {
            if (!world.getBlockState(pos).isReplaceable()) continue;
            if (player.getBoundingBox().intersects(new Box(pos))) continue;

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

        // ищем блок в руке
        int slot = findBlockSlot(player);
        if (slot < 0) return;

        pendingHit = hit;
        savedSlot = player.getInventory().getSelectedSlot();

        if (silentRotate.get()) {
            float[] rot = com.example.bfb.AnticheatBypass.lookAt(player.getEyePos(), hit.getPos());
            com.example.bfb.RotationManager.setAndSend(rot[0], rot[1], 4);
            player.setBodyYaw(rot[0]);
            player.setHeadYaw(rot[0]);
        }

        if (player.getInventory().getSelectedSlot() != slot && autoSwitch.get()) {
            CombatUtil.syncSlot(player, slot);
            phase = 1;
        } else {
            phase = 2;
        }
        phaseTtl = 4;
    }

    private void tickPhase(net.minecraft.client.MinecraftClient client,
                           net.minecraft.client.network.ClientPlayerEntity player) {
        if (--phaseTtl <= 0) {
            reset();
            return;
        }

        if (pendingHit == null) { reset(); return; }

        switch (phase) {
            case 1 -> {
                // слот уже переключён — теперь interact
                phase = 2;
                phaseTtl = 4;
            }
            case 2 -> {
                client.interactionManager.interactBlock(player, Hand.MAIN_HAND, pendingHit);
                // interactBlock сам вызывает swingHand
                int base = switch (CombatController.mode()) {
                    case GRIM, POLAR, REALLYWORLD, DONUTSMP -> 5 + RNG.nextInt(3);
                    case SPOOKYTIME -> 4 + RNG.nextInt(2);
                    default -> delay.getInt() + RNG.nextInt(2);
                };
                wait = base;
                reset();
            }
        }
    }

    private int findBlockSlot(net.minecraft.client.network.ClientPlayerEntity player) {
        var inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        if (isBlock(inv.getStack(selected))) return selected;

        for (int step = 1; step < 9; step++) {
            int slot = (selected + step) % 9;
            if (isBlock(inv.getStack(slot))) return slot;
        }
        return -1;
    }

    private static boolean isBlock(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem block
                && block.getBlock().getDefaultState().isSolid();
    }

    private static List<BlockPos> spots(net.minecraft.client.network.ClientPlayerEntity player,
                                        BlockPos below, boolean rage) {
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
        for (Direction direction : new Direction[]{
                Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                Direction.WEST, Direction.EAST, Direction.UP}) {
            BlockPos neighbor = air.offset(direction);
            BlockState state = world.getBlockState(neighbor);
            if (!state.isSolid()) continue;
            Direction face = direction.getOpposite();
            Vec3d point = Vec3d.ofCenter(neighbor).add(
                    face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            return new BlockHitResult(point, face, neighbor, false);
        }
        return null;
    }
}