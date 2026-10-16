package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import org.lwjgl.glfw.GLFW;

/**
 * AirJump — прыжок в воздухе.
 *
 * РАНЬШЕ: setVelocity(v.x, 0.42, v.z) — мгновенная Y-velocity 0.42.
 * Intave ловит моментально. Vulcan тоже.
 *
 * ТЕПЕРЬ: addVelocity с малым шагом. НЕ даёт высоту сразу —
 * но и не палится. Можно прыгнуть на 2-3 блока вверх за 5-10 прыжков.
 *
 * ТРЕБУЕТ: игрок в воздухе, не в воде/лаве, не на лианах, не летает.
 * Активируется по jump-клавише (как обычный прыжок).
 */
public class AirJumpModule extends CheatModule {
    private boolean wasJump;
    private int cooldown;

    public AirJumpModule() {
        super("AirJump", "Jump in mid-air (safe mode)", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;

        if (cooldown > 0) cooldown--;

        boolean jump = client.options.jumpKey.isPressed();
        boolean pressed = jump && !wasJump;
        wasJump = jump;

        if (!pressed || cooldown > 0) return;

        // в воде/лаве/на лианах — не работаем
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;

        // летаем / в транспорте — не работаем
        if (player.getAbilities().flying || player.getVehicle() != null) return;

        // на земле — это обычный прыжок, не наш случай
        if (player.isOnGround()) return;

        // сбрасываем падение чтобы не копилась fall при подъёме
        player.fallDistance = 0f;

        // микро-импульс вверх — 0.10 за прыжок
        // для сравнения: vanilla jump = 0.42
        // наш = ~1/4 ванильного, но не палимся
        player.addVelocity(0, 0.10, 0);

        // кулдаун чтобы не спамить пакетами
        cooldown = 6;
    }

    @Override
    public void onDisable() {
        wasJump = false;
        cooldown = 0;
    }
}