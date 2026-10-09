package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.HudStyle;
import com.example.bfb.RenderUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrailsModule extends CheatModule {
    public final NumberSetting length = add(new NumberSetting("Length", 14, 3, 40, 1));
    public final NumberSetting spacing = add(new NumberSetting("Spacing", 0.35, 0.1, 1.5, 0.05));
    public final NumberSetting alpha = add(new NumberSetting("Alpha", 220, 20, 255, 5));
    public final NumberSetting lineWidth = add(new NumberSetting("Width", 3.5, 1.0, 8.0, 0.5));
    public final BooleanSetting taper = add(new BooleanSetting("Width taper", true));
    public final BooleanSetting glowLayer = add(new BooleanSetting("Glow layer", true));
    public final ColorSetting color1 = add(new ColorSetting("Color 1", 0x9C5CFF));
    public final ColorSetting color2 = add(new ColorSetting("Color 2", 0xFF5CA8));
    public final BooleanSetting gradient = add(new BooleanSetting("Gradient", true));
    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow speed", 1.0, 0.2, 4.0, 0.1));
    public final NumberSetting range = add(new NumberSetting("Range", 32, 8, 64, 1));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    private static final int MAX_ENTITIES = 8;
    private static final Map<Integer, ArrayDeque<Vec3d>> POSITIONS = new HashMap<>();

    public TrailsModule() {
        super("Trails", "Comet-like trail behind entities", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        POSITIONS.clear();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;
        int maxLen = length.getInt();
        double spacingSq = spacing.get() * spacing.get();
        double reachSq = range.get() * range.get();

        Set<Integer> alive = new HashSet<>();
        int count = 0;
        for (LivingEntity entity : com.example.bfb.EntityCache.living()) {
            if (count >= MAX_ENTITIES) break;
            if (!CombatUtil.accept(entity, client.player, players.get(), mobs.get(), animals.get())) continue;
            if (entity.squaredDistanceTo(client.player) > reachSq) continue;
            count++;
            alive.add(entity.getId());
            ArrayDeque<Vec3d> deque = POSITIONS.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>());
            Vec3d curr = entity.getEntityPos();
            if (deque.isEmpty() || deque.peekLast().squaredDistanceTo(curr) >= spacingSq) {
                deque.addLast(curr);
                while (deque.size() > maxLen) deque.removeFirst();
            }
        }
        POSITIONS.keySet().removeIf(id -> !alive.contains(id));
    }

    @Override
    public void onWorldRender(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        int baseAlpha = alpha.getInt();
        boolean useGradient = gradient.get();
        int c1 = color1.getRgb() & 0xFFFFFF;
        int c2 = color2.getRgb() & 0xFFFFFF;
        float baseW = lineWidth.getFloat();

        for (var entry : POSITIONS.entrySet()) {
            ArrayDeque<Vec3d> deque = entry.getValue();
            int total = deque.size();
            if (total < 2) continue;

            Vec3d[] arr = deque.toArray(new Vec3d[0]);

            for (int i = 0; i < total - 1; i++) {
                Vec3d a = arr[i];
                Vec3d b = arr[i + 1];

                float tA = i / (float) (total - 1);
                float tB = (i + 1) / (float) (total - 1);

                int lineColorA = pickColor(useGradient, c1, c2, tA);
                int lineColorB = pickColor(useGradient, c1, c2, tB);

                int alphaA = (int) (baseAlpha * tA * tA);
                int alphaB = (int) (baseAlpha * tB * tB);
                if (alphaA < 5 && alphaB < 5) continue;

                float widthA = taper.get() ? baseW * (0.25f + tA * 0.75f) : baseW;
                float widthB = taper.get() ? baseW * (0.25f + tB * 0.75f) : baseW;

                // glow layer — толще, полупрозрачнее
                if (glowLayer.get()) {
                    int glowAlphaA = alphaA / 4;
                    int glowAlphaB = alphaB / 4;
                    RenderUtil.tracer(matrices, consumers, a, b, camera,
                            (glowAlphaA << 24) | lineColorA, (widthA + widthB) * 0.5f * 2.5f);
                }

                // core layer
                RenderUtil.tracer(matrices, consumers, a, b, camera,
                        (alphaA << 24) | lineColorA, widthA);
            }
        }
    }

    private int pickColor(boolean gradient, int c1, int c2, float t) {
        if (rainbow.get()) return RenderUtil.rainbow(rainbowSpeed.get());
        if (gradient) return HudStyle.lerpColor(c2, c1, t);
        return c1;
    }
}