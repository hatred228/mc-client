package com.example.bfb;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

public class LuaModule extends CheatModule {
    private final LuaTable table;

    public LuaModule(String name, String description, Category category, LuaTable table) {
        super(name, description, category, GLFW.GLFW_KEY_UNKNOWN);
        this.table = table;
    }

    @Override
    public void onEnable() {
        call("onEnable");
    }

    @Override
    public void onDisable() {
        call("onDisable");
    }

    @Override
    public void onStartTick() {
        call("onStart");
    }

    @Override
    public void onTick() {
        call("onTick");
    }

    /** Вызывается из ScriptManager.kill() когда TriggerBot/KillAura убили цель. */
    public void onKill(String victim) {
        LuaValue value = table.get("onKill");
        if (value != null && value.isfunction()) {
            try {
                value.call(LuaValue.valueOf(victim));
            } catch (Exception e) {
                ScriptManager.report(getName(), "onKill", e);
            }
        }
    }

    private void call(String name) {
        LuaValue value = table.get(name);
        if (value != null && value.isfunction()) {
            try {
                value.call();
            } catch (Exception e) {
                ScriptManager.report(getName(), name, e);
            }
        }
    }
}