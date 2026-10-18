package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.FriendManager;
import com.example.bfb.ProjectileAim;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * ArbaletAim — плавное наведение арбалета.
 */
public class ArbaletAimModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 40, 8, 80, 1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 60, 5, 180, 1));
    public final NumberSetting aimSpeed = add(new NumberSetting("Aim speed", 0.35, 0.1, 0.9, 0.05));
    public final NumberSetting tremor = add(new NumberSetting("Tremor", 0.05, 0.0, 0.3, 0.01));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final ModeSetting rotateMode = add(new ModeSetting("Rotate mode", "Silent", "Silent", "Visual"));

    private final Random RNG = new Random();
    private float smoothYaw = Float.NaN;
    private float smoothPitch = Float.NaN;

    public ArbaletAimModule() {
        super("ArbaletAim", "Auto-aim crossbow with prediction", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        smoothYaw = Float.NaN;
        smoothPitch = Float.NaN;
        RotationManager.release();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null || client.currentScreen != null) {
            RotationManager.release();
            return;
        }
        boolean held = player.getMainHandStack().getItem() instanceof CrossbowItem
                || player.getOffHandStack().getItem() instanceof CrossbowItem;
        if (!held) {
            RotationManager.release();
            return;
        }

        Entity target = pick(player);
        if (target == null) return;

        float[] ideal = ProjectileAim.solve(player, target, 3.15f, 0.05f);

        if (Float.isNaN(smoothYaw)) {
            smoothYaw = player.getYaw();
            smoothPitch = player.getPitch();
        }

        float k = aimSpeed.getFloat() * 0.35f;
        float dy = wrap(ideal[0] - smoothYaw);
        float dp = ideal[1] - smoothPitch;

        float tremorAmp = tremor.getFloat();
        if (tremorAmp > 0f && Math.abs(dy) < 15f && Math.abs(dp) < 15f) {
            long t = System.currentTimeMillis();
            dy += (float) Math.sin(t / 47.0) * tremorAmp;
            dp += (float) Math.cos(t / 53.0) * tremorAmp * 0.6f;
        }

        smoothYaw += dy * k;
        smoothPitch += dp * k;
        smoothPitch = MathHelper.clamp(smoothPitch, -90f, 90f);

        if (rotateMode.is("Silent")) {
            RotationManager.setAndSend(smoothYaw, smoothPitch, 4);
        } else {
            player.setYaw(smoothYaw);
            player.setPitch(smoothPitch);
        }
    }

    private Entity pick(PlayerEntity player) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        double reach = range.get();
        double halfFov = fov.get() * 0.5f;
        for (Entity entity : com.example.bfb.EntityCache.living()) {
            if (!CombatUtil.accept(entity, player, players.get(), mobs.get(), animals.get())) continue;
            if (FriendManager.isFriend(entity)) continue;
            double dist = player.squaredDistanceTo(entity);
            if (dist > reach * reach) continue;
            if (angle(player, entity) > halfFov) continue;
            if (dist < bestDist) { bestDist = dist; best = entity; }
        }
        return best;
    }

    private static float angle(PlayerEntity player, Entity target) {
        Vec3d look = player.getRotationVec(1f);
        Vec3d to = target.getBoundingBox().getCenter().subtract(player.getEyePos());
        double len = to.length();
        if (len < 1.0e-4) return 0;
        return (float) Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(to.multiply(1.0 / len)), -1, 1)));
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}