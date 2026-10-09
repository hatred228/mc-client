package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.SalFont;
import com.example.bfb.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class KeystrokesModule extends CheatModule {
    public final BooleanSetting mouse = add(new BooleanSetting("Mouse", true));
    public final BooleanSetting space = add(new BooleanSetting("Space", true));

    public KeystrokesModule() {
        super("Keystrokes", "WASD, Space and mouse keys on screen", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void draw(DrawContext context, MinecraftClient client, boolean mouseKeys, boolean spaceKey) {
        if (client.player == null) return;
        int w = 84, rows = 2 + (spaceKey ? 1 : 0) + (mouseKeys ? 1 : 0);
        int h = rows * 28 - 4;
        int x = context.getScaledWindowWidth() - w - 8;
        int y = context.getScaledWindowHeight() - h - 8;
        context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x9008080C);
        int bw = 26, bh = 26, gap = 2;
        long handle = client.getWindow().getHandle();
        int cy = y;
        key(context, x + bw + gap, cy, bw, bh, "W", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS);
        cy += bh + gap;
        key(context, x, cy, bw, bh, "A", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS);
        key(context, x + bw + gap, cy, bw, bh, "S", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS);
        key(context, x + (bw + gap) * 2, cy, bw, bh, "D", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS);
        cy += bh + gap;
        if (spaceKey) {
            key(context, x, cy, w, 20, "SPACE", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
            cy += 20 + gap;
        }
        if (mouseKeys) {
            key(context, x, cy, (w - gap) / 2, 24, "LMB", GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
            key(context, x + (w + gap) / 2, cy, (w - gap) / 2, 24, "RMB", GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS);
        }
    }

    private static void key(DrawContext context, int x, int y, int w, int h, String label, boolean pressed) {
        context.fill(x, y, x + w, y + h, pressed ? 0xFF2E86AB : 0x50141820);
        int tw = SalFont.width(label);
        SalFont.draw(context, label, x + (w - tw) / 2, y + (h - SalFont.height()) / 2,
                pressed ? 0xFFFFFFFF : 0xFF9AA0A6);
    }
}