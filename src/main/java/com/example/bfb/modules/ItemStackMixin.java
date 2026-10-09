package com.example.bfb.mixin;

import com.example.bfb.ModuleManager;
import com.example.bfb.modules.TrueItemNamesModule;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void bfb$trueName(CallbackInfoReturnable<Text> cir) {
        TrueItemNamesModule mod = ModuleManager.get(TrueItemNamesModule.class);
        if (mod == null || !mod.isEnabled()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;
        cir.setReturnValue(self.getItem().getName());
    }
}