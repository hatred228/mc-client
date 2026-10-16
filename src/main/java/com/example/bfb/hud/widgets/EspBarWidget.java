package com.example.bfb.hud.widgets;

import com.example.bfb.ModuleManager;
import com.example.bfb.hud.AnimatedFloat;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.modules.EspModule;
import com.example.bfb.modules.KillAuraModule;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Просто HP-полоска сбоку. Ничего лишнего: ни имени, ни итема, ни цифр.
 * Ширина, высота, стиль — настраиваются в EspModule.
 */
public class EspBarWidget extends HudWidget {

    private final AnimatedFloat smoothHp = new AnimatedFloat(1f);
    private final AnimatedFloat smoothAlpha = new AnimatedFloat(0f);
    private LivingEntity lastTarget;
    private long lastFrame;

    public EspBarWidget() {
        super("EspBar", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null || mc.world == null) { width = height = 0; return; }

        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 1f / 60f
                : Math.min(.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;

        EspModule esp = ModuleManager.get(EspModule.class);
        boolean active = esp != null && esp.isEnabled() && esp.barEnabled.get();

        LivingEntity target = pickTarget();
        boolean inChat = mc.currentScreen instanceof ChatScreen;

        if (target != null) {
            lastTarget = target;
            smoothAlpha.setTarget(active ? 1f : 0f);
        } else if (lastTarget == null || lastTarget.isRemoved() || !lastTarget.isAlive()) {
            smoothAlpha.setTarget(0f);
        } else if (!inChat) {
            smoothAlpha.setTarget(0f);
        }

        float reveal = smoothAlpha.update(dt, 12f);
        if (reveal < 0.02f) { width = height = 0; return; }
        if (target == null) target = lastTarget;
        if (target == null) { width = height = 0; return; }

        float hpRatio = Math.max(0f, Math.min(1f, target.getHealth() / Math.max(1f, target.getMaxHealth())));
        smoothHp.setTarget(hpRatio);
        float hp = smoothHp.update(dt, 8f);

        int alpha = (int) (reveal * 255);

        // === размеры полоски ===
        float barW = s(80f) * esp.barWidth.getFloat();
        float barH = s(4f) * esp.barHeight.getFloat();

        // позиция — дефолт слева-центр, таскается в HUD редакторе
        float vw = ctx.getScaledWindowWidth();
        float vh = ctx.getScaledWindowHeight();
        if (!customized) {
            x = s(6f);
            y = vh * 0.5f - barH * 0.5f;
        }
        place(ctx, barW, barH);

        // фон
        ui.roundedRect(x, y, barW, barH, barH * 0.5f,
                HudTheme.withAlpha(0x80000000, alpha));

        // заполнение
        float fill = barW * hp;
        if (fill > 0.5f) {
            int col;
            switch (esp.barStyle.get()) {
                case "Gradient": {
                    // градиент по ширине от c1 к c2, но проще — health color
                    col = hp > 0.66f ? HudTheme.positive()
                            : hp > 0.33f ? HudTheme.middle()
                            : HudTheme.negative();
                    break;
                }
                case "Solid":
                default: {
                    col = hp > 0.66f ? HudTheme.positive()
                            : hp > 0.33f ? HudTheme.middle()
                            : HudTheme.negative();
                    break;
                }
            }
            ui.roundedRect(x, y, fill, barH, Math.min(barH * 0.5f, fill * 0.5f),
                    HudTheme.withAlpha(col, alpha));
        }

        width = barW;
        height = barH;

        if (target.isRemoved() || !target.isAlive()) smoothAlpha.setTarget(0f);
    }

    private LivingEntity pickTarget() {
        KillAuraModule aura = ModuleManager.get(KillAuraModule.class);
        if (aura != null && aura.isEnabled() && KillAuraModule.target instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity le && le.isAlive()) {
            return le;
        }
        return null;
    }
}