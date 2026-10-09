package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.HudStyle;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class MinimapModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "TopLeft",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));

    public final NumberSetting size = add(new NumberSetting("Size", 100, 60, 200, 4));
    public final NumberSetting radius = add(new NumberSetting("Radius", 32, 16, 64, 8));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 2, 1, 5, 1));

    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgInner = add(new ColorSetting("Background inner", 0xFF10182E));
    public final ColorSetting bgOuter = add(new ColorSetting("Background outer", 0xFF05070F));

    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    public final BooleanSetting ores = add(new BooleanSetting("Highlight ores", true));

    private static int[] pixelCache;
    private static int cacheRadius;
    private static double lastX = Double.NaN;
    private static double lastZ = Double.NaN;
    private static int refreshTimer;

    public MinimapModule() {
        super("Minimap", "Round minimap with rotation", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client, float tickDelta) {
        MinimapModule mod = com.example.bfb.ModuleManager.get(MinimapModule.class);
        if (mod == null || !mod.isEnabled() || client.player == null) return;
        var player = client.player;
        ClientWorld world = client.world;
        if (world == null) return;

        int size = mod.size.getInt();
        int radius = mod.radius.getInt();
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        int defX, defY;
        switch (mod.position.get()) {
            case "TopRight" -> { defX = sw - size - 6; defY = 6; }
            case "BottomLeft" -> { defX = 6; defY = sh - size - 6; }
            case "BottomRight" -> { defX = sw - size - 6; defY = sh - size - 6; }
            default -> { defX = 6; defY = 6; }
        }

        int x = HudEditor.drawX("minimap", defX, defY);
        int y = HudEditor.drawY("minimap", defX, defY);
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - mod.edgeWidth.getInt();

        // фон-круг с радиальным градиентом
        HudStyle.circleGradient(context, cx, cy, r,
                mod.bgInner.getRgb() & 0xFFFFFF,
                mod.bgOuter.getRgb() & 0xFFFFFF);

        // кэш пикселей
        double dx = Math.abs(player.getX() - lastX);
        double dz = Math.abs(player.getZ() - lastZ);
        if (pixelCache == null || cacheRadius != radius || dx > 4 || dz > 4 || --refreshTimer <= 0) {
            refresh(world, player.getBlockPos(), radius, mod.ores.get());
            cacheRadius = radius;
            lastX = player.getX();
            lastZ = player.getZ();
            refreshTimer = 30;
        }

        double yawRad = mod.rotate.get() ? Math.toRadians(player.getYaw(tickDelta)) : 0;
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double scale = (double) radius / r;
        int gridSize = radius * 2 + 1;
        int px0 = player.getBlockX() - radius;
        int pz0 = player.getBlockZ() - radius;
        double pcx = player.getX();
        double pcz = player.getZ();

        int[] row = new int[size];
        for (int py = 0; py < size; py++) {
            int relY = py - size / 2;
            int maxDx = (int) Math.sqrt(Math.max(0, (double) r * r - (double) relY * relY));
            double sdz = relY * scale;
            for (int pxi = 0; pxi < size; pxi++) {
                int relX = pxi - size / 2;
                if (Math.abs(relX) > maxDx) {
                    row[pxi] = 0;
                    continue;
                }
                double sdx = relX * scale;
                double wx, wz;
                if (mod.rotate.get()) {
                    wx = sdx * cos - sdz * sin;
                    wz = sdx * sin + sdz * cos;
                } else {
                    wx = sdx;
                    wz = sdz;
                }
                int bx = (int) Math.round(pcx + wx);
                int bz = (int) Math.round(pcz + wz);
                int gx = bx - px0;
                int gz = bz - pz0;
                if (gx < 0 || gz < 0 || gx >= gridSize || gz >= gridSize) {
                    row[pxi] = 0;
                    continue;
                }
                int color = pixelCache[gz * gridSize + gx];
                row[pxi] = (color >>> 24) == 0 ? 0 : color;
            }
            int runStart = 0;
            int runColor = row[0];
            for (int pxi = 1; pxi <= size; pxi++) {
                int next = pxi < size ? row[pxi] : 0xFFFFFFFF;
                if (next != runColor) {
                    if ((runColor >>> 24) > 0 && pxi > runStart) {
                        context.fill(x + runStart, y + py, x + pxi, y + py + 1, runColor);
                    }
                    runStart = pxi;
                    runColor = next;
                }
            }
        }

        // игрок в центре
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF00E0FF);

        // градиентное кольцо
        HudStyle.gradientRing(context, cx, cy, r + mod.edgeWidth.getInt() / 2, mod.edgeWidth.getInt(),
                mod.edgeColor1.getRgb() & 0xFFFFFF, mod.edgeColor2.getRgb() & 0xFFFFFF, 48);

        HudEditor.outline(context, "minimap", x, y, size, size);
    }

    private static void refresh(ClientWorld world, BlockPos center, int radius, boolean ores) {
        int gridSize = radius * 2 + 1;
        if (pixelCache == null || pixelCache.length != gridSize * gridSize) {
            pixelCache = new int[gridSize * gridSize];
        }
        int minX = center.getX() - radius;
        int minZ = center.getZ() - radius;
        int topY = Math.min(world.getTopYInclusive(), center.getY() + 24);
        int botY = Math.max(world.getBottomY(), center.getY() - 40);
        BlockPos.Mutable mp = new BlockPos.Mutable();
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int bx = minX + gx;
                int bz = minZ + gz;
                int color = 0;
                for (int yy = topY; yy >= botY; yy--) {
                    mp.set(bx, yy, bz);
                    BlockState state = world.getBlockState(mp);
                    if (state.isAir()) continue;
                    int c = colorOf(state, ores);
                    if (c != 0) { color = c; break; }
                }
                pixelCache[gz * gridSize + gx] = color;
            }
        }
    }

    private static int colorOf(BlockState state, boolean ores) {
        var block = state.getBlock();
        if (ores) {
            if (state.isIn(net.minecraft.registry.tag.BlockTags.DIAMOND_ORES)) return 0xFF00FFFF;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.EMERALD_ORES)) return 0xFF00FF80;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.GOLD_ORES)) return 0xFFFFD700;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.IRON_ORES)) return 0xFFD0A090;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.REDSTONE_ORES)) return 0xFFFF2020;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.LAPIS_ORES)) return 0xFF2040FF;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.COAL_ORES)) return 0xFF303030;
            if (state.isIn(net.minecraft.registry.tag.BlockTags.COPPER_ORES)) return 0xFFFF8040;
            if (block == Blocks.ANCIENT_DEBRIS) return 0xFF6B4A3A;
            if (block == Blocks.NETHER_QUARTZ_ORE) return 0xFFEEEEEE;
        }
        if (block == Blocks.WATER) return 0xC03050CC;
        if (block == Blocks.LAVA) return 0xFFFF6020;
        if (block == Blocks.GRASS_BLOCK || block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS
                || block == Blocks.FERN || block == Blocks.LARGE_FERN) return 0xFF4D9E3F;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) return 0xFF2F6E2A;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) return 0xFF6B4A2B;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.SAND)) return 0xFFE8DCA0;
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE) return 0xFF808080;
        if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) return 0xFF505050;
        if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT) return 0xFF6B4A2B;
        if (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW) return 0xFFEEEEEE;
        if (block == Blocks.NETHERRACK) return 0xFF6B2020;
        if (block == Blocks.END_STONE) return 0xFFE8E0A0;
        if (block == Blocks.GRAVEL) return 0xFF9C8A7A;
        if (block == Blocks.SANDSTONE) return 0xFFD8C890;
        if (block == Blocks.OBSIDIAN) return 0xFF101018;
        if (state.isOpaqueFullCube()) return 0xFF909090;
        return 0;
    }
}