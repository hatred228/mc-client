package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.AimAssistModule;
import com.example.bfb.modules.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void bfb$aimAssistMouse(double frameSeconds, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Freecam всегда должен обновляться
        FreecamModule freecam = FreecamModule.instance;
        if (freecam != null && freecam.isEnabled()) {
            freecam.onFrame((float) frameSeconds);
        }

        if (client.currentScreen != null) return;

        AimAssistModule aim = ModuleManager.get(AimAssistModule.class);
        if (aim == null || !aim.isEnabled() || !aim.hasAimTargetPublic()) {
            return;   // пускаем vanilla — мышь работает нормально
        }

        // AimAssist берёт управление камерой — rotation идёт через CombatController
        // в onTick модуля, здесь только блокируем vanilla updateMouse
        ci.cancel();
    }
}