package com.example.bfb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudEditor {
    public static boolean editing = false;

    private static final Map<String, int[]> POS = new HashMap<>();
    private static final Map<String, int[]> DEF = new LinkedHashMap<>();

    private static String dragging = null;
    private static int dragOffX = 0, dragOffY = 0;

    private HudEditor() {}

    public static int[] register(String id, int defX, int defY) {
        DEF.putIfAbsent(id, new int[]{defX, defY});
        return POS.computeIfAbsent(id, k -> new int[]{defX, defY});
    }

    public static int drawX(String id, int defX, int defY) { return register(id, defX, defY)[0]; }
    public static int drawY(String id, int defX, int defY) { return register(id, defX, defY)[1]; }
    public static int getX(String id, int defX) { return register(id, defX, 0)[0]; }
    public static int getY(String id, int defY) { return register(id, 0, defY)[1]; }

    public static void reset(String id) {
        int[] d = DEF.get(id);
        if (d == null) return;
        int[] p = POS.computeIfAbsent(id, k -> new int[]{d[0], d[1]});
        p[0] = d[0];
        p[1] = d[1];
    }

    public static void resetAll() {
        for (Map.Entry<String, int[]> e : DEF.entrySet()) {
            int[] p = POS.computeIfAbsent(e.getKey(), k -> new int[]{e.getValue()[0], e.getValue()[1]});
            p[0] = e.getValue()[0];
            p[1] = e.getValue()[1];
        }
    }

    public static void tick() {
        if (!editing) { dragging = null; return; }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        boolean alt = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
        boolean lmb = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmb = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (!alt) { dragging = null; return; }

        double mx = client.mouse.getX() * client.getWindow().getScaledWidth()
                / (double) client.getWindow().getWidth();
        double my = client.mouse.getY() * client.getWindow().getScaledHeight()
                / (double) client.getWindow().getHeight();

        if (dragging != null && lmb) {
            int[] p = POS.get(dragging);
            if (p != null) {
                p[0] = (int) (mx - dragOffX);
                p[1] = (int) (my - dragOffY);
            }
        } else if (dragging != null && rmb) {
            reset(dragging);
            dragging = null;
        } else if (!lmb) {
            dragging = null;
        }
    }

    public static void hitTest(String id, int x, int y, int w, int h) {
        if (!editing) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        boolean alt = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
        if (!alt) return;

        double mx = client.mouse.getX() * client.getWindow().getScaledWidth()
                / (double) client.getWindow().getWidth();
        double my = client.mouse.getY() * client.getWindow().getScaledHeight()
                / (double) client.getWindow().getHeight();

        if (dragging == null && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                && mx >= x && mx <= x + w && my >= y && my <= y + h) {
            dragging = id;
            dragOffX = (int) mx - x;
            dragOffY = (int) my - y;
        }
    }

    public static void outline(net.minecraft.client.gui.DrawContext ctx, String id, int x, int y, int w, int h) {
        if (!editing) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        boolean alt = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
        if (!alt) return;

        boolean hover = isHovered(client, x, y, w, h);
        boolean drag = dragging != null && dragging.equals(id);
        if (!hover && !drag) return;

        int color = drag ? 0xFFFFFF00 : 0x80FF5CA8;
        ctx.fill(x - 1, y - 1, x + w + 1, y, color);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        ctx.fill(x - 1, y, x, y + h, color);
        ctx.fill(x + w, y, x + w + 1, y + h, color);
        hitTest(id, x, y, w, h);
    }

    private static boolean isHovered(MinecraftClient client, int x, int y, int w, int h) {
        double mx = client.mouse.getX() * client.getWindow().getScaledWidth()
                / (double) client.getWindow().getWidth();
        double my = client.mouse.getY() * client.getWindow().getScaledHeight()
                / (double) client.getWindow().getHeight();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ================= сохранение/загрузка позиций =================

    /** Снапшот всех позиций как JSON-объект. */
    public static JsonObject toJson() {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, int[]> e : POS.entrySet()) {
            int[] v = e.getValue();
            JsonArray arr = new JsonArray();
            arr.add(v[0]);
            arr.add(v[1]);
            obj.add(e.getKey(), arr);
        }
        return obj;
    }

    public static void fromJson(JsonObject obj) {
        if (obj == null) return;
        for (String key : obj.keySet()) {
            JsonArray arr = obj.getAsJsonArray(key);
            if (arr == null || arr.size() < 2) continue;
            POS.put(key, new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()});
        }
    }
}