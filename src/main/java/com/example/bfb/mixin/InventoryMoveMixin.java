package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.InventoryMoveModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * InventoryMove — отдельный mixin потому что KeyboardInput.tick()
 * не вызывается когда экран открыт. Ставим на ClientPlayerEntity.tickMovement.
 */
@Mixin(ClientPlayerEntity.class)
public class InventoryMoveMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bfb$inventoryMove(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (self != client.player) return;

        InventoryMoveModule mod = ModuleManager.get(InventoryMoveModule.class);
        if (mod == null || !mod.isEnabled()) return;
        if (client.currentScreen == null) return;

        GameOptions options = client.options;
        self.input.playerInput = new PlayerInput(
                options.forwardKey.isPressed(),
                options.backKey.isPressed(),
                options.leftKey.isPressed(),
                options.rightKey.isPressed(),
                options.jumpKey.isPressed(),
                mod.sneak.get() && options.sneakKey.isPressed(),
                options.sprintKey.isPressed()
        );
    }
}