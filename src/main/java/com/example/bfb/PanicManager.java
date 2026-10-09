package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class PanicManager {
    private static boolean panicked = false;
    private static boolean wasComboDown = false;

    private PanicManager() {}

    public static boolean isPanicked() { return panicked; }
    public static boolean hideVisuals() { return panicked; }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();

        boolean combo = isDown(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
                && isDown(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                && isDown(handle, GLFW.GLFW_KEY_DELETE);

        if (combo && !wasComboDown) {
            panicked = !panicked;
            if (panicked) {
                // выключаем все модули без сохранения в конфиг
                for (CheatModule m : ModuleManager.getModules()) {
                    if (m.isEnabled()) m.applyEnabled(false);
                }
                // закрываем любой открытый экран
                if (client.currentScreen != null) client.setScreen(null);
            }
            if (client.player != null) {
                client.player.sendMessage(Text.literal(
                        panicked ? "§c[Panic] hidden" : "§a[Panic] restored"
                ), true);
            }
        }
        wasComboDown = combo;
    }

    private static boolean isDown(long handle, int key) {
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }
}