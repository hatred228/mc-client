package com.example.bfb.hud.widgets;

import com.example.bfb.ModuleManager;
import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.ListHudWidget;
import com.example.bfb.render.ui.Icons;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KeybindsWidget extends ListHudWidget<KeybindsWidget.Entry> {

    public KeybindsWidget(HudCompactStyle.Options options) {
        super("Keybinds", Icons.KEYBOARD, 0.02f, 0.72f, options);
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        for (var module : ModuleManager.getModules()) {
            int key = module.getKeybind();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            String n = module.getName();
            if (n.equals("Interface") || n.equals("Arraylist")
                    || n.equals("Client") || n.equals("Theme")) continue;
            result.add(new Entry(module.getName(), key));
        }
        return result;
    }

    @Override protected Object key(Entry entry) { return entry.moduleName; }
    @Override protected String left(Entry entry) { return entry.moduleName; }
    @Override protected String right(Entry entry) { return "[" + keyName(entry.key) + "]"; }
    @Override protected String valueOption() { return "Key"; }

    @Override
    protected boolean nativeIcon(Entry entry) { return true; }

    @Override
    protected void drawNativeIcon(net.minecraft.client.gui.DrawContext ctx, Entry entry,
                                   float iconX, float iconY, float iconSize, int alpha) {
        // Иконка клавиши через Lucide
        com.example.bfb.render.ui.IconFont.draw(ctx, Icons.KEY,
                iconX, iconY, iconSize, (alpha << 24) | 0xFFFFFFFF);
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        if (key >= GLFW.GLFW_MOUSE_BUTTON_MIDDLE && key <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return switch (key) {
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                default -> "M" + (key + 1);
            };
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isEmpty()) return name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSH";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSH";
            case GLFW.GLFW_KEY_SPACE -> "SPC";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_BACKSPACE -> "BKSP";
            default -> "K" + key;
        };
    }

    public static final class Entry {
        public final String moduleName;
        public final int key;
        public Entry(String moduleName, int key) {
            this.moduleName = moduleName;
            this.key = key;
        }
    }
}