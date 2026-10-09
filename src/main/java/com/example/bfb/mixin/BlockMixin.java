package com.example.bfb.mixin;

import com.example.bfb.modules.XRayModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void bfb$xray(BlockState state, BlockState other, Direction side, CallbackInfoReturnable<Boolean> cir) {
        XRayModule xray = XRayModule.instance;
        if (xray == null || !xray.isEnabled()) return;
        cir.setReturnValue(xray.wants(state));
    }
}
