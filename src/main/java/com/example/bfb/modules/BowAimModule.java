package com.example.bfb.modules;

import com.example.bfb.AimController;
import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.FriendManager;
import com.example.bfb.ProjectileAim;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class BowAimModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 40, 8, 80, 1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 60, 5, 180, 1));
    public final NumberSetting speed = add(new NumberSetting("Smooth", 0.4, 0.1, 1.0, 0.05));
    public final NumberSetting tremor = add(new NumberSetting("Tremor", 0.05, 0.0, 0.5, 0.01));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    private int tick;

    public BowAimModule() {
        super("BowAim", "Auto-aim bow with prediction", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null || client.currentScreen != null) return;
        if (!(player.getMainHandStack().getItem() instanceof BowItem)
                && !(player.getOffHandStack().getItem() instanceof BowItem)) {
            AimController.clearTarget();
            return;
        }
        if (!player.isUsingItem()) { AimController.clearTarget(); return; }

        float pull = BowItem.getPullProgress(player.getItemUseTime());
        if (pull < 0.1f) { AimController.clearTarget(); return; }

        Entity target = pick(player);
        if (target == null) { AimController.clearTarget(); return; }

        float arrowSpeed = pull * 3.0f;
        float[] rot = ProjectileAim.solve(player, target, arrowSpeed, 0.05f);

        // обновляем цель только раз в 4 тика — чтобы не дёргалось
        tick++;
        if (tick % 4 == 0) {
            AimController.setTarget(rot[0], rot[1], speed.getFloat(), tremor.getFloat(), 80);
        }
        AimController.syncServer(player);
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
}