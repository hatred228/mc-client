package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PlayerModelModule extends CheatModule {
    public static PlayerModelModule instance;
    public final ModeSetting model = add(new ModeSetting("Model", "Off", "Off", "Tung Tung", "Hana", "Yuna", "Rin"));

    public PlayerModelModule() {
        super("PlayerModel", "Player model in inventory", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    private static int boxX;
    private static int boxY;
    private static int boxW;
    private static int boxH;
    private static boolean pending;

    public static boolean replacesPlayer() {
        return instance != null && instance.isEnabled() && !instance.model.is("Off");
    }

    public static void remember(int x, int y, int width, int height) {
        boxX = x;
        boxY = y;
        boxW = width;
        boxH = height;
        pending = true;
    }

    public static void drawPending(DrawContext context) {
        if (!pending) return;
        pending = false;
        if (!replacesPlayer() || boxW <= 0 || boxH <= 0) return;
        Identifier texture = texture(instance.model.get());
        if (texture == null) return;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, boxX, boxY, 0f, 0f, boxW, boxH, 832, 1248, 832, 1248);
    }

    private static Identifier texture(String model) {
        String file = switch (model) {
            case "Tung Tung" -> "tung";
            case "Hana" -> "hana";
            case "Yuna" -> "yuna";
            case "Rin" -> "rin";
            default -> null;
        };
        if (file == null) return null;
        return Identifier.of("libbase", "textures/models/" + file + ".png");
    }
}
