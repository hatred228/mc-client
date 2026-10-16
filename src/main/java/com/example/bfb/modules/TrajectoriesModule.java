package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.ProjectileSim;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class TrajectoriesModule extends CheatModule {
    public final ColorSetting color = add(new ColorSetting("Color", 0x7CFFC4));
    public final ColorSetting colorHit = add(new ColorSetting("Hit color", 0xFFFF5A6A));
    public final BooleanSetting multishot = add(new BooleanSetting("Crossbow multishot (3)", true));
    public final NumberSetting spreadAngle = add(new NumberSetting("Multishot angle", 10, 5, 20, 1));

    // === радиусы действия ===
    public final BooleanSetting showRadius = add(new BooleanSetting("Show explosion radius", true));
    public final BooleanSetting showPotRadius = add(new BooleanSetting("Show potion radius", true));
    public final NumberSetting radiusSegments = add(new NumberSetting("Radius segments", 40, 12, 80, 4));
    public final ColorSetting radiusColor = add(new ColorSetting("Radius color", 0xFF66CC));
    public final ColorSetting explosionColor = add(new ColorSetting("Explosion color", 0xFF4444));
    public final BooleanSetting radiusFill = add(new BooleanSetting("Radius fill", true));

    public TrajectoriesModule() {
        super("Trajectories", "Flight line for projectiles", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        Shot shot = shot(player);
        if (shot == null) return;

        boolean isMultishot = shot.isCrossbow && multishot.get() && hasMultishot(player);
        if (isMultishot) {
            int angle = spreadAngle.getInt();
            drawArc(matrices, consumers, camera, player, shot, 0f);
            drawArc(matrices, consumers, camera, player, shot, -angle);
            drawArc(matrices, consumers, camera, player, shot, +angle);
        } else {
            drawArc(matrices, consumers, camera, player, shot, 0f);
        }
    }

    private void drawArc(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera,
                          ClientPlayerEntity player, Shot shot, float yawOffset) {
        ProjectileSim.Path path = ProjectileSim.trace(player,
                player.getYaw() + yawOffset,
                player.getPitch() + shot.pitch(),
                shot.speed(),
                shot.gravity());

        int baseRgb = path.entity
                ? (colorHit.getRgb() & 0xFFFFFF)
                : (color.getRgb() & 0xFFFFFF);

        int total = path.points.size();
        Vec3d previous = null;
        int i = 0;
        for (Vec3d point : path.points) {
            if (previous != null) {
                float t = i / (float) Math.max(1, total - 1);
                int a = (int) (60 + 195 * t);
                int argb = (a << 24) | baseRgb;
                RenderUtil.tracer(matrices, consumers, previous, point, camera, argb, 2f);
            }
            previous = point;
            i++;
        }

        if (previous == null) return;

        // мигающая точка попадания
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 150.0);
        int pAlpha = (int) (140 + 115 * pulse);
        int argb = (pAlpha << 24) | baseRgb;
        RenderUtil.ring(matrices, consumers, previous, camera, 0.18 + 0.04 * pulse, argb);
        RenderUtil.ring(matrices, consumers, previous, camera, 0.10, argb);

        // радиус действия
        if (shot.isPotion() && showPotRadius.get()) {
            int radius = shot.isLingering() ? 3 : 4;
            drawRadiusCircle(matrices, consumers, camera, previous, radius,
                    (0xAA << 24) | (radiusColor.getRgb() & 0xFFFFFF));
        } else if (shot.isExplosive() && showRadius.get()) {
            drawRadiusCircle(matrices, consumers, camera, previous, 3,
                    (0xAA << 24) | (explosionColor.getRgb() & 0xFFFFFF));
        }
    }

    /**
     * Плоский круг на земле + опционально лёгкая заливка (без "3D вазы").
     */
    private void drawRadiusCircle(MatrixStack matrices, VertexConsumerProvider consumers,
                                  Vec3d camera, Vec3d hit,
                                  double radius, int color) {
        int seg = radiusSegments.getInt();

        // основной круг на земле
        for (int i = 0; i < seg; i++) {
            double a1 = Math.PI * 2 * i / seg;
            double a2 = Math.PI * 2 * (i + 1) / seg;
            Vec3d p1 = hit.add(Math.cos(a1) * radius, 0.02, Math.sin(a1) * radius);
            Vec3d p2 = hit.add(Math.cos(a2) * radius, 0.02, Math.sin(a2) * radius);
            RenderUtil.tracer(matrices, consumers, p1, p2, camera, color, 1.8f);
        }

        // заливка — серия тонких колец с убывающей alpha
        if (radiusFill.get()) {
            int rings = (int) (radius * 2);
            for (int r = 0; r < rings; r++) {
                double rr = r * 0.5;
                float k = (float) (rr / radius);
                int a = (int) ((1f - k) * 55);
                if (a <= 3) continue;
                int c = (a << 24) | (color & 0xFFFFFF);

                double innerR = rr;
                double outerR = Math.min(rr + 0.5, radius);
                for (int i = 0; i < seg; i++) {
                    double ang1 = Math.PI * 2 * i / seg;
                    double ang2 = Math.PI * 2 * (i + 1) / seg;
                    Vec3d p1 = hit.add(Math.cos(ang1) * innerR, 0.015, Math.sin(ang1) * innerR);
                    Vec3d p2 = hit.add(Math.cos(ang2) * innerR, 0.015, Math.sin(ang2) * innerR);
                    Vec3d p1b = hit.add(Math.cos(ang1) * outerR, 0.015, Math.sin(ang1) * outerR);
                    RenderUtil.tracer(matrices, consumers, p1, p2, camera, c, 1f);
                    RenderUtil.tracer(matrices, consumers, p1, p1b, camera, c, 1f);
                }
            }
        }
    }

    private boolean hasMultishot(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof CrossbowItem)) stack = player.getOffHandStack();
        if (!(stack.getItem() instanceof CrossbowItem)) return false;
        try {
            var registry = player.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            int level = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.MULTISHOT), stack);
            return level > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Shot shot(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!thrown(stack)) stack = player.getOffHandStack();
        if (stack.getItem() instanceof BowItem) {
            float pull = 1f;
            if (player.isUsingItem() && player.getActiveItem().getItem() instanceof BowItem) {
                pull = Math.max(0.12f, BowItem.getPullProgress(player.getItemUseTime()));
            }
            return new Shot(pull * 3f, 0.05f, 0f, false, false, false, false);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return new Shot(3.15f, 0.05f, 0f, true, false, false, false);
        }
        if (stack.isOf(Items.ENDER_PEARL)) return new Shot(1.5f, 0.03f, 0f, false, false, false, false);
        if (stack.isOf(Items.SPLASH_POTION)) return new Shot(0.5f, 0.05f, -20f, false, true, false, false);
        if (stack.isOf(Items.LINGERING_POTION)) return new Shot(0.5f, 0.05f, -20f, false, true, true, false);
        if (stack.isOf(Items.END_CRYSTAL)) return new Shot(1f, 0.05f, 0f, false, false, false, true);
        if (stack.isOf(Items.TNT)) return new Shot(1f, 0.05f, 0f, false, false, false, true);
        return null;
    }

    private static boolean thrown(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.isOf(Items.ENDER_PEARL)
                || stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION)
                || stack.isOf(Items.END_CRYSTAL)
                || stack.isOf(Items.TNT);
    }

    private record Shot(float speed, float gravity, float pitch, boolean isCrossbow,
                        boolean isPotion, boolean isLingering, boolean isExplosive) {}
}