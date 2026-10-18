package com.example.bfb.hud.widgets;

import com.example.bfb.CheatModule;
import com.example.bfb.ModuleManager;
import com.example.bfb.hud.HudAnimation;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.modules.VisualsModule;
import com.example.bfb.render.ui.UiDrawList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArraylistWidget extends HudWidget {

    private final Map<String, Row> rows = new LinkedHashMap<>();

    private static final class Row {
        final HudAnimation anim = new HudAnimation();
        boolean active;
        int color;
    }

    public ArraylistWidget() {
        super("Arraylist", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        // === проверяем Enabled в VisualsModule.arraylist ===
        VisualsModule vm = VisualsModule.get();
        if (vm != null && !vm.arraylist.bool("Enabled")) {
            // если выключено — плавно убираем все строки и выходим
            for (Row row : rows.values()) {
                row.active = false;
                row.anim.run(0, 180);
                row.anim.update();
            }
            rows.entrySet().removeIf(e -> e.getValue().anim.value() < 0.01f);
            width = height = 0;
            return;
        }

        // собираем включённые модули
        List<CheatModule> enabled = new ArrayList<>();
        for (CheatModule m : ModuleManager.getModules()) {
            if (!m.isEnabled()) continue;
            String n = m.getName();
            // исключаем сам arraylist и служебные
            if (n.equals("Arraylist") || n.equals("ArrayList")
                    || n.equals("Interface") || n.equals("Client") || n.equals("Theme")) continue;
            enabled.add(m);
        }

        // сортировка по ширине текста (широкие сверху)
        enabled.sort(Comparator.comparingInt(
                (CheatModule m) -> -Math.round(textWidth(m.getName(), 7f))));

        for (Row row : rows.values()) row.active = false;

        for (CheatModule m : enabled) {
            Row row = rows.computeIfAbsent(m.getName(), k -> new Row());
            row.color = m.getCategory().getColor() | 0xFF000000;
            row.active = true;
        }

        for (Row row : rows.values()) {
            row.anim.run(row.active ? 1 : 0, row.active ? 180 : 220);
            row.anim.update();
        }
        rows.entrySet().removeIf(e -> !e.getValue().active && e.getValue().anim.value() < 0.01f);

        if (rows.isEmpty()) { width = height = 0; return; }

        float lineH = s(10f);
        float pad = s(4f);
        float barW = s(1.5f);
        float vw = ctx.getScaledWindowWidth();
        float rightEdge = vw - s(3f);

        float totalH = 0;
        for (Row r : rows.values()) totalH += lineH * r.anim.value();

        if (!customized) y = s(4f);
        x = 0;
        width = vw;
        height = totalH;

        boolean showAccent = vm == null || vm.arraylist.bool("Accent bar");

        float ry = y;
        for (Map.Entry<String, Row> e : rows.entrySet()) {
            Row row = e.getValue();
            float reveal = row.anim.value();
            if (reveal <= 0.01f) continue;
            int alpha = Math.round(reveal * 240);

            String name = e.getKey();
            float textW = textWidth(name, 7f);
            float rowW = pad * 2 + textW + (showAccent ? barW : 0);
            float rowX = rightEdge - rowW;
            float slideX = (1f - reveal) * s(15f);

            if (showAccent) {
                ui.rect(rowX + rowW - barW + slideX,
                        ry + s(1f),
                        barW,
                        lineH * reveal - s(2f),
                        HudTheme.withAlpha(row.color, alpha));
            }

            ui.text(rowX + pad + slideX,
                    ry + s(1.5f),
                    7f,
                    HudTheme.withAlpha(row.color, alpha),
                    name);

            ry += lineH * reveal;
        }
    }
}