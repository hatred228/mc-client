package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.NoRotateModule;
import com.example.bfb.modules.VelocityModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.LookAtS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private Vec3d bfb$beforeExplosion;

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void bfb$knockback(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        VelocityModule module = ModuleManager.get(VelocityModule.class);
        var player = MinecraftClient.getInstance().player;
        if (module == null || !module.isEnabled() || player == null
                || packet.getEntityId() != player.getId()) return;
        Vec3d velocity = packet.getVelocity();
        double[] out = module.smooth(velocity.x, velocity.y, velocity.z);
        player.setVelocity(out[0], out[1], out[2]);
        ci.cancel();
    }

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void bfb$before(ExplosionS2CPacket packet, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        bfb$beforeExplosion = player == null ? null : player.getVelocity();
    }

    @Inject(method = "onExplosion", at = @At("RETURN"))
    private void bfb$after(ExplosionS2CPacket packet, CallbackInfo ci) {
        if (bfb$beforeExplosion == null) return;
        VelocityModule module = ModuleManager.get(VelocityModule.class);
        var player = MinecraftClient.getInstance().player;
        if (module == null || !module.isEnabled() || player == null) return;
        Vec3d now = player.getVelocity();
        double[] out = module.smooth(
                now.x - bfb$beforeExplosion.x,
                now.y - bfb$beforeExplosion.y,
                now.z - bfb$beforeExplosion.z);
        player.setVelocity(
                bfb$beforeExplosion.x + out[0],
                bfb$beforeExplosion.y + out[1],
                bfb$beforeExplosion.z + out[2]);
        bfb$beforeExplosion = null;
    }

    @Inject(method = "onLookAt", at = @At("HEAD"), cancellable = true)
    private void bfb$look(LookAtS2CPacket packet, CallbackInfo ci) {
        NoRotateModule module = ModuleManager.get(NoRotateModule.class);
        if (module != null && module.isEnabled()) ci.cancel();
    }

    @Inject(method = "onPlayerRotation", at = @At("HEAD"), cancellable = true)
    private void bfb$rotate(PlayerRotationS2CPacket packet, CallbackInfo ci) {
        NoRotateModule module = ModuleManager.get(NoRotateModule.class);
        if (module != null && module.isEnabled()) ci.cancel();
    }
}