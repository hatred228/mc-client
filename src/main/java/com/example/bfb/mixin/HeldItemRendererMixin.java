package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.SwingAnimationsModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
                    ordinal = 4,
                    shift = At.Shift.AFTER))
    private void bfb$afterApplyEquipOffset(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                           float swingProgress, ItemStack item, float equipProgress,
                                           MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                           CallbackInfo ci) {
        SwingAnimationsModule mod = ModuleManager.get(SwingAnimationsModule.class);
        if (mod == null || !mod.isEnabled() || mod.animation.is("Default")) return;

        boolean isLeftHand = (hand == Hand.OFF_HAND);

        // Scale применяется ВСЕГДА (и для правой, и для левой руки)
        float s = mod.scale.getFloat();
        matrices.scale(s, s, s);

        // Translate и Rotate — ТОЛЬКО для правой руки
        if (!isLeftHand) {
            matrices.translate(mod.handX.getFloat(), mod.handY.getFloat(), mod.handZ.getFloat());
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mod.rotateX.getFloat()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mod.rotateY.getFloat()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mod.rotateZ.getFloat()));
        }
    }
}