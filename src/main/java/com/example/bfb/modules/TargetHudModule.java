package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.LabelFont;
import com.example.bfb.Loc;
import com.example.bfb.gui.GuiDraw;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class TargetHudModule extends CheatModule {
    public final NumberSetting range = add(new NumberSetting("Range", 8.0, 3.0, 20.0, 0.5));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.1, 0.7, 1.8, 0.05));
    public final BooleanSetting showHealth = add(new BooleanSetting("Show Health", true));
    public final BooleanSetting showHead = add(new BooleanSetting("Show Head", true));
    public final BooleanSetting showDistance = add(new BooleanSetting("Show Distance", true));
    public final BooleanSetting showAbsorption = add(new BooleanSetting("Show Absorption", true));
    public final BooleanSetting smoothHealth = add(new BooleanSetting("Smooth Health", true));
    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    public final NumberSetting rounding = add(new NumberSetting("Rounding", 6, 0, 12, 1));
    public final NumberSetting edgeWidth = add(new NumberSetting("Edge width", 2, 0, 4, 1));
    public final ColorSetting edgeColor1 = add(new ColorSetting("Edge color 1", 0x9C5CFF));
    public final ColorSetting edgeColor2 = add(new ColorSetting("Edge color 2", 0xFF5CA8));
    public final ColorSetting bgColor = add(new ColorSetting("Background", 0xE0100A14));
    public final ColorSetting nameColor = add(new ColorSetting("Name color", 0xFFFFFF));
    public final ColorSetting hpHighColor = add(new ColorSetting("HP high", 0x22FF44));
    public final ColorSetting hpMidColor = add(new ColorSetting("HP mid", 0xFFFFA500));
    public final ColorSetting hpLowColor = add(new ColorSetting("HP low", 0xFFFF2222));
    public final ColorSetting absColor = add(new ColorSetting("Absorption", 0xFFFFDD44));

    private LivingEntity target;
    private int displayTicks;
    private float appearProgress = 0f;
    private float displayedRatio = 1f;
    private float displayedAbsorption = 0f;

    public TargetHudModule() {
        super("TargetHUD", "Celestial-style target info", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        if (client.player == null || client.world == null) return;

        HitResult hit = client.crosshairTarget;
        LivingEntity newTarget = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            if (living.squaredDistanceTo(client.player) <= range.get() * range.get()) newTarget = living;
        }
        if (newTarget == null && KillAuraModule.target instanceof LivingEntity living) {
            if (living.squaredDistanceTo(client.player) <= range.get() * range.get()) newTarget = living;
        }

        if (newTarget != null) {
            if (target != newTarget) {
                displayedRatio = newTarget.getMaxHealth() > 0
                        ? newTarget.getHealth() / newTarget.getMaxHealth() : 1f;
                displayedAbsorption = newTarget.getAbsorptionAmount() / Math.max(1f, newTarget.getMaxHealth());
            }
            target = newTarget;
            displayTicks = 40;
            appearProgress = Math.min(1f, appearProgress + 0.10f);
        } else if (displayTicks > 0) {
            displayTicks--;
        } else {
            target = null;
            appearProgress = Math.max(0f, appearProgress - 0.06f);
        }

        if (target != null) {
            float actual = target.getMaxHealth() > 0
                    ? Math.max(0f, Math.min(1f, target.getHealth() / target.getMaxHealth())) : 1f;
            displayedRatio = lerp(displayedRatio, actual, 0.12f);
            float actualAbs = target.getAbsorptionAmount() / Math.max(1f, target.getMaxHealth());
            displayedAbsorption = lerp(displayedAbsorption, actualAbs, 0.12f);
        }
    }

    public static void draw(DrawContext context, MinecraftClient client, TargetHudModule mod) {
        if (mod == null || !mod.isEnabled() || mod.target == null || mod.appearProgress < 0.01f) return;

        LivingEntity entity = mod.target;
        String name = entity instanceof PlayerEntity pe ? pe.getGameProfile().name() : Loc.entityName(entity);
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float ratio = maxHealth > 0 ? Math.max(0f, Math.min(1f, health / maxHealth)) : 1f;
        float smoothRatio = mod.smoothHealth.get() ? mod.displayedRatio : ratio;
        float absorption = entity.getAbsorptionAmount();
        float absRatio = mod.smoothHealth.get() ? mod.displayedAbsorption : absorption / Math.max(1f, maxHealth);

        // layout
        int pad = 7;
        int headSize = 30;
        int lineH = LabelFont.height();
        int lineGap = 1;

        String hpText = String.format("%.1f", health);
        String absText = (absorption > 0.1f && mod.showAbsorption.get())
                ? " +" + String.format("%.1f", absorption) : "";
        String distText = mod.showDistance.get()
                ? String.format("%.0fm", Math.sqrt(entity.squaredDistanceTo(client.player))) : "";

        int hpW = LabelFont.width(hpText);
        int absW = LabelFont.width(absText);
        int nameW = LabelFont.width(name);
        int distW = LabelFont.width(distText);

        // правый блок (имя сверху, строка с hp/abs/дист снизу)
        int secondRowW = hpW + absW + (distText.isEmpty() ? 0 : (distW + 8));
        int textW = Math.max(nameW, secondRowW) + 4;

        int boxW = pad + headSize + pad + textW + pad;
        int boxH = pad + headSize + pad;

        float scale = mod.scale.getFloat();
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        int defaultX = sw - (int) (boxW * scale) - 16;
        int defaultY = sh / 2 - (int) (boxH * scale) / 2;

        int targetX = HudEditor.drawX("targethud", defaultX, defaultY);
        int targetY = HudEditor.drawY("targethud", defaultX, defaultY);

        // плавное появление: slide справа + fade
        float ease = 1f - (float) Math.pow(1f - mod.appearProgress, 3);
        int slideX = (int) ((1f - ease) * 30);
        int slideY = (int) ((1f - ease) * 4);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(targetX + slideX, targetY + slideY);
        matrices.scale(scale, scale);

        int r = mod.rounding.getInt();
        int edgeW = mod.edgeWidth.getInt();
        int c1 = mod.edgeColor1.getRgb() & 0xFFFFFF;
        int c2 = mod.edgeColor2.getRgb() & 0xFFFFFF;
        int bg = mod.bgColor.getRgb();

        // shadow
        if (mod.shadow.get()) GuiDraw.shadow(context, 0, 0, boxW, boxH, r);

        // background
        GuiDraw.round(context, 0, 0, boxW, boxH, r, bg);

        // верхняя градиентная полоса (edge)
        if (edgeW > 0) {
            GuiDraw.topEdge(context, 0, 0, boxW, edgeW, r, c1, c2);
        }

        // --- player head ---
        int contentX = pad;
        if (mod.showHead.get() && entity instanceof AbstractClientPlayerEntity acp) {
            SkinTextures skin = DefaultSkinHelper.getSkinTextures(acp.getGameProfile());
            int headX = contentX;
            int headY = pad;
            GuiDraw.round(context, headX - 1, headY - 1, headSize + 2, headSize + 2, 4, 0xFF1A0E1E);
            PlayerSkinDrawer.draw(context, skin, headX, headY, headSize, 0xFFFFFFFF);
            contentX += headSize + pad;
        }

        int textX = contentX;
        int textTopY = pad + 1;

        // name
        LabelFont.draw(context, name, textX, textTopY, 0xFF000000 | (mod.nameColor.getRgb() & 0xFFFFFF));

        // second row: hp [+abs] + distance right-aligned
        int secondY = textTopY + lineH + lineGap;
        if (mod.showHealth.get()) {
            int hpColor = healthColorFull(smoothRatio, mod);
            LabelFont.draw(context, hpText, textX, secondY, 0xFF000000 | hpColor);
            if (!absText.isEmpty()) {
                int absColor = mod.absColor.getRgb() & 0xFFFFFF;
                LabelFont.draw(context, absText, textX + hpW, secondY, 0xFF000000 | absColor);
            }
        }
        if (!distText.isEmpty()) {
            int dx = boxW - pad - distW;
            LabelFont.draw(context, distText, dx, secondY, 0xFF9099B0);
        }

        // --- hp bar снизу ---
        if (mod.showHealth.get()) {
            int barX = pad;
            int barY = boxH - pad + 1;
            int barW = boxW - pad * 2;
            int barH = 3;

            GuiDraw.round(context, barX, barY, barW, barH, 1, 0xFF1A0E1E);

            int fillW = (int) (barW * smoothRatio);
            if (fillW > 0) {
                int leftColor, rightColor;
                if (smoothRatio > 0.66f) {
                    leftColor = mod.hpHighColor.getRgb() & 0xFFFFFF;
                    rightColor = mod.hpMidColor.getRgb() & 0xFFFFFF;
                } else if (smoothRatio > 0.33f) {
                    leftColor = mod.hpMidColor.getRgb() & 0xFFFFFF;
                    rightColor = leftColor;
                } else {
                    leftColor = mod.hpLowColor.getRgb() & 0xFFFFFF;
                    rightColor = leftColor;
                }
                context.fillGradient(barX, barY, barX + fillW, barY + barH,
                        0xFF000000 | leftColor, 0xFF000000 | rightColor);
                context.fill(barX, barY, barX + fillW, barY + 1, 0x40FFFFFF);
            }

            // absorption второй слой
            if (mod.showAbsorption.get() && absRatio > 0.01f) {
                int absStart = barX + fillW;
                int absEnd = barX + (int) (barW * Math.min(1f, smoothRatio + absRatio));
                if (absEnd > absStart) {
                    int absColor = mod.absColor.getRgb() & 0xFFFFFF;
                    context.fillGradient(absStart, barY, absEnd, barY + barH,
                            0xFF000000 | absColor, 0xFFCC8800);
                }
            }
        }

        matrices.popMatrix();

        HudEditor.outline(context, "targethud", targetX, targetY, (int) (boxW * scale), (int) (boxH * scale));
    }

    private static int healthColorFull(float ratio, TargetHudModule mod) {
        int low = mod.hpLowColor.getRgb() & 0xFFFFFF;
        int mid = mod.hpMidColor.getRgb() & 0xFFFFFF;
        int high = mod.hpHighColor.getRgb() & 0xFFFFFF;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2f;
            return lerpColor(mid, high, t);
        } else {
            float t = ratio * 2f;
            return lerpColor(low, mid, t);
        }
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (((int) (ar + (br - ar) * t)) << 16)
                | (((int) (ag + (bg - ag) * t)) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}