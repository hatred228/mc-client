package com.example.bfb.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("tints")
    int[] bfb$getTints();

    @Accessor("tints")
    void bfb$setTints(int[] tints);
}