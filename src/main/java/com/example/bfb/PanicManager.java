package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PanicManager {
    private static boolean panicked = false;
    private static boolean wasComboDown = false;
    private static Path panicFile;

    private PanicManager() {}

    public static boolean isPanicked() { return panicked; }
    public static boolean hideVisuals() { return panicked; }

    public static void setPanicFile(String path) {
        try {
            panicFile = Paths.get(path);
        } catch (Throwable ignored) {}
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();

        // CTRL + SHIFT + SPACE
        boolean combo = isDown(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
                && isDown(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                && isDown(handle, GLFW.GLFW_KEY_SPACE);

        if (combo && !wasComboDown) {
            triggerPanic(client);
        }
        wasComboDown = combo;
    }

    private static void triggerPanic(MinecraftClient client) {
        panicked = true;

        // 1. Выключаем модули
        for (CheatModule m : ModuleManager.getModules()) {
            if (m.isEnabled()) m.applyEnabled(false);
        }

        // 2. Закрываем ClickGui
        if (client.currentScreen != null) client.setScreen(null);

        // 3. Пишем panic-file
        writePanicSignal();

        // 4. Уведомление
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§7[§aOK§7] hidden"), true);
        }
    }

    private static void writePanicSignal() {
        if (panicFile == null) return;
        try {
            Files.createDirectories(panicFile.getParent());
            Files.writeString(panicFile, "PANIC_CLEAN\n");
        } catch (Exception ignored) {}
    }

    private static boolean isDown(long handle, int key) {
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }
}