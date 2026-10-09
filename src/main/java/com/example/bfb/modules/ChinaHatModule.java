package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ChinaHatModule extends CheatModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", 0.45, 0.1, 1.2, 0.05));
    public final NumberSetting height = add(new NumberSetting("Height", 0.28, 0.05, 0.8, 0.02));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFF3B6B));
    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow speed", 1.0, 0.2, 4.0, 0.1));
    public final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    public final BooleanSetting self = add(new BooleanSetting("Self", false));
    public final BooleanSetting others = add(new BooleanSetting("Others", true));

    public ChinaHatModule() {
        super("ChinaHat", "Conical hat above players", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.world == null || client.player == null) return;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player && !self.get()) continue;
            if (player != client.player && !others.get()) continue;

            Vec3d pos = player.getLerpedPos(client.getRenderTickCounter().getTickProgress(false));
            double headY = pos.y + player.getHeight() + 0.1;

            Vec3d center = new Vec3d(pos.x, headY, pos.z);
            Vec3d tip = center.add(0, height.get(), 0);
            double r = radius.get();

            int rgb = rainbow.get() ? RenderUtil.rainbow(rainbowSpeed.get()) : color.getRgb();
            int outlineArgb = (220 << 24) | rgb;
            int fillArgb = (100 << 24) | rgb;

            int segments = 24;
            for (int i = 0; i < segments; i++) {
                double a1 = Math.PI * 2 * i / segments;
                double a2 = Math.PI * 2 * (i + 1) / segments;

                Vec3d p1 = center.add(Math.cos(a1) * r, 0, Math.sin(a1) * r);
                Vec3d p2 = center.add(Math.cos(a2) * r, 0, Math.sin(a2) * r);

                if (fill.get()) {
                    RenderUtil.triangle(matrices, consumers, camera, center, p1, p2, fillArgb);
                }

                RenderUtil.tracer(matrices, consumers, p1, p2, camera, outlineArgb, 1.5f);
                RenderUtil.tracer(matrices, consumers, p1, tip, camera, outlineArgb, 1.0f);
                RenderUtil.tracer(matrices, consumers, p2, tip, camera, outlineArgb, 1.0f);
            }
        }
    }
}