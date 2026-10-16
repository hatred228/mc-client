package com.example.bfb.mixin;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Accessor для ItemCooldownManager (class_1796).
 *
 * В 1.21.11 yarn:
 *   - поле "entries" — Map<Identifier, ItemCooldownManager$Entry>
 *   - поле "tick" — текущий tick count
 *
 * Entry — record с полями startTick и endTick.
 */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownsAccessor {
    @Accessor("entries")
    Map<Identifier, ?> bfb$getEntries();

    @Accessor("tick")
    int bfb$getTick();
}