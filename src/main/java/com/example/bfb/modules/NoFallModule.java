package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

/**
 * NoFall — защита от fall damage.
 *
 * Три режима:
 *
 * LEGIT (по умолчанию, безопасно):
 *   Сбрасываем только клиентский fallDistance. Урон на сервере всё равно
 *   проходит, но клиент не трясёт камеру и не издаёт sound. НЕ защищает от урона.
 *   Используется как "визуальная гладкость".
 *
 * ONGROUND_FLIP (рискованно):
 *   Перед приземлением (за 1 тик) шлём PositionAndOnGround с onGround=true.
 *   Сервер думает что ты коснулся земли → сбрасывает свой fall counter.
 *   Палится на Intave/Vulcan если timing неправильный. Работает на NCP/Matrix.
 *   Только для fallDistance >= minFall.
 *
 * GRIM (только на Grim):
 *   Grim AC известен тем что проверяет velocity, а не position.
 *   Тут можно отправить PositionAndOnGround с Y + 0.001 и onGround=false
 *   один раз — Grim сбрасывает fall без flag. На других AC — флаг.
 *
 * ПОЧЕМУ НЕ PACKET-BASED Y+0.0625:
 *   Это старый trick, палится на всех современных AC.
 *   Delta Y = 0.0625 в воздухе без input = невозможное движение.
 */
public class NoFallModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit",
            "Legit", "OnGroundFlip", "Grim"));
    public final NumberSetting minFall = add(new NumberSetting("Min fall", 3.0, 2.0, 20.0, 0.5));

    private boolean flippedThisFall;

    public NoFallModule() {
        super("NoFall", "Fall damage protection", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || player.networkHandler == null) return;

        // сброс флага когда игрок приземлился реально
        if (player.isOnGround()) {
            flippedThisFall = false;
            return;
        }

        // если не падаем достаточно — ничего не делаем
        if (player.fallDistance < minFall.getFloat()) return;

        // в воде/лаве/на лестнице/в элитрах fall damage не грозит
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;
        if (player.getAbilities().flying || player.getVehicle() != null) return;

        switch (mode.get()) {
            case "OnGroundFlip" -> onGroundFlip(client, player);
            case "Grim" -> grimFall(client, player);
            default -> legitFall(player);
        }
    }

    /**
     * LEGIT — только клиентский сброс. Урон на сервере всё равно пройдёт.
     * Полезно чтобы камера не тряслась и звук не играл.
     */
    private void legitFall(net.minecraft.client.network.ClientPlayerEntity player) {
        player.fallDistance = 0f;
    }

    /**
     * ONGROUND_FLIP — один packet с onGround=true в момент падения.
     * Сервер получает "я коснулся земли" → сбрасывает свой fall counter.
     * Палится на Intave/Vulcan. Работает на NCP, Matrix.
     */
    private void onGroundFlip(net.minecraft.client.MinecraftClient client,
                              net.minecraft.client.network.ClientPlayerEntity player) {
        if (flippedThisFall) return;

        // отправляем ДО того как vanilla сам отправит packet
        // onGround=true, Y как есть, без изменения координат
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(), player.getY(), player.getZ(),
                true, player.horizontalCollision));

        // на следующий тик vanilla сам отправит packet с onGround=false
        // (потому что isOnGround() реально false) — дельта mask уберётся

        player.fallDistance = 0f;
        flippedThisFall = true;
    }

    /**
     * GRIM — специфика Grim AC. Grim проверяет motion vs position delta.
     * Отправляем micro-Y packet с onGround=false. Grim сбрасывает fall tracker.
     * На Intave/Vulcan — моментальный flag.
     */
    private void grimFall(net.minecraft.client.MinecraftClient client,
                          net.minecraft.client.network.ClientPlayerEntity player) {
        if (flippedThisFall) return;

        // +0.001 — минимальная дельта, ниже порога проверки на motion
        // onGround=false — не палимся как "якобы на земле"
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(), player.getY() + 0.001, player.getZ(),
                false, player.horizontalCollision));

        player.fallDistance = 0f;
        flippedThisFall = true;
    }

    @Override
    public void onDisable() {
        flippedThisFall = false;
    }
}