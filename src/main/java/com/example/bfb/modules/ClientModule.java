package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.Lang;
import com.example.bfb.Loc;
import com.example.bfb.combat.CombatController;
import com.example.bfb.combat.CombatMode;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class ClientModule extends CheatModule {
    public final ModeSetting language = add(new ModeSetting("Language", "English", "English", "Russian"));
    public final BooleanSetting hudEditor = add(new BooleanSetting("HUD Edit (LALT+LMB)", false));
    public final ModeSetting combatTarget = add(new ModeSetting("Combat target", "Vanilla",
            "Vanilla", "Grim", "Vulcan", "Matrix", "Polar", "ReallyWorld",
            "SpookyTime", "DonutSMP", "VimeMC", "RaidMine"));

    private String applied = "";
    private boolean hudState = false;
    private CombatMode appliedCombat = CombatMode.VANILLA;

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

        CombatMode newMode = switch (combatTarget.get()) {
            case "Grim" -> CombatMode.GRIM;
            case "Vulcan" -> CombatMode.VULCAN;
            case "Matrix" -> CombatMode.MATRIX;
            case "Polar" -> CombatMode.POLAR;
            case "ReallyWorld" -> CombatMode.REALLYWORLD;
            case "SpookyTime" -> CombatMode.SPOOKYTIME;
            case "DonutSMP" -> CombatMode.DONUTSMP;
            case "VimeMC" -> CombatMode.VIMEMC;
            case "RaidMine" -> CombatMode.RAIDMINE;
            default -> CombatMode.VANILLA;
        };
        if (newMode != appliedCombat) {
            appliedCombat = newMode;
            CombatController.setMode(newMode);
        }
    }
}