package com.example.bfb.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    // nudge удалён. Вся логика TargetStrafe — через KeyboardInputMixin.
}