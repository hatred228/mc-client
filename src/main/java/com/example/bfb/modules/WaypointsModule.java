package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.SalFont;
import com.example.bfb.setting.ColorSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WaypointsModule extends CheatModule {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFD54A));
    private static final List<Vec3d> points = new ArrayList<>();
    private static final List<String> names = new ArrayList<>();

    public WaypointsModule() {
        super("Waypoints", "Keybind: add waypoint at your position", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        var p = BfbMod.getClient().player;
        if (p == null) return;
        Vec3d pos = new Vec3d(p.getX(), p.getY(), p.getZ());
        int i = nearest(pos);
        if (i >= 0 && points.get(i).distanceTo(pos) < 5) {
            points.remove(i); names.remove(i);
            p.sendMessage(Text.literal("§e[Waypoints] removed"), true);
        } else {
            points.add(pos);
            names.add((int) p.getX() + ", " + (int) p.getY() + ", " + (int) p.getZ());
            p.sendMessage(Text.literal("§a[Waypoints] added"), true);
        }
        applyEnabled(false);
    }

    private static int nearest(Vec3d pos) {
        int best = -1; double bd = 6;
        for (int i = 0; i < points.size(); i++) {
            double d = points.get(i).distanceTo(pos);
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        int argb = color.argb(200);
        for (Vec3d p : points) {
            RenderUtil.bone(matrices, consumers, camera, p, p.add(0, 60, 0), argb, 2.0f);
            RenderUtil.bone(matrices, consumers, camera, p.add(0, 60, 0), p, argb, 2.0f);
        }
    }

    public static void drawText(DrawContext context, MinecraftClient client, float tickDelta) {
        var player = client.player;
        if (player == null || points.isEmpty()) return;
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        for (int i = 0; i < points.size(); i++) {
            Vec3d p = points.get(i);
            Vec3d screen = RenderUtil.toScreen(p.add(0, 2, 0));
            if (screen == null) continue;
            String label = names.get(i) + " [" + (int) p.distanceTo(playerPos) + "m]";
            int w = SalFont.width(label);
            context.fill((int) screen.x - w / 2 - 3, (int) screen.y - 5,
                    (int) screen.x + w / 2 + 3, (int) screen.y + SalFont.height() + 2, 0x9008080C);
            SalFont.draw(context, label, (int) screen.x - w / 2, (int) screen.y - 2, 0xFFD54A);
        }
    }
}