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
                player.getPitch() + shot.pitch,
                shot.speed,
                shot.gravity);

        int baseRgb = path.entity
                ? (colorHit.getRgb() & 0xFFFFFF)
                : (color.getRgb() & 0xFFFFFF);

        int total = path.points.size();
        Vec3d previous = null;
        int i = 0;
        for (Vec3d point : path.points) {
            if (previous != null) {
                // градиент: в начале почти прозрачный, в конце яркий
                float t = i / (float) Math.max(1, total - 1);
                int a = (int) (60 + 195 * t);
                int argb = (a << 24) | baseRgb;
                RenderUtil.tracer(matrices, consumers, previous, point, camera, argb, 2f);
            }
            previous = point;
            i++;
        }

        if (previous != null) {
            // мигающая точка попадания
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 150.0);
            int pAlpha = (int) (140 + 115 * pulse);
            int argb = (pAlpha << 24) | baseRgb;
            RenderUtil.ring(matrices, consumers, previous, camera, 0.18 + 0.04 * pulse, argb);
            RenderUtil.ring(matrices, consumers, previous, camera, 0.10, argb);
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
            return new Shot(pull * 3f, 0.05f, 0f, false);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return new Shot(3.15f, 0.05f, 0f, true);
        }
        if (stack.isOf(Items.ENDER_PEARL)) return new Shot(1.5f, 0.03f, 0f, false);
        if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
            return new Shot(0.5f, 0.05f, -20f, false);
        }
        return null;
    }

    private static boolean thrown(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.isOf(Items.ENDER_PEARL)
                || stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION);
    }

    private record Shot(float speed, float gravity, float pitch, boolean isCrossbow) {
    }
}