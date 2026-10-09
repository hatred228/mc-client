package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class NukerModule extends CheatModule {
    public static BlockPos pos1;
    public static BlockPos pos2;

    public final ModeSetting mode = add(new ModeSetting("Mode", "Radius", "Radius", "Selection"));
    public final NumberSetting radius = add(new NumberSetting("Radius", 3, 1, 5, 1));
    public final NumberSetting delay = add(new NumberSetting("Delay", 5, 4, 12, 1));
    public final ModeSetting blocks = add(new ModeSetting("Blocks", "All", "All", "Ores", "Wood", "Stone"));

    private static final Random RNG = new Random();
    private int wait;
    private int layer;
    private boolean wasUse;

    public NukerModule() {
        super("Nuker", "Breaks blocks without rotation", Category.WORLD, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;
        mark(client);
        if (wait > 0) { wait--; return; }

        Vec3d origin = new Vec3d(player.getX(), player.getY(), player.getZ());
        BlockPos target = mode.is("Selection") ? nextSelection(origin) : nextRadius(origin);
        if (target == null) return;

        // reach <= 4.2
        if (player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(target)) > 17.64) return;
        // LOS через raycast
        if (!canSee(player, target)) return;

        // БЕЗ rotation — ломаем только если смотрим (или почти смотрим)
        Direction side = facingSide(player, target);
        client.interactionManager.updateBlockBreakingProgress(target, side);
        player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        int base = delay.getInt();
        wait = base + RNG.nextInt(3);
    }

    private static boolean canSee(ClientPlayerEntity player, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        Vec3d eye = player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                eye, center, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        return hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(pos);
    }

    private static Direction facingSide(ClientPlayerEntity player, BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = eye.x - center.x;
        double dy = eye.y - center.y;
        double dz = eye.z - center.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private BlockPos nextRadius(Vec3d origin) {
        MinecraftClient client = BfbMod.getClient();
        int r = radius.getInt();
        BlockPos center = BlockPos.ofFloored(origin);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (!wanted(client.world.getBlockState(pos))) continue;
                    double d = origin.squaredDistanceTo(Vec3d.ofCenter(pos));
                    if (d > 17.64 || d >= bestDist) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    private BlockPos nextSelection(Vec3d origin) {
        if (pos1 == null || pos2 == null) return null;
        MinecraftClient client = BfbMod.getClient();
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        int minY = client.world == null ? Math.min(pos1.getY(), pos2.getY()) : client.world.getBottomY();
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int layers = maxY - minY + 1;
        if (layers < 1) return null;
        for (int step = 0; step < 6; step++) {
            if (layer >= layers) layer = 0;
            int y = maxY - layer;
            BlockPos found = null;
            double best = Double.MAX_VALUE;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!wanted(client.world.getBlockState(pos))) continue;
                    double dist = origin.distanceTo(Vec3d.ofCenter(pos));
                    if (dist > 4.2 || dist >= best) continue;
                    best = dist;
                    found = pos;
                }
            }
            if (found != null) return found;
            layer++;
        }
        return null;
    }

    private boolean wanted(BlockState state) {
        if (state.isAir()) return false;
        if (blocks.is("Ores")) return state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES)
                || state.isOf(Blocks.ANCIENT_DEBRIS) || state.isOf(Blocks.NETHER_QUARTZ_ORE) || state.isOf(Blocks.NETHER_GOLD_ORE);
        if (blocks.is("Wood")) return state.isIn(BlockTags.LOGS);
        if (blocks.is("Stone")) return state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isOf(Blocks.NETHERRACK)
                || state.isOf(Blocks.END_STONE) || state.isOf(Blocks.BLACKSTONE) || state.isOf(Blocks.DEEPSLATE);
        return true;
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        Box box = zone();
        if (box == null) return;
        RenderUtil.fill(matrices, consumers, box, camera, 0x553C2E78);
        RenderUtil.outline(matrices, consumers, box, camera, 0xFF7C5CFF, 2.2f);
    }

    private void mark(MinecraftClient client) {
        if (!mode.is("Selection") || client.currentScreen != null) {
            wasUse = false;
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean pressed = down && !wasUse;
        wasUse = down;
        if (!pressed || !(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (pos1 == null || pos2 != null) {
            pos1 = hit.getBlockPos();
            pos2 = null;
            layer = 0;
        } else {
            pos2 = hit.getBlockPos();
            layer = 0;
        }
        if (client.player != null) {
            String which = pos2 == null ? "A" : "B";
            BlockPos shown = pos2 == null ? pos1 : pos2;
            client.player.sendMessage(Text.literal("§dNuker " + which + ": §f" + shown.toShortString()), true);
        }
    }

    private Box zone() {
        if (mode.is("Selection")) {
            if (pos1 == null) return null;
            MinecraftClient client = BfbMod.getClient();
            int bottom = client.world == null ? pos1.getY() : client.world.getBottomY();
            BlockPos second = pos2 == null ? pos1 : pos2;
            int minX = Math.min(pos1.getX(), second.getX());
            int minZ = Math.min(pos1.getZ(), second.getZ());
            int maxX = Math.max(pos1.getX(), second.getX()) + 1;
            int maxZ = Math.max(pos1.getZ(), second.getZ()) + 1;
            int maxY = Math.max(pos1.getY(), second.getY()) + 1;
            return new Box(minX, bottom, minZ, maxX, maxY, maxZ);
        }
        ClientPlayerEntity player = BfbMod.getClient().player;
        if (player == null) return null;
        BlockPos center = player.getBlockPos();
        int r = radius.getInt();
        return new Box(center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r + 1, center.getY() + r + 1, center.getZ() + r + 1);
    }
}