package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.HitEffectsModule;
import com.example.bfb.modules.HitSoundModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class AttackSoundMixin {
    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void bfb$hitSound(PlayerEntity player, Entity target, CallbackInfo ci) {
        HitSoundModule mod = ModuleManager.get(HitSoundModule.class);
        if (mod != null && mod.isEnabled() && player == MinecraftClient.getInstance().player) {
            mod.onHit();
        }
        HitEffectsModule.onAttack(target);
    }
}
