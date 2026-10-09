package com.example.bfb;

import com.example.bfb.setting.Setting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CheatModule {
    private final String name;
    private final String description;
    private final Category category;
    private final int defaultKeybind;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;
    private int keybind;

    public CheatModule(String name, String description, Category category, int keybind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultKeybind = keybind;
        this.keybind = keybind;
    }

    protected final <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void applyEnabled(boolean enabled) {
        this.enabled = enabled;
    }

        public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            playSound(true);
            onEnable();
            NotificationSystem.push(getName() + " enabled", getCategory().getColor());
        } else {
            playSound(false);
            onDisable();
            NotificationSystem.push(getName() + " disabled", 0xFF888888);
        }
        BfbConfig.save();
    }

    private void playSound(boolean on) {
        var client = BfbMod.getClient();
        if (client == null || client.player == null) return;
        if (on) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.6f, 1.6f);
        } else {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6f, 0.7f);
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getKeybind() {
        return keybind;
    }

    public void applyKey(int keybind) {
        this.keybind = keybind;
    }

    public void bind(int keybind) {
        this.keybind = keybind;
        BfbMod.updateBind(this);
        BfbConfig.save();
    }

    public void unbind() {
        bind(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
    }

    public void resetKeybind() {
        bind(defaultKeybind);
    }

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public void onTick() {
    }

    public void onStartTick() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
    }
}