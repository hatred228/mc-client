package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

public class DerpModule extends CheatModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Rage", "Legit", "Rage"));
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
    }

    public void begin(ClientPlayerEntity player) {
        savedYaw = player.getYaw();
        savedPitch = player.getPitch();
        active = true;
        float step = (mode.is("Rage") ? 55f : 22f) * speed.getFloat();
        spin += step;
        derpYaw = savedYaw + spin;
        float derpPitch = (float) (Math.sin(spin * 0.05) * (mode.is("Rage") ? 89 : 35));
        player.setYaw(derpYaw);
        player.setPitch(derpPitch);
        player.setHeadYaw(derpYaw);
    }

    public void end(ClientPlayerEntity player) {
        if (!active) return;
        player.setYaw(savedYaw);
        player.setPitch(savedPitch);
        // camera restored, but head/body keep spinning (visible in F5)
        player.setHeadYaw(derpYaw);
        player.setBodyYaw(derpYaw);
        active = false;
    }
}
