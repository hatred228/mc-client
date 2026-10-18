package com.example.bfb.render.nvg;

import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Загрузка TTF-шрифтов в NanoVG.
 *
 * Шрифты грузятся в память и передаются NanoVG через nvgCreateFontMem.
 * NanoVG сам управляет атласом глифов.
 * Буфер освобождается NanoVG при nvgDeleteGL3.
 */
public final class NvgFont {

    public static final String RUBIK = "rubik";
    public static final String LUCIDE = "lucide";

    // Пути должны совпадать с реальным расположением файлов в jar.
    // Из build/jar: assets/undetected/font/rubik-regular.ttf
    private static final String RUBIK_PATH = "/assets/undetected/font/rubik-regular.ttf";
    private static final String LUCIDE_PATH = "/assets/undetected/font/lucide.ttf";

    private NvgFont() {}

    public static boolean loadRubik() {
        return load(RUBIK_PATH, RUBIK);
    }

    public static boolean loadLucide() {
        return load(LUCIDE_PATH, LUCIDE);
    }

    private static boolean load(String classpath, String name) {
        if (!NvgContext.ready()) return false;

        try (InputStream is = NvgFont.class.getResourceAsStream(classpath)) {
            if (is == null) {
                System.err.println("[BFB] Font not found: " + classpath);
                return false;
            }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            // freeData = true → NanoVG освободит буфер при nvgDeleteGL3
            int result = NanoVG.nvgCreateFontMem(
                    NvgContext.handle(),
                    name,
                    buffer,
                    true
            );
            if (result == -1) {
                System.err.println("[BFB] Failed to load font: " + name);
                MemoryUtil.memFree(buffer);
                return false;
            }
            System.out.println("[BFB] Loaded font: " + name + " (" + bytes.length + " bytes)");
            return true;

        } catch (Exception e) {
            System.err.println("[BFB] Font load failed: " + classpath + " — " + e.getMessage());
            return false;
        }
    }
}