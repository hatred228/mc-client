package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

public class DerpModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Rage"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1));

    private float spin;
    private float savedYaw;
    private float savedPitch;
    private float derpYaw;
    private boolean active;

    public DerpModule() {
        super("Derp", "Spins your head for other players", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        active = false;
        RotationManager.release();
    }

    public void begin(ClientPlayerEntity player) {
        if (!active) {
            savedYaw = player.getYaw();
            savedPitch = player.getPitch();
            active = true;
        }
        float step = (mode.is("Rage") ? 15f : 8f) * speed.getFloat();
        spin += step;
        derpYaw = savedYaw + spin;

        float derpPitch = (float) (Math.sin(spin * 0.05) * (mode.is("Rage") ? 45 : 20));

        // silent rotation — сервер видит, камера игрока свободна
        RotationManager.setAndSend(derpYaw, derpPitch, 4);

        // тело тоже поворачиваем
        player.setHeadYaw(derpYaw);
        player.setBodyYaw(derpYaw);
    }

    public void end(ClientPlayerEntity player) {
        if (!active) return;
        RotationManager.release();
        active = false;
    }
}