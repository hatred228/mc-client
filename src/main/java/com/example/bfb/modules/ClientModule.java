package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.Lang;
import com.example.bfb.Loc;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class ClientModule extends CheatModule {
    public final ModeSetting language = add(new ModeSetting("Language", "English", "English", "Russian"));
    public final BooleanSetting hudEditor = add(new BooleanSetting("HUD Edit (LALT+LMB)", false));
    private String applied = "";
    private boolean hudState = false;

    public ClientModule() {
        super("Client", "Client settings, language, HUD editor", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }

    @Override
    public void onTick() {
        String now = language.get();
        if (!now.equals(applied)) {
            applied = now;
            boolean ru = now.equalsIgnoreCase("Russian");
            Lang.setRussian(ru);
            Loc.setRussian(ru);
        }
        boolean newHud = hudEditor.get();
        if (newHud != hudState) {
            hudState = newHud;
            HudEditor.editing = newHud;
        }
    }
}