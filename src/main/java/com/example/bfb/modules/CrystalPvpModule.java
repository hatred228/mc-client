package com.example.bfb.modules;

import com.example.bfb.AnticheatBypass;
import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.EntityCache;
import com.example.bfb.FriendManager;
import com.example.bfb.RotationManager;
import com.example.bfb.combat.CombatController;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Random;

/**
 * CrystalPvP — цикл: обсидиан → кристалл → взрыв → повторить.
 * Работает пока цель жива и в радиусе.
 *
 * Silent rotation — сервер видит rotation к блоку, камера свободна.
 */
public class CrystalPvpModule extends CheatModule {

    public final ModeSetting mode = add(new ModeSetting("Mode", "Auto", "Legit", "Fast", "Auto"));
    public final NumberSetting range = add(new NumberSetting("Range", 4.5, 3.0, 6.0, 0.1));
    public final NumberSetting targetRange = add(new NumberSetting("Target range", 4.0, 2.5, 6.0, 0.1));
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 1, 6, 1));
    public final NumberSetting maxCycles = add(new NumberSetting("Max cycles", 20, 1, 60, 1));
    public final BooleanSetting silent = add(new BooleanSetting("Silent rotate", true));
    public final BooleanSetting restoreSlot = add(new BooleanSetting("Restore slot", true));
    public final BooleanSetting autoBreak = add(new BooleanSetting("Auto break", true));
    public final BooleanSetting noSelfDamage = add(new BooleanSetting("Avoid self damage", true));
    public final BooleanSetting checkDamage = add(new BooleanSetting("Damage check", true));

    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));

    private enum Phase { IDLE, PLACE_OBSIDIAN, PLACE_CRYSTAL, BREAK_CRYSTAL, WAIT_CRYSTAL }

    private final Random RNG = new Random();
    private Phase phase = Phase.IDLE;
    private int delayTicker = 0;
    private int savedSlot = -1;
    private BlockPos obsidianPos = null;
    private LivingEntity target = null;
    private int cycles = 0;
    private int waitTicks = 0;

    public CrystalPvpModule() {
        super("CrystalPvP", "Auto obsidian + crystal pvp loop", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        phase = Phase.IDLE;
        delayTicker = 0;
        target = null;
        obsidianPos = null;
        cycles = 0;
        waitTicks = 0;
        RotationManager.release();
        restore();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;

        // восстановление слота
        if (savedSlot >= 0 && phase == Phase.IDLE && restoreSlot.get()) {
            restore();
        }

        // проверка цели
        if (target != null && (!target.isAlive() || player.distanceTo(target) > targetRange.get() + 1.0)) {
            // цель мертва или убежала — сброс
            target = null;
            cycles = 0;
            phase = Phase.IDLE;
            restore();
            return;
        }

        // если цели нет — ищем
        if (target == null) {
            target = pickTarget(player);
            if (target == null) return;
            cycles = 0;
        }

        // лимит циклов
        if (cycles >= maxCycles.getInt()) {
            target = null;
            cycles = 0;
            phase = Phase.IDLE;
            restore();
            return;
        }

        if (delayTicker > 0) { delayTicker--; return; }
        if (waitTicks > 0) { waitTicks--; return; }

        switch (phase) {
            case IDLE -> {
                if (checkDamage.get() && !wouldDealDamage(player, target)) {
                    return;
                }
                BlockPos placeAt = findPlacementPos(player, target);
                if (placeAt == null) return;
                obsidianPos = placeAt;
                phase = Phase.PLACE_OBSIDIAN;
            }
            case PLACE_OBSIDIAN -> {
                if (placeObsidian(player)) {
                    phase = Phase.PLACE_CRYSTAL;
                    delayTicker = 1;
                } else {
                    resetCycle();
                }
            }
            case PLACE_CRYSTAL -> {
                if (placeCrystal(player)) {
                    phase = Phase.WAIT_CRYSTAL;
                    // сервер должен заспавнить кристалл — ждём 1-2 тика
                    waitTicks = 2;
                } else {
                    resetCycle();
                }
            }
            case WAIT_CRYSTAL -> {
                // ищем кристалл над обсидианом
                if (findCrystal(player) != null) {
                    phase = Phase.BREAK_CRYSTAL;
                } else {
                    // кристалл не появился — ставим заново
                    phase = Phase.PLACE_CRYSTAL;
                    waitTicks = 1;
                }
            }
            case BREAK_CRYSTAL -> {
                if (autoBreak.get() && breakCrystal(player)) {
                    cycles++;
                    phase = Phase.PLACE_CRYSTAL;
                    // задержка по профилю античита между циклами
                    delayTicker = CombatController.attackDelay();
                } else {
                    // кристалл уже взорвался или нет рядом — ставим новый
                    phase = Phase.PLACE_CRYSTAL;
                    waitTicks = 1;
                }
            }
        }
    }

    private void resetCycle() {
        phase = Phase.IDLE;
        obsidianPos = null;
        delayTicker = delay.getInt();
    }

    private boolean placeObsidian(ClientPlayerEntity player) {
        int slot = findSlot(player, Items.OBSIDIAN);
        if (slot < 0) return false;
        return placeBlockAt(player, slot, obsidianPos);
    }

    private boolean placeCrystal(ClientPlayerEntity player) {
        int slot = findSlot(player, Items.END_CRYSTAL);
        if (slot < 0 || obsidianPos == null) return false;

        // проверяем что над обсидианом нет кристалла
        if (findCrystalAt(player, obsidianPos.up()) != null) return true;

        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(obsidianPos).add(0, 0.5, 0),
                Direction.UP, obsidianPos, false);

        if (silent.get()) {
            float[] rot = AnticheatBypass.lookAt(player.getEyePos(), hit.getPos());
            RotationManager.setAndSend(rot[0], rot[1], 4);
            player.setBodyYaw(rot[0]);
            player.setHeadYaw(rot[0]);
        }

        if (savedSlot < 0) savedSlot = player.getInventory().getSelectedSlot();
        if (player.getInventory().getSelectedSlot() != slot) syncSlot(player, slot);

        BfbMod.getClient().interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean placeBlockAt(ClientPlayerEntity player, int slot, BlockPos pos) {
        BlockHitResult support = findSupport(player, pos);
        if (support == null) return false;

        if (silent.get()) {
            float[] rot = AnticheatBypass.lookAt(player.getEyePos(), support.getPos());
            RotationManager.setAndSend(rot[0], rot[1], 4);
            player.setBodyYaw(rot[0]);
            player.setHeadYaw(rot[0]);
        }

        if (savedSlot < 0) savedSlot = player.getInventory().getSelectedSlot();
        if (player.getInventory().getSelectedSlot() != slot) syncSlot(player, slot);

        BfbMod.getClient().interactionManager.interactBlock(player, Hand.MAIN_HAND, support);
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean breakCrystal(ClientPlayerEntity player) {
        if (obsidianPos == null) return false;
        EndCrystalEntity crystal = findCrystalAt(player, obsidianPos.up());
        if (crystal == null) return false;
        if (noSelfDamage.get() && player.distanceTo(crystal) < 3.0) {
            // не бьём — мы близко, иначе сами умрём
            return false;
        }

        if (silent.get()) {
            float[] rot = AnticheatBypass.lookAt(player.getEyePos(), crystal.getBoundingBox().getCenter());
            RotationManager.setAndSend(rot[0], rot[1], 4);
            player.setBodyYaw(rot[0]);
            player.setHeadYaw(rot[0]);
        }

        // проверка профиля античита — можно ли атаковать
        if (!CombatController.canAttack(player, crystal)) {
            // если canAttack палит — бьём всё равно, но без GCD
        }

        BfbMod.getClient().interactionManager.attackEntity(player, crystal);
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private EndCrystalEntity findCrystalAt(ClientPlayerEntity player, BlockPos pos) {
        var world = BfbMod.getClient().world;
        if (world == null) return null;
        Box box = new Box(pos).expand(1.5);
        for (EndCrystalEntity crystal : world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true)) {
            if (crystal.getBlockPos().equals(pos)) return crystal;
        }
        // fallback — любой рядом
        EndCrystalEntity best = null;
        double bestD = 2.0 * 2.0;
        for (EndCrystalEntity crystal : world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true)) {
            double d = crystal.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (d < bestD) { bestD = d; best = crystal; }
        }
        return best;
    }

    private EndCrystalEntity findCrystal(ClientPlayerEntity player) {
        if (obsidianPos == null) return null;
        return findCrystalAt(player, obsidianPos.up());
    }

    private LivingEntity pickTarget(ClientPlayerEntity player) {
        double reach = targetRange.get();
        double reachSq = reach * reach;
        return EntityCache.living().stream()
                .filter(e -> e != player && e.isAlive())
                .filter(e -> !FriendManager.isFriend(e))
                .filter(e -> (e instanceof PlayerEntity) ? players.get() : mobs.get())
                .filter(e -> e.squaredDistanceTo(player) <= reachSq)
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);
    }

    private BlockPos findPlacementPos(ClientPlayerEntity player, LivingEntity target) {
        var world = BfbMod.getClient().world;
        if (world == null) return null;

        BlockPos feet = target.getBlockPos();
        BlockPos[] candidates = {
                feet.down(),
                feet.down().north(),
                feet.down().south(),
                feet.down().east(),
                feet.down().west(),
                feet.down().up(),
        };

        for (BlockPos p : candidates) {
            if (!world.getBlockState(p).isReplaceable()) continue;
            if (world.getBlockState(p).isSolidBlock(world, p)) continue;
            if (p.equals(player.getBlockPos().down())) continue;
            if (findSupport(player, p) != null) return p;
        }
        return null;
    }

    private BlockHitResult findSupport(ClientPlayerEntity player, BlockPos air) {
        var world = BfbMod.getClient().world;
        if (world == null) return null;
        for (Direction dir : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                Direction.WEST, Direction.EAST, Direction.UP}) {
            BlockPos neighbor = air.offset(dir);
            var state = world.getBlockState(neighbor);
            if (!state.isSolidBlock(world, neighbor)) continue;
            Direction face = dir.getOpposite();
            Vec3d hit = Vec3d.ofCenter(neighbor).add(
                    face.getOffsetX() * 0.5,
                    face.getOffsetY() * 0.5,
                    face.getOffsetZ() * 0.5);
            if (player.getEyePos().squaredDistanceTo(hit) > 36.0) continue;
            return new BlockHitResult(hit, face, neighbor, false);
        }
        return null;
    }

    private int findSlot(ClientPlayerEntity player, net.minecraft.item.Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private void syncSlot(ClientPlayerEntity player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        if (player.networkHandler != null) {
            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void restore() {
        ClientPlayerEntity player = BfbMod.getClient().player;
        if (player != null && savedSlot >= 0) {
            syncSlot(player, savedSlot);
        }
        savedSlot = -1;
    }

    private boolean wouldDealDamage(ClientPlayerEntity player, LivingEntity target) {
        double d = player.distanceTo(target);
        return d <= 4.5 && d >= 1.5;
    }
}