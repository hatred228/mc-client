package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.Setting;
import com.example.bfb.setting.SettingGroup;
import org.lwjgl.glfw.GLFW;

/**
 * Обёртка над SettingGroup как отдельный CheatModule.
 * Позволяет видеть поднастройки Visuals как отдельные модули в GUI.
 */
public class VisualsSubModule extends CheatModule {
    private final SettingGroup group;

    public VisualsSubModule(String name, String description, SettingGroup group) {
        super(name, description, Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN);
        this.group = group;
        // регистрируем все children как настройки модуля
        for (Setting child : group.children()) {
            registerSetting(child);
        }
        // сам enabled — тоже настройка, чтобы галка включения была
        // (используем BooleanSetting "Enabled" из группы)
        applyEnabled(true);
    }

    public SettingGroup group() { return group; }

    /** Активен ли этот виджет — читаем из BooleanSetting "Enabled" внутри группы. */
    @Override
    public boolean isEnabled() {
        return group.bool("Enabled");
    }

    @Override
    public void setEnabled(boolean enabled) {
        Setting s = group.get("Enabled");
        if (s instanceof com.example.bfb.setting.BooleanSetting b) {
            b.set(enabled);
        }
    }

    @Override
    public void toggle() {
        Setting s = group.get("Enabled");
        if (s instanceof com.example.bfb.setting.BooleanSetting b) {
            b.toggle();
            com.example.bfb.BfbConfig.save();
        }
    }

    @Override
    public void applyEnabled(boolean enabled) {
        setEnabled(enabled);
    }
}