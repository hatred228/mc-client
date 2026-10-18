package com.example.bfb.hud.widgets;

import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.modules.VisualsModule;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StaffDetectorWidget extends HudWidget {

    public StaffDetectorWidget() {
        super("StaffDetector", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        if (mc.player == null || mc.getNetworkHandler() == null) { width = height = 0; return; }

        VisualsModule vm = VisualsModule.get();
        if (vm != null && !vm.staff.bool("Enabled")) { width = height = 0; return; }

        boolean showEmpty = vm != null && vm.staff.bool("Show when empty");
        boolean inChat = mc.currentScreen instanceof ChatScreen;
        List<PlayerListEntry> staff = findStaff();

        boolean show = !staff.isEmpty() || showEmpty || inChat;
        if (!show) { width = height = 0; return; }

        boolean showHeader = vm == null || vm.staff.bool("Header");
        boolean showMode = vm == null || vm.staff.bool("Show mode");
        String position = vm != null ? vm.staff.mode("Position") : "TopLeft";

        float pad = s(5f);
        float rowH = s(11f);
        float headerH = showHeader ? s(14f) : 0f;
        int rows = Math.max(1, staff.size());
        float w = s(105f);
        float h = headerH + pad + rows * rowH + pad * 0.5f;

        float anchorX = position.endsWith("Right") ? 1f : 0f;
        float anchorY = position.startsWith("Bottom") ? 1f : 0f;
        anchor(ctx, w, h, anchorX, anchorY);
        if (!customized) {
            if (position.startsWith("Bottom")) y -= s(40f);
            else y += s(40f);
        }

        HudCompactStyle.panel(ui, x, y, w, h, scale, 240);

        if (showHeader) {
            String title = "Staff (" + staff.size() + ")";
            float tw = textWidth(title, 8f);
            ui.text(x + (w - tw) * 0.5f, y + s(3f), 8f, HudTheme.text(), title);
            ui.rect(x + s(0.5f), y + s(14f), w - s(1f),
                    Math.max(0.5f, s(0.3f)), HudTheme.border());
        }

        float ry = y + headerH + pad * 0.6f;

        if (staff.isEmpty()) {
            ui.text(x + pad, ry + s(1.5f), 7f, HudTheme.muted(), "No staff online");
            ry += rowH;
        } else {
            for (PlayerListEntry entry : staff) {
                String name = entry.getProfile().name();
                if (name == null) name = "?";

                GameMode gm = entry.getGameMode();
                boolean spectator = gm == GameMode.SPECTATOR;
                int nameColor = spectator ? 0xFFFF5050 : 0xFFFFC85F;

                ui.rect(x + pad - s(2f), ry + s(3f), s(2f), s(2f), nameColor);

                float nameMaxW = w - pad * 2 - (showMode ? s(30f) : 0f);
                String shown = fit(name, nameMaxW);
                ui.text(x + pad, ry + s(1.5f), 7f, nameColor, shown);

                if (showMode && gm != null) {
                    String modeStr = modeName(gm);
                    float mw = textWidth(modeStr, 7f);
                    ui.text(x + w - pad - mw, ry + s(1.5f), 7f, HudTheme.muted(), modeStr);
                }
                ry += rowH;
            }
        }

        width = w;
        height = h;
    }

    private List<PlayerListEntry> findStaff() {
        List<PlayerListEntry> out = new ArrayList<>();
        var handler = mc.getNetworkHandler();
        if (handler == null) return out;

        VisualsModule vm = VisualsModule.get();
        boolean checkSpectators = vm == null || vm.staff.bool("Spectators");
        boolean checkRoles = vm == null || vm.staff.bool("Role prefixes");
        boolean includeSelf = vm != null && vm.staff.bool("Include self");

        String myName = mc.player.getGameProfile().name();
        String[] keywords = {
                "admin", "mod", "moder", "staff", "owner", "helper",
                "curator", "operator", "[op]", "op ",
                "куратор", "модератор", "админ", "стафф", "хелпер",
                "[mod", "[admin", "[owner", "[staff", "[helper", "[curator"
        };

        for (PlayerListEntry entry : handler.getPlayerList()) {
            String name = entry.getProfile().name();
            if (name == null) continue;
            if (!includeSelf && name.equalsIgnoreCase(myName)) continue;

            if (checkSpectators && entry.getGameMode() == GameMode.SPECTATOR) {
                out.add(entry);
                continue;
            }

            if (checkRoles) {
                String display = entry.getDisplayName() != null
                        ? entry.getDisplayName().getString().toLowerCase(Locale.ROOT)
                        : "";
                String full = display + " " + name.toLowerCase(Locale.ROOT);
                for (String kw : keywords) {
                    if (full.contains(kw)) { out.add(entry); break; }
                }
            }
        }
        return out;
    }

    private static String modeName(GameMode gm) {
        return switch (gm) {
            case SPECTATOR -> "spec";
            case CREATIVE -> "creative";
            case ADVENTURE -> "adventure";
            case SURVIVAL -> "survival";
            default -> gm.name().toLowerCase(Locale.ROOT);
        };
    }
}