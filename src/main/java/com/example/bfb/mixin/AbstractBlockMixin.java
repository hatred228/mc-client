package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.FastBreakModule;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "calcBlockBreakingDelta", at = @At("RETURN"), cancellable = true)
    private void bfb$fastBreak(BlockState state, PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        FastBreakModule fastBreak = ModuleManager.get(FastBreakModule.class);
        if (fastBreak == null || !fastBreak.isEnabled()) return;
        if (player != MinecraftClient.getInstance().player) return;
        cir.setReturnValue(cir.getReturnValue() * fastBreak.multiplier.getFloat());
    }
}
