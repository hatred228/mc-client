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

        // 1. Выключаем модули + вызываем onDisable
        for (CheatModule m : ModuleManager.getModules()) {
            if (m.isEnabled()) {
                m.applyEnabled(false);
                try { m.onDisable(); } catch (Throwable ignored) {}
            }
        }

        // 2. Отключаем HUD полностью
        try {
            com.example.bfb.hud.HudIntegration hud = com.example.bfb.hud.HudIntegration.get();
            if (hud != null) hud.setEnabled(false);
        } catch (Throwable ignored) {}

        // 3. Отдельно глушим RENDER и VISUALS модули (мало ли)
        for (CheatModule m : ModuleManager.getModules()) {
            Category cat = m.getCategory();
            if (cat == Category.RENDER || cat == Category.VISUALS) {
                m.applyEnabled(false);
                try { m.onDisable(); } catch (Throwable ignored) {}
            }
        }

        // 4. Скрываем меню
        try {
            if (client.currentScreen != null) client.setScreen(null);
        } catch (Throwable ignored) {}

        // 5. Пишем panic-файл для лаунчера
        writePanicSignal();

        // 6. Уведомление (только тебе видно)
        try {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("§7[§aOK§7] hidden"), true);
            }
        } catch (Throwable ignored) {}
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