package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoWebModule;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NoWeb — перехват tickMovement, восстановление скорости после cobweb slowdown.
 */
@Mixin(LivingEntity.class)
public class EntityNoWebMixin {

    private Vec3d bfb$beforeWeb = null;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bfb$captureBefore(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self != MinecraftClient.getInstance().player) return;

        NoWebModule mod = ModuleManager.get(NoWebModule.class);
        if (mod == null || !mod.isEnabled()) return;

        if (bfb$inWeb(self)) {
            bfb$beforeWeb = self.getVelocity();
        } else {
            bfb$beforeWeb = null;
        }
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void bfb$restoreAfter(CallbackInfo ci) {
        if (bfb$beforeWeb == null) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self != MinecraftClient.getInstance().player) { bfb$beforeWeb = null; return; }

        NoWebModule mod = ModuleManager.get(NoWebModule.class);
        if (mod == null || !mod.isEnabled()) { bfb$beforeWeb = null; return; }

        if (!bfb$inWeb(self)) { bfb$beforeWeb = null; return; }

        double mult = mod.boostSpeed.get() ? 1.0 : 0.5;
        Vec3d restored = new Vec3d(
                bfb$beforeWeb.x * mult,
                self.getVelocity().y,
                bfb$beforeWeb.z * mult
        );
        self.setVelocity(restored);
        bfb$beforeWeb = null;
    }

    /** Проверка — есть ли паутина в хитбоксе игрока. */
    private static boolean bfb$inWeb(LivingEntity entity) {
        Box box = entity.getBoundingBox().expand(0.1);
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        var world = entity.getEntityWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockState(new net.minecraft.util.math.BlockPos(x, y, z)).isOf(Blocks.COBWEB)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}