package com.example.bfb.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor для приватных полей PlayerMoveC2SPacket.Full.
 *
 * В 1.21.11 yarn поля приватные, геттеров без параметров нет.
 * Публичные методы getX(double) — интерполяционные, не подходят.
 *
 * Этот accessor даёт прямой доступ к полям x/y/z, чтобы при
 * пересоздании Full-пакета (silent rotation) использовать
 * оригинальные координаты, а не текущие player.getX().
 */
@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveFullAccessor {
    @Accessor("x") double bfb$getX();
    @Accessor("y") double bfb$getY();
    @Accessor("z") double bfb$getZ();
}