package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.HitEffectsModule;
import com.example.bfb.modules.HitImpactModule;
import com.example.bfb.modules.HitSoundModule;
import com.example.bfb.modules.SwingAnimationsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class AttackSoundMixin {
    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void bfb$hitSound(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player != MinecraftClient.getInstance().player) return;

        boolean kill = target instanceof LivingEntity le && !le.isAlive();

        HitSoundModule mod = ModuleManager.get(HitSoundModule.class);
        if (mod != null && mod.isEnabled()) {
            mod.onHit(false, kill);
        }

        HitEffectsModule.onAttack(target);

        SwingAnimationsModule.triggerHit();
        if (kill) SwingAnimationsModule.triggerKill();

        HitImpactModule.triggerHit();
        if (kill) HitImpactModule.triggerKill();
    }
}