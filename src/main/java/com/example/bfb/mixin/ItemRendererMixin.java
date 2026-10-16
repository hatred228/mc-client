package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.HandsVisualModule;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Прокраска предмета в руке через tints[] (per-vertex tint).
 * Работает через accessor на ItemRenderState.layers[].tints.
 * Накладываем тинт на каждый layer ПЕРЕД рендером.
 */
@Mixin(ItemRenderState.class)
public class ItemRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void bfb$handsVisualTint(CallbackInfo ci) {
        HandsVisualModule mod = ModuleManager.get(HandsVisualModule.class);
        if (mod == null || !mod.isEnabled()) return;

        int tint = mod.tintColor();

        ItemRenderState self = (ItemRenderState) (Object) this;
        ItemRenderStateAccessor selfAccess = (ItemRenderStateAccessor) self;
        int layerCount = selfAccess.bfb$getLayerCount();
        ItemRenderState.LayerRenderState[] layers = selfAccess.bfb$getLayers();
        if (layers == null) return;

        for (int i = 0; i < layerCount; i++) {
            ItemRenderState.LayerRenderState layer = layers[i];
            if (layer == null) continue;

            LayerRenderStateAccessor layerAccess = (LayerRenderStateAccessor) layer;
            int[] tints = layerAccess.bfb$getTints();

            // Инициализируем если пусто (initTints заполняет -1)
            if (tints == null || tints.length == 0) {
                tints = layer.initTints(4);
            }
            if (tints == null) continue;

            // tint на все вершины
            for (int j = 0; j < tints.length; j++) {
                tints[j] = tint;
            }
            layerAccess.bfb$setTints(tints);
        }
    }
}