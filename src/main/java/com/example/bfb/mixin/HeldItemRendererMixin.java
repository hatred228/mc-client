package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.SwingAnimationsModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
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
    private void bfb$afterApplyEquipOffset(AbstractClientPlayerEntity player, float tickProgress, float pitch,
                                            Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                            MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                            CallbackInfo ci) {
        SwingAnimationsModule mod = ModuleManager.get(SwingAnimationsModule.class);
        if (mod == null || !mod.isEnabled()) return;
        if (mod.animation.is("Default")) return;

        boolean isMain = hand == Hand.MAIN_HAND;

        // === SCALE — применяется КО ВСЕМ рукам ===
        float s = mod.scale.getFloat();
        if (s != 1.0f) matrices.scale(s, s, s);

        // === ROTATION + HAND OFFSET — только к MAIN hand ===
        if (isMain) {
            float rx = mod.rotateX.getFloat();
            float ry = mod.rotateY.getFloat();
            float rz = mod.rotateZ.getFloat();

            float k = mod.lerpSpeed.getFloat();
            SwingAnimationsModule.updateSmooth(ry, rx, rz, k);
            rx = SwingAnimationsModule.lastSmoothPitchValue();
            ry = SwingAnimationsModule.lastSmoothYawValue();
            rz = SwingAnimationsModule.lastSmoothRollValue();

            if (rz != 0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
            if (ry != 0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
            if (rx != 0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));

            matrices.translate(mod.handX.getFloat(), mod.handY.getFloat(), mod.handZ.getFloat());
        }

        if (mod.swingAmplitude.getFloat() != 1.0f && swingProgress > 0f) {
            float amp = mod.swingAmplitude.getFloat();
            float sp = (float) Math.sin(swingProgress * Math.PI);
            matrices.translate(0f, -sp * 0.1f * amp, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-sp * 45f * amp));
        }

        if (mod.hitSlowdown.get()) {
            float p = SwingAnimationsModule.hitProgress(mod.hitSlowDuration.getInt());
            if (p > 0f) {
                float f = 1f - (1f - mod.hitSlowFactor.getFloat()) * p;
                matrices.scale(f, f, f);
            }
        }
        if (mod.hitSpin.get()) {
            float p = SwingAnimationsModule.hitProgress(mod.hitSlowDuration.getInt());
            if (p > 0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mod.hitSpinAmount.getFloat() * p));
        }
        if (mod.hitScale.get()) {
            float p = SwingAnimationsModule.hitProgress(mod.hitSlowDuration.getInt());
            if (p > 0f) {
                float s2 = 1f + mod.hitScaleAmount.getFloat() * p;
                matrices.scale(s2, s2, s2);
            }
        }
        if (mod.killEffect.get()) {
            float p = SwingAnimationsModule.killProgress(mod.killDuration.getInt());
            if (p > 0f) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mod.killSpinAmount.getFloat() * p));
                float s3 = 1f + 0.3f * p;
                matrices.scale(s3, s3, s3);
            }
        }
    }
}