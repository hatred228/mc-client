package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.InventoryMoveModule;
import com.example.bfb.modules.SafeWalkModule;
import com.example.bfb.modules.TargetStrafeModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Shadow @Final private GameOptions settings;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bfb$afterTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // ============ InventoryMove ============
        InventoryMoveModule inv = ModuleManager.get(InventoryMoveModule.class);
        if (inv != null && inv.isEnabled() && client.currentScreen != null) {
            this.playerInput = new PlayerInput(
                    settings.forwardKey.isPressed(),
                    settings.backKey.isPressed(),
                    settings.leftKey.isPressed(),
                    settings.rightKey.isPressed(),
                    settings.jumpKey.isPressed(),
                    inv.sneak.get() && settings.sneakKey.isPressed(),
                    settings.sprintKey.isPressed()
            );
        }

        // ============ TargetStrafe ============
        TargetStrafeModule strafe = ModuleManager.get(TargetStrafeModule.class);
        if (strafe != null && strafe.isEnabled() && TargetStrafeModule.overrideInput != null) {
            PlayerInput current = this.playerInput;
            PlayerInput override = TargetStrafeModule.overrideInput;

            if (current != null) {
                boolean fwd = current.forward() || override.forward();
                boolean back = current.backward() || override.backward();
                boolean left = current.left() || override.left();
                boolean right = current.right() || override.right();

                if (fwd && back) { fwd = false; back = false; }
                if (left && right) { left = false; right = false; }

                this.playerInput = new PlayerInput(
                        fwd, back, left, right,
                        current.jump(),
                        current.sneak(),
                        current.sprint() || override.sprint()
                );
            } else {
                this.playerInput = override;
            }
        }

        // ============ SafeWalk ============
        SafeWalkModule safe = ModuleManager.get(SafeWalkModule.class);
        if (safe == null || !safe.isEnabled()) return;

        ClientPlayerEntity player = client.player;
        if (!player.isOnGround()) return;
        if (player.getAbilities().flying) return;
        if (player.isTouchingWater() || player.isInLava()) return;
        if (player.isSpectator()) return;

        PlayerInput input = this.playerInput;
        if (input == null) return;

        float fwd = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        float str = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        if (fwd == 0 && str == 0) return;

        double yaw = Math.toRadians(player.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double dx = fwd * -sin + str * cos;
        double dz = fwd * cos + str * sin;
        double len = Math.hypot(dx, dz);
        if (len < 1.0e-4) return;
        dx /= len;
        dz /= len;

        double checkX = player.getX() + dx * 0.55;
        double checkZ = player.getZ() + dz * 0.55;

        BlockPos feetPos = BlockPos.ofFloored(checkX, player.getY() + 0.05, checkZ);
        BlockPos belowPos = feetPos.down();

        BlockState feetState = client.world.getBlockState(feetPos);
        BlockState belowState = client.world.getBlockState(belowPos);

        boolean lavaAhead = false;
        if (safe.lava.get()) {
            if (feetState.isOf(Blocks.LAVA) || belowState.isOf(Blocks.LAVA)
                    || feetState.isOf(Blocks.MAGMA_BLOCK) || belowState.isOf(Blocks.MAGMA_BLOCK)) {
                lavaAhead = true;
            }
            if (client.world.getFluidState(feetPos).isIn(FluidTags.LAVA)
                    || client.world.getFluidState(belowPos).isIn(FluidTags.LAVA)) {
                lavaAhead = true;
            }
        }
        boolean fireAhead = false;
        if (safe.fire.get()) {
            if (feetState.isOf(Blocks.FIRE) || belowState.isOf(Blocks.FIRE)
                    || feetState.isOf(Blocks.SOUL_FIRE) || belowState.isOf(Blocks.SOUL_FIRE)) {
                fireAhead = true;
            }
        }
        boolean edgeAhead = false;
        if (safe.edges.get()) {
            if (!belowState.isSolid() && !feetState.isSolid()
                    && client.world.getFluidState(belowPos).isEmpty()
                    && client.world.getFluidState(feetPos).isEmpty()) {
                edgeAhead = true;
            }
        }
        boolean voidAhead = false;
        if (safe.voidWorld.get()) {
            if (!feetState.isSolid() && !belowState.isSolid()
                    && client.world.getFluidState(feetPos).isEmpty()
                    && client.world.getFluidState(belowPos).isEmpty()) {
                voidAhead = true;
            }
        }

        if (lavaAhead || fireAhead || edgeAhead || voidAhead) {
            this.playerInput = new PlayerInput(
                    false, false, false, false,
                    input.jump(), input.sneak(), input.sprint()
            );
        }
    }
}