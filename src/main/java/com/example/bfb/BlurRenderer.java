package com.example.bfb;

import net.minecraft.client.MinecraftClient;

/**
 * Обёртка над ванильным блюром для HUD.
 * В 1.21.11 прямой GL11 недоступен, поэтому используем GameRenderer.renderBlur().
 */
public final class BlurRenderer {
    private static boolean blurCalledThisFrame = false;

    private BlurRenderer() {}

    /** Вызывать в начале HUD-рендера — 1 раз за кадр. */
    public static void beginFrame() {
        blurCalledThisFrame = false;
    }

    /** Размывает весь экран (за кадром). Вызывать ПЕРЕД отрисовкой панелей с blur. */
    public static void applyBlur() {
        if (blurCalledThisFrame) return;
        blurCalledThisFrame = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null) return;
        try {
            // 1.21.11: renderBlur() без аргументов
            client.gameRenderer.renderBlur();
        } catch (Throwable t) {
            // Если сигнатура другая — не падаем, просто без блюра.
        }
    }

    public static void endFrame() {
        blurCalledThisFrame = false;
    }
}