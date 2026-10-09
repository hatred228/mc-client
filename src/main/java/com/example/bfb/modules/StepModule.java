package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.attribute.EntityAttributes;
import org.lwjgl.glfw.GLFW;

public class StepModule extends CheatModule {
    public final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.6, 1.5, 0.1));

    public StepModule() {
        super("Step", "Step up full blocks", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var player = BfbMod.getClient().player;
        if (player == null) return;
        var attribute = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attribute != null) attribute.setBaseValue(height.get());
    }

    @Override
    public void onDisable() {
        var player = BfbMod.getClient().player;
        if (player == null) return;
        var attribute = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attribute != null) attribute.setBaseValue(0.6);
    }
}