package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

/**
 * Sprint — auto-sprint, включая sideways.
 *
 * Grim палит sprint в воде / при использовании item, если нет условия.
 */
public class SprintModule extends CheatModule {
    public final BooleanSetting omni = add(new BooleanSetting("Omni", true));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Default", "Default", "SpookyTime"));

    public SprintModule() {
        super("Sprint", "Auto-sprint, including sideways", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = BfbMod.getClient().player;
        if (player == null || player.isSneaking()) return;

        // не в воде/лаве/на лестнице
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return;
        if (player.getAbilities().flying || player.getVehicle() != null) return;

        boolean moving = omni.get()
                ? player.forwardSpeed != 0 || player.sidewaysSpeed != 0
                : player.forwardSpeed > 0;
        if (!moving) return;
        if (player.isSprinting()) return;

        // в SpookyTime режиме — только вперёд
        if (mode.is("SpookyTime") && player.forwardSpeed <= 0) return;

        player.setSprinting(true);
    }
}