package com.example.bfb.render.nvg;

import static org.lwjgl.nanovg.NanoVGGL3.NVG_ANTIALIAS;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES;
import static org.lwjgl.nanovg.NanoVGGL3.nvgCreate;
import static org.lwjgl.nanovg.NanoVGGL3.nvgDelete;

/**
 * Обёртка над NanoVG-контекстом.
 *
 * Lazy init — создаётся при первом вызове ensure().
 * Должен использоваться ТОЛЬКО в render-thread с активным GL-контекстом.
 */
public final class NvgContext {

    private static long handle = 0L;
    private static boolean failed = false;

    private NvgContext() {}

    /** Есть ли активный контекст. */
    public static boolean ready() {
        return handle != 0L;
    }

    /** Получить handle для NanoVG вызовов. 0 если не готов. */
    public static long handle() {
        return handle;
    }

    /**
     * Lazy init. Возвращает true если контекст готов.
     * Вызывать в render-thread.
     */
    public static boolean ensure() {
        if (failed) return false;
        if (handle != 0L) return true;

        try {
            handle = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
            if (handle == 0L) {
                System.err.println("[BFB] NanoVG init failed — nvgCreate returned 0");
                failed = true;
                return false;
            }
            System.out.println("[BFB] NanoVG initialized (handle=" + handle + ")");
            return true;
        } catch (Throwable t) {
            System.err.println("[BFB] NanoVG init exception: " + t.getMessage());
            t.printStackTrace();
            failed = true;
            return false;
        }
    }

    /** Уничтожить контекст. Вызывать при остановке клиента. */
    public static void destroy() {
        if (handle != 0L) {
            try {
                nvgDelete(handle);
                System.out.println("[BFB] NanoVG destroyed");
            } catch (Throwable t) {
                System.err.println("[BFB] NanoVG destroy failed: " + t.getMessage());
            }
            handle = 0L;
        }
    }
}