package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.HudStyle;
import com.example.bfb.ModuleManager;
import com.example.bfb.SalFont;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import com.example.bfb.Lang;
import net.minecraft.client.gui.DrawContext;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KeybindsModule extends CheatModule {
    public final ModeSetting position = add(new ModeSetting("Position", "BottomRight",
            "TopLeft", "TopRight", "BottomLeft", "BottomRight"));

    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.1));
    public final NumberSetting rowHeight = add(new NumberSetting("Row height", 12, 8, 20, 1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 4, 0, 10, 1));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 2, 0, 5, 1));
    public final NumberSetting headerH = add(new NumberSetting("Header height", 14, 10, 24, 1));

    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0xE0100A14));
    public final ColorSetting nameColor = add(new ColorSetting("Name color", 0xFFFFFF));
    public final ColorSetting keyColor = add(new ColorSetting("Key color", 0xFF5CA8));

    public final BooleanSetting showAll = add(new BooleanSetting("Show all bound", false));

    public KeybindsModule() {
        super("Keybinds", "Shows active keybinds", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        applyEnabled(true);
    }

    public static void draw(DrawContext context, MinecraftClient client) {
        KeybindsModule mod = ModuleManager.get(KeybindsModule.class);
        if (mod == null || !mod.isEnabled()) return;

        List<CheatModule> bound = new ArrayList<>();
        for (CheatModule module : ModuleManager.getModules()) {
            if (module == mod) continue;
            int key = module.getKeybind();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            if (!mod.showAll.get() && !module.isEnabled()) continue;
            bound.add(module);
        }
        if (bound.isEmpty()) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        float scale = mod.scale.getFloat();
        int rowH = mod.rowHeight.getInt();
        int radius = mod.radius.getInt();
        int edgeW = mod.edgeWidth.getInt();
        int headerH = mod.headerH.getInt();
        int c1 = mod.edgeColor1.getRgb() & 0xFFFFFF;
        int c2 = mod.edgeColor2.getRgb() & 0xFFFFFF;
        int bg = mod.bgColor.getRgb();
        int nameArgb = 0xFF000000 | (mod.nameColor.getRgb() & 0xFFFFFF);
        int keyArgb = 0xFF000000 | (mod.keyColor.getRgb() & 0xFFFFFF);

        int nameW = 0;
        int keyW = 0;
        List<String> keys = new ArrayList<>();
        for (CheatModule module : bound) {
            nameW = Math.max(nameW, SalFont.width(module.getName()));
            String k = "[" + keyName(module.getKeybind()) + "]";
            keys.add(k);
            keyW = Math.max(keyW, SalFont.width(k));
        }

        int contentW = 8 + nameW + 12 + keyW + 8;
        int contentH = bound.size() * rowH;
        int boxW = contentW;
        int boxH = headerH + contentH + 4;
        int totalW = (int) (boxW * scale);
        int totalH = (int) (boxH * scale);

        int defX, defY;
        switch (mod.position.get()) {
            case "TopLeft" -> { defX = 8; defY = 8; }
            case "TopRight" -> { defX = sw - totalW - 8; defY = 8; }
            case "BottomLeft" -> { defX = 8; defY = sh - totalH - 8; }
            default -> { defX = sw - totalW - 8; defY = sh - totalH - 8; }
        }
        int baseX = HudEditor.getX("keybinds", defX);
        int baseY = HudEditor.getY("keybinds", defY);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);

        HudStyle.panel(context, 0, 0, boxW, boxH, radius, edgeW, c1, c2, bg, true);
        HudStyle.header(context, 0, 0, boxW, headerH, c1, c2, Lang.tr("Keybinds"), 0xFFFFFF);

        int y = headerH + 2;
        for (int i = 0; i < bound.size(); i++) {
            CheatModule module = bound.get(i);
            String key = keys.get(i);
            SalFont.draw(context, module.getName(), 6, y + 1, nameArgb);
            int kw = SalFont.width(key);
            SalFont.draw(context, key, boxW - 6 - kw, y + 1, keyArgb);
            y += rowH;
        }

        matrices.popMatrix();

        HudEditor.outline(context, "keybinds", baseX, baseY, totalW, totalH);
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
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSH";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSH";
            case GLFW.GLFW_KEY_SPACE -> "SPC";
            default -> Integer.toString(key);
        };
    }
}