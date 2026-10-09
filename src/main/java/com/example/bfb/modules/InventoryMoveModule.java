package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class InventoryMoveModule extends CheatModule {
    public final BooleanSetting sneak = add(new BooleanSetting("Sneak", true));

    public InventoryMoveModule() {
        super("InventoryMove", "Move with inventory and menus open", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }
}
