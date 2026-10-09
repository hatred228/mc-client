package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.FriendManager;
import com.example.bfb.ScriptManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class TriggerBotModule extends CheatModule {
    public final NumberSetting delay = add(new NumberSetting("Delay", 3, 2, 10, 1));
    public final NumberSetting minTicks = add(new NumberSetting("Min ticks", 5, 3, 15, 1));
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 3.5, 0.1));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting visibleOnly = add(new BooleanSetting("Visible only", false));
    public final BooleanSetting pingCheck = add(new BooleanSetting("Ping check", false));
    public final BooleanSetting debug = add(new BooleanSetting("Debug", false));
    public final NumberSetting maxPing = add(new NumberSetting("Max ping", 200, 50, 400, 10));
    public final NumberSetting cooldownReady = add(new NumberSetting("Cooldown", 0.95, 0.85, 1.0, 0.01));

    private int wait;
    private int ticksSinceLastAttack;
    private Entity lastHit;
    private int lastHitTicks;
    private int lastHitId = -1;
    private static final Random RNG = new Random();

    public TriggerBotModule() {
        super("TriggerBot", "Legit hits", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        wait = 0;
        ticksSinceLastAttack = 999;
        lastHit = null;
        lastHitTicks = 0;
        lastHitId = -1;
    }

    @Override
    public void onTick() {
        ticksSinceLastAttack++;
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.currentScreen != null) return;

        // отслеживание смерти последней цели — для ScriptManager.kill
        if (lastHitId >= 0) {
            lastHitTicks++;
            Entity current = client.world != null ? client.world.getEntityById(lastHitId) : null;
            boolean dead = current == null || !current.isAlive();
            if (debug.get() && lastHitTicks % 20 == 0) {
                player.sendMessage(net.minecraft.text.Text.literal(
                        "§7[tb] id=" + lastHitId + " ticks=" + lastHitTicks + " dead=" + dead), true);
            }
            if (dead) {
                String victimName = lastHit != null ? lastHit.getName().getString() : ("id#" + lastHitId);
                ScriptManager.kill(victimName);
                lastHit = null;
                lastHitId = -1;
                lastHitTicks = 0;
            } else if (lastHitTicks > 200) {
                lastHit = null;
                lastHitId = -1;
                lastHitTicks = 0;
            }
        }

        // ping gate
        if (pingCheck.get() && client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null && entry.getLatency() > maxPing.getInt()) return;
        }

        if (wait > 0) { wait--; return; }
        if (ticksSinceLastAttack < minTicks.getInt()) return;

        // цель под прицелом (расширенный hitbox учитывается автоматически)
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;
        var target = hit.getEntity();
        if (target == null || !target.isAlive()) return;
        if (!CombatUtil.accept(target, player, players.get(), mobs.get(), animals.get())) return;
        if (FriendManager.isFriend(target)) return;

        // range UI max 3.5 — совпадает с серверным hard cap CombatUtil.attack
        if (target.squaredDistanceTo(player) > range.get() * range.get()) return;

        if (visibleOnly.get() && !CombatUtil.canSee(player, target)) return;
        if (player.getAttackCooldownProgress(0f) < cooldownReady.getFloat()) return;

        // без reach spoof — безопасно на Matrix/Vulcan
        CombatUtil.attack(target, false);

        lastHit = target;
        lastHitId = target.getId();
        lastHitTicks = 0;
        ticksSinceLastAttack = 0;
        wait = delay.getInt() + RNG.nextInt(2);

        if (debug.get()) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "§a[tb] hit: " + target.getName().getString() + " id=" + target.getId()), true);
        }
    }

    @Override
    public void onDisable() {
        wait = 0;
        ticksSinceLastAttack = 999;
        lastHit = null;
        lastHitId = -1;
        lastHitTicks = 0;
    }
}