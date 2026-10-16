package com.example.bfb.hud.widgets;

import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.hud.Notifications;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotionsWidget extends HudWidget {

    private final HudCompactStyle.Options options;
    private static final Map<Identifier, Boolean> WARNED = new HashMap<>();

    public PotionsWidget(HudCompactStyle.Options options) {
        super("Potions", 0, 0);
        this.options = options;
    }

    @Override
    public HudCompactStyle.Options options() { return options; }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null) { width = height = 0; return; }

        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
        boolean inChat = mc.currentScreen instanceof ChatScreen;

        // превью в чате — даже без эффектов
        if (effects.isEmpty() && !inChat) {
            WARNED.clear();
            width = height = 0;
            return;
        }

        float pad = s(5f);
        float rowH = s(12f);
        float headerH = options.show("Header") ? s(14f) : 0;
        float iconSize = s(9f);
        float iconGap = s(3f);
        float w = s(96f);
        float h = headerH + pad * 0.7f + Math.max(effects.size(), 1) * rowH + pad * 0.5f;

        anchor(ctx, w, h, 0f, 0.05f);

        HudCompactStyle.panel(ui, x, y, w, h, scale, 220);

        if (options.show("Header")) {
            String title = "Potions";
            float tw = textWidth(title, 8f);
            ui.text(x + (w - tw) * 0.5f, y + s(3f), 8f, HudTheme.text(), title);

            ui.rect(Math.round(x + s(0.5f)), Math.round(y + s(14f)),
                    Math.round(w - s(1f)), 1, 0x18FFFFFF);
        }

        float ry = y + headerH + pad * 0.7f;
        float nameMaxW = w - pad * 2 - iconSize - iconGap - s(30f);

        if (effects.isEmpty()) {
            ui.text(x + pad, ry + s(2f), 7f, HudTheme.muted(), "No effects");
            width = w;
            height = h;
            return;
        }

        for (StatusEffectInstance eff : effects) {
            RegistryEntry<StatusEffect> type = eff.getEffectType();
            String name = type.value().getName().getString();
            String fitName = fit(name, nameMaxW);

            int amp = eff.getAmplifier() + 1;
            String level = amp > 1 ? roman(amp) : "";
            String dur = formatDuration(eff.getDuration());

            Identifier id = Registries.STATUS_EFFECT.getId(type.value());
            int secs = eff.getDuration() / 20;
            if (id != null) {
                if (secs < 30 && secs > 0) {
                    if (!Boolean.TRUE.equals(WARNED.get(id))) {
                        Notifications.push(name + " ending soon", 0xFFFFC85F);
                        WARNED.put(id, true);
                    }
                } else if (secs > 35) {
                    WARNED.put(id, false);
                }
            }

            // ИКОНКА ЭФФЕКТА — реальная vanilla mob_effect текстура
            if (id != null) {
                Identifier sprite = Identifier.of(id.getNamespace(),
                        "mob_effect/" + id.getPath());
                int ix = Math.round(x + pad);
                int iy = Math.round(ry + s(1.5f));
                int is = Math.round(iconSize);
                try {
                    ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite,
                            ix, iy, is, is, 0xFFFFFFFF);
                } catch (Throwable ignored) {
                    // fallback если sprite не найден
                    ui.circle(ix + is * 0.5f, iy + is * 0.5f, is * 0.4f,
                            HudTheme.accent());
                }
            }

            float tx = x + pad + iconSize + iconGap;
            ui.text(tx, ry + s(2f), 7f, HudTheme.text(), fitName);

            if (!level.isEmpty()) {
                ui.text(tx + textWidth(fitName, 7f) + s(2f),
                        ry + s(2f), 7f, HudTheme.muted(), level);
            }

            float durW = textWidth(dur, 7f);
            ui.text(x + w - pad - durW, ry + s(2f), 7f, HudTheme.muted(), dur);

            ry += rowH;
        }

        width = w;
        height = h;
    }

    private static String formatDuration(int ticks) {
        int secs = ticks / 20;
        if (secs >= 60) return (secs / 60) + "m" + (secs % 60 > 0 ? (secs % 60) + "s" : "");
        return secs + "s";
    }

    private static String roman(int n) {
        return switch (n) {
            case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}