package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.FullbrightModule;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 2))
    private Object bfb$fullbright(SimpleOption<?> option) {
        Object value = option.getValue();
        FullbrightModule fullbright = ModuleManager.get(FullbrightModule.class);
        if (fullbright != null && fullbright.isEnabled() && value instanceof Double) {
            return fullbright.level.get() / 15.0 * 16.0;
        }
        return value;
    }
}
