package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class HitBoxesModule extends CheatModule {
    public final NumberSetting expand = add(new NumberSetting("Expand", 0.2, 0.0, 1.5, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFF));

    public HitBoxesModule() {
        super("HitBoxes", "Bigger hitbox for aiming and hitting", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static float extra() {
        HitBoxesModule boxes = com.example.bfb.ModuleManager.get(HitBoxesModule.class);
        if (boxes == null || !boxes.isEnabled()) return 0f;
        return boxes.expand.getFloat();
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        var client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;
        int argb = 0xFF000000 | color.getRgb();
        var area = client.player.getBoundingBox().expand(48);
        for (var entity : client.world.getOtherEntities(client.player, area)) {
            RenderUtil.outline(matrices, consumers, entity.getBoundingBox().expand(expand.get()), camera, argb, 1.25f);
        }
    }
}
