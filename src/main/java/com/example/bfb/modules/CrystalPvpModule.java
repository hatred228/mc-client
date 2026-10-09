package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class CrystalPvpModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 5, 3, 8, 0.5));
    public final NumberSetting minDamage = add(new NumberSetting("Min damage", 2, 1, 20, 0.5));
    public final NumberSetting maxSelf = add(new NumberSetting("Max self", 12, 0, 20, 0.5));
    public final NumberSetting delay = add(new NumberSetting("Delay", 4, 3, 10, 1));
    public final BooleanSetting place = add(new BooleanSetting("Place crystal", true));
    public final BooleanSetting autoObsidian = add(new BooleanSetting("Auto obsidian", true));
    public final BooleanSetting explode = add(new BooleanSetting("Break", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));

    private static final Random RNG = new Random();
    private int wait;

    // phase-машина: 0=idle, 1=obsidian slot, 2=obsidian interact,
    //              3=obsidian restore, 4=crystal slot, 5=crystal interact, 6=crystal restore
    private int phase = 0;
    private int slotA = -1;       // рабочий слот
    private int slotPrev = -1;    // слот до операции
    private BlockHitResult hitA = null;
    private BlockPos crystalOn = null;
    private int ttl = 0;

    public CrystalPvpModule() {
        super("CrystalPvP", "Crystal PvP with obsidian place", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        resetPhase();
        wait = 0;
    }

    private void resetPhase() {
        phase = 0;
        slotA = -1;
        slotPrev = -1;
        hitA = null;
        crystalOn = null;
        ttl = 0;
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null || client.interactionManager == null || client.currentScreen != null) {
            resetPhase();
            return;
        }

        // ведём phase до конца
        if (phase != 0) {
            tickPhase(client, player, world);
            return;
        }

        if (wait > 0) { wait--; return; }

        LivingEntity target = nearest(player);
        if (target == null) return;

        // === Break ===
        if (explode.get()) {
            EndCrystalEntity best = null;
            float bestDamage = 0;
            Box area = player.getBoundingBox().expand(range.get());
            for (EndCrystalEntity crystal : world.getEntitiesByClass(EndCrystalEntity.class, area, e -> true)) {
                Vec3d pos = crystal.getEntityPos();
                float self = blast(player, pos);
                float dealt = blast(target, pos);
                if (self > maxSelf.get() || dealt < minDamage.get() || dealt <= bestDamage) continue;
                best = crystal;
                bestDamage = dealt;
            }
            if (best != null) {
                client.interactionManager.attackEntity(player, best);
                player.swingHand(Hand.MAIN_HAND);
                wait = delay.getInt() + RNG.nextInt(2);
                return;
            }
        }

        // === Place crystal on existing obsidian ===
        if (place.get()) {
            int cSlot = crystalSlot(player);
            if (cSlot < 0) return;
            BlockPos base = bestBase(player, target);
            if (base != null) {
                // обсидиан уже есть → ставим кристалл (phase 4-6)
                Vec3d aim = Vec3d.ofCenter(base).add(0, 0.5, 0);
                slotA = cSlot;
                slotPrev = player.getInventory().getSelectedSlot();
                hitA = new BlockHitResult(aim, Direction.UP, base, false);
                ttl = 4;
                if (slotPrev != slotA) {
                    CombatUtil.syncSlot(player, slotA);
                    phase = 4;
                } else {
                    phase = 5;
                }
                return;
            }

            // нет обсидиана → если autoObsidian включён, ставим его
            if (autoObsidian.get()) {
                int oSlot = obsidianSlot(player);
                if (oSlot < 0) return;
                BlockHitResult floorHit = findObsidianFloor(player, target);
                if (floorHit == null) return;
                slotA = oSlot;
                slotPrev = player.getInventory().getSelectedSlot();
                hitA = floorHit;
                ttl = 4;
                if (slotPrev != slotA) {
                    CombatUtil.syncSlot(player, slotA);
                    phase = 1;
                } else {
                    phase = 2;
                }
            }
        }
    }

    private void tickPhase(net.minecraft.client.MinecraftClient client,
                           net.minecraft.client.network.ClientPlayerEntity player,
                           net.minecraft.world.World world) {
        if (--ttl <= 0) {
            // время вышло — вернуть слот если меняли
            if (phase >= 1 && phase <= 3 && slotPrev >= 0 && slotPrev != slotA) {
                CombatUtil.syncSlot(player, slotPrev);
            } else if (phase >= 4 && phase <= 6 && slotPrev >= 0 && slotPrev != slotA) {
                CombatUtil.syncSlot(player, slotPrev);
            }
            resetPhase();
            return;
        }

        if (hitA == null) { resetPhase(); return; }

        // Obsidian phases
        if (phase == 1) {
            phase = 2;
            return;
        }
        if (phase == 2) {
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitA);
            player.swingHand(Hand.MAIN_HAND);
            // после установки обсидиана ждём 1 тик, потом возвращаем слот
            phase = 3;
            return;
        }
        if (phase == 3) {
            if (slotPrev >= 0 && slotPrev != slotA) CombatUtil.syncSlot(player, slotPrev);
            // ждём ещё 1 тик, потом пробуем поставить кристалл
            crystalOn = hitA.getBlockPos().up();  // обсидиан встал над floor
            resetPhaseKeepTarget();
            wait = 1;
            return;
        }

        // Crystal phases
        if (phase == 4) {
            phase = 5;
            return;
        }
        if (phase == 5) {
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitA);
            player.swingHand(Hand.MAIN_HAND);
            if (slotPrev >= 0 && slotPrev != slotA) {
                phase = 6;
            } else {
                wait = delay.getInt() + RNG.nextInt(2);
                resetPhase();
            }
            return;
        }
        if (phase == 6) {
            if (slotPrev >= 0 && slotPrev != slotA) CombatUtil.syncSlot(player, slotPrev);
            wait = delay.getInt() + RNG.nextInt(2);
            resetPhase();
        }
    }

    private void resetPhaseKeepTarget() {
        phase = 0;
        slotA = -1;
        slotPrev = -1;
        hitA = null;
        ttl = 0;
    }

    private LivingEntity nearest(net.minecraft.client.network.ClientPlayerEntity player) {
        LivingEntity best = null;
        double bestDist = range.get() * range.get();
        Box area = player.getBoundingBox().expand(range.get());
        for (LivingEntity entity : player.getEntityWorld().getEntitiesByClass(LivingEntity.class, area, candidate ->
                CombatUtil.accept(candidate, player, players.get(), mobs.get(), false))) {
            double dist = entity.squaredDistanceTo(player);
            if (dist < bestDist) { bestDist = dist; best = entity; }
        }
        return best;
    }

    /** Ищем уже существующий обсидиан/бедрок с местом для кристалла. */
    private BlockPos bestBase(net.minecraft.client.network.ClientPlayerEntity player, LivingEntity target) {
        BlockPos center = target.getBlockPos();
        BlockPos chosen = null;
        float best = 0;
        int radius = 3;
        var world = player.getEntityWorld();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (!canPlace(world, pos)) continue;
                    if (player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 20.25) continue;
                    Vec3d crystal = Vec3d.ofCenter(pos.up());
                    float self = blast(player, crystal);
                    float dealt = blast(target, crystal);
                    if (self > maxSelf.get() || dealt < minDamage.get() || dealt <= best) continue;
                    best = dealt;
                    chosen = pos;
                }
            }
        }
        return chosen;
    }

    /** Ищем floor, на который можно поставить обсидиан, рядом с целью. */
    private BlockHitResult findObsidianFloor(net.minecraft.client.network.ClientPlayerEntity player, LivingEntity target) {
        var world = player.getEntityWorld();
        BlockPos feet = target.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos spot = feet.offset(direction);
            BlockPos floor = spot.down();
            if (!world.getBlockState(spot).isAir() || !world.getBlockState(spot.up()).isAir()) continue;
            if (!world.getBlockState(floor).isSolid()) continue;
            if (player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(floor)) > 20.25) continue;
            // и сам spot должен быть в пределах 4.5 блоков от игрока (для reach interact)
            double d = player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(spot));
            if (d < bestDist) { bestDist = d; best = floor; }
        }
        if (best == null) return null;
        Vec3d aim = Vec3d.ofCenter(best).add(0, 0.5, 0);
        return new BlockHitResult(aim, Direction.UP, best, false);
    }

    private static boolean canPlace(net.minecraft.world.World world, BlockPos pos) {
        var block = world.getBlockState(pos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) return false;
        if (!world.getBlockState(pos.up()).isAir() || !world.getBlockState(pos.up(2)).isAir()) return false;
        return world.getOtherEntities(null, new Box(pos.up())).isEmpty();
    }

    private static int crystalSlot(net.minecraft.client.network.ClientPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }

    private static int obsidianSlot(net.minecraft.client.network.ClientPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.OBSIDIAN)) return i;
        }
        return -1;
    }

    private static float blast(LivingEntity entity, Vec3d crystal) {
        double dist = Math.sqrt(entity.squaredDistanceTo(crystal)) / 12.0;
        if (dist > 1) return 0;
        double impact = 1.0 - dist;
        return (float) ((impact * impact + impact) * 21.0 + 1.0);
    }
}