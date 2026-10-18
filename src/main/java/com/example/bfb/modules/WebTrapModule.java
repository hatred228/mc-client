package com.example.bfb.modules;

import com.example.bfb.AnticheatBypass;
import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.ModuleManager;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class WebTrapModule extends CheatModule {

    public final ModeSetting targetMode = add(new ModeSetting("Target", "KillAura",
            "KillAura", "AimAssist", "Crosshair"));
    public final NumberSetting range = add(new NumberSetting("Range", 4.5, 3.0, 6.0, 0.1));
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 1, 10, 1));
    public final BooleanSetting restoreSlot = add(new BooleanSetting("Restore slot", true));
    public final BooleanSetting rotate = add(new BooleanSetting("Silent rotate", true));
    public final BooleanSetting onlyOnGround = add(new BooleanSetting("Only on ground", true));
    public final BooleanSetting inAir = add(new BooleanSetting("Trap in air", false));

    private int cooldown = 0;

    public WebTrapModule() {
        super("WebTrap", "Places cobwebs on target", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        RotationManager.release();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;

        if (cooldown > 0) { cooldown--; return; }
        if (onlyOnGround.get() && !player.isOnGround()) return;

        LivingEntity target = pickTarget(player);
        if (target == null) return;
        if (player.distanceTo(target) > range.get()) return;

        int webSlot = findCobwebSlot(player);
        if (webSlot < 0) return;

        BlockPos place;
        if (inAir.get() && !target.isOnGround()) {
            place = target.getBlockPos();
        } else {
            place = findPlacementPos(target);
        }
        if (place == null) return;

        BlockHitResult hit = findSupport(player, place);
        if (hit == null) return;

        int prevSlot = player.getInventory().getSelectedSlot();

        if (rotate.get()) {
            Vec3d center = Vec3d.ofCenter(hit.getBlockPos());
            float[] rot = AnticheatBypass.lookAt(player.getEyePos(), center);
            // === RotationManager держит rotation, не даёт откатить ===
            RotationManager.setAndSend(rot[0], rot[1], 4);
            player.setBodyYaw(rot[0]);
            player.setHeadYaw(rot[0]);
        }

        if (player.getInventory().getSelectedSlot() != webSlot) {
            player.getInventory().setSelectedSlot(webSlot);
            if (player.networkHandler != null) {
                player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(webSlot));
            }
        }

        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);

        if (restoreSlot.get() && prevSlot != webSlot) {
            player.getInventory().setSelectedSlot(prevSlot);
            if (player.networkHandler != null) {
                player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
            }
        }

        cooldown = delay.getInt();
    }

    private LivingEntity pickTarget(ClientPlayerEntity player) {
        switch (targetMode.get()) {
            case "KillAura" -> {
                KillAuraModule ka = ModuleManager.get(KillAuraModule.class);
                if (ka != null && ka.isEnabled()
                        && KillAuraModule.target instanceof LivingEntity le && le.isAlive())
                    return le;
            }
            case "AimAssist" -> {
                AimAssistModule aa = ModuleManager.get(AimAssistModule.class);
                if (aa != null && aa.isEnabled()
                        && AimAssistModule.target instanceof LivingEntity le && le.isAlive())
                    return le;
            }
            case "Crosshair" -> {
                if (BfbMod.getClient().crosshairTarget instanceof EntityHitResult hit
                        && hit.getEntity() instanceof LivingEntity le && le.isAlive())
                    return le;
            }
        }
        return null;
    }

    private int findCobwebSlot(ClientPlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && s.isOf(Items.COBWEB)) return i;
        }
        return -1;
    }

    private BlockPos findPlacementPos(LivingEntity target) {
        var world = BfbMod.getClient().world;
        if (world == null) return null;
        BlockPos feet = target.getBlockPos();
        if (world.getBlockState(feet).isReplaceable()) return feet;
        BlockPos above = feet.up();
        if (world.getBlockState(above).isReplaceable()) return above;
        return null;
    }

    private BlockHitResult findSupport(ClientPlayerEntity player, BlockPos air) {
        var world = BfbMod.getClient().world;
        if (world == null) return null;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = air.offset(dir);
            var state = world.getBlockState(neighbor);
            if (state.isAir() || state.isReplaceable()) continue;
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
}