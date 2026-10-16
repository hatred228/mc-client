package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Spider — лазание по стенам.
 *
 * РАНЬШЕ: setVelocity(v.x, 0.12, v.z) на стене. Мгновенная смена Y-velocity —
 * Intave ловит моментально. Vulcan тоже.
 *
 * ТЕПЕРЬ: addVelocity(0, step, 0) — медленно, шагами по 0.04 за тик.
 * Максимум 0.20 за тик (примерно как прыжок). Не даёт мгновенный импульс.
 *
 * Плюс условия: только когда реально стена впереди, на земле НЕТ,
 * не в воде/лаве, не на лианах.
 */
public class SpiderModule extends CheatModule {
    public final NumberSetting step = add(new NumberSetting("Step", 0.04, 0.02, 0.08, 0.005));
    public final NumberSetting maxY = add(new NumberSetting("Max Y velocity", 0.20, 0.10, 0.30, 0.01));

    public SpiderModule() {
        super("Spider", "Climb walls", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;

        // не в воде/лаве — там другие правила
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;

        // не летим / не в транспорте / не в спектейторе
        if (player.getAbilities().flying || player.getVehicle() != null || player.isSpectator()) return;

        // уже на земле — не спайдерим
        if (player.isOnGround()) return;

        // нужно чтобы игрок хотел двигаться вперёд
        if (player.forwardSpeed <= 0.01f) return;

        // проверяем горизонтальные коллизии — стена впереди?
        if (!player.horizontalCollision) return;

        // velocity Y сейчас
        double yVel = player.getVelocity().y;

        // уже быстро летим вверх — не помогаем (палимся)
        if (yVel > maxY.get()) return;

        // аддитивно — медленный шаг вверх
        player.addVelocity(0, step.get(), 0);

        // сбрасываем fallDistance — иначе при подъёме накапливается
        player.fallDistance = 0f;
    }
}