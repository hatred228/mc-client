package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.EntityCache;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;

public class AttackAuraModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 3.5, 0.1));
    public final NumberSetting angle = add(new NumberSetting("Angle", 45.0, 10.0, 90.0, 5.0));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    public AttackAuraModule() {
        super("AttackAura", "Attacks around you while holding attack", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null) return;
        if (client.options.attackKey.isPressed()) return;
        Entity best = null;
        double bestD = range.get() * range.get();
        for (Entity e : EntityCache.living()) {
            if (!CombatUtil.accept(e, player, players.get(), mobs.get(), animals.get())) continue;
            double d = e.squaredDistanceTo(player);
            if (d < bestD) { bestD = d; best = e; }
        }
        if (best == null) return;
        // angle читает silent yaw
        if (CombatUtil.fullAngle(player, best) > angle.getFloat()) return;
        if (player.getAttackCooldownProgress(0f) >= 0.95 && player.distanceTo(best) <= 3.05) {
            CombatUtil.attack(best);
        }
    }
}