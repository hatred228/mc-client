package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * AntiAFK — чтобы сервер не кикал за неактивность.
 *
 * Раньше: silent rotation + swing + jump каждые 40 сек.
 * Проблема: rotation через AnticheatBypass.smoothLook давала резкий snap.
 *
 * Теперь: только swing + микро-движение через addVelocity.
 * Плюс — jump если на земле, но только раз в 40 сек.
 * Сервер видит активность.
 */
public class AntiAFKModule extends CheatModule {
    public final NumberSetting seconds = add(new NumberSetting("Seconds", 40, 10, 180, 5));
    private int ticks;

    public AntiAFKModule() {
        super("AntiAFK", "Jumps so you don't get kicked", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;

        if (++ticks < seconds.getInt() * 20) return;
        ticks = 0;

        // swingHand — самый безопасный признак активности
        player.swingHand(Hand.MAIN_HAND);

        // небольшой рывок вперёд через input override — не setVelocity
        // (setVelocity даёт мгновенную дельту = flag на Intave/Vulcan)
        // Вместо этого — просто jump если на земле
        if (player.isOnGround()) {
            player.jump();
        } else {
            // в воздухе — микро addVelocity (мало палится)
            player.addVelocity(0, 0.02, 0);
        }
    }

    @Override
    public void onDisable() {
        ticks = 0;
    }
}