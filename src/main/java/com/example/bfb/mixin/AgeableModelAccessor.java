package com.example.bfb.mixin;

import net.minecraft.client.render.entity.AgeableMobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AgeableMobEntityRenderer.class)
public interface AgeableModelAccessor {
    @Accessor("babyModel")
    EntityModel<?> bfb$babyModel();
}
