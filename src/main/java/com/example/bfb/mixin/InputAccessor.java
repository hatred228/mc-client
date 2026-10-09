package com.example.bfb.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("playerInput")
    void bfb$setPlayerInput(PlayerInput input);

    @Accessor("playerInput")
    PlayerInput bfb$getPlayerInput();
}