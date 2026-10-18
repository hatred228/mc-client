package com.example.bfb.hud.widgets;

import com.example.bfb.hud.HudCompactStyle;
import com.example.bfb.hud.HudTheme;
import com.example.bfb.hud.HudWidget;
import com.example.bfb.render.ui.UiDrawList;
import net.minecraft.client.MinecraftClient;

public class WatermarkWidget extends HudWidget {

    private float fps;
    private float ping;
    private long previousFrame;

    public WatermarkWidget() {
        super("Watermark", 0, 0);
    }

    @Override
    protected void draw(UiDrawList ui, float scale) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { width = height = 0; return; }

        long now = System.nanoTime();
        float dt = previousFrame == 0 ? 1f / 60f
                : Math.min(.05f, (now - previousFrame) / 1_000_000_000f);
        previousFrame = now;

        fps = HudCompactStyle.approach(fps == 0 ? mc.getCurrentFps() : fps,
                mc.getCurrentFps(), dt, 8);

        int realPing = 0;
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            realPing = entry == null ? 0 : entry.getLatency();
        }
        ping = HudCompactStyle.approach(ping, realPing, dt, 4);

        String user = mc.player.getName().getString();
        String fpsText = String.valueOf(Math.round(fps));
        String pingText = String.valueOf(Math.round(ping));

        float fontPx = 7f;
        float smallPx = 7f;
        float groupGap = s(6f);
        float padding = s(8f);
        float sepW = s(1f);

        // ФИКСИРОВАННАЯ ширина под 3 цифры — позиция не плавает
        float numW = textWidth("000", fontPx);
        float fpsSuffix = textWidth("fps", smallPx);
        float pingSuffix = textWidth("ms", smallPx);
        float userW = textWidth(user, fontPx);

        // центр числа внутри резервированной ширины
        float fpsNumW = textWidth(fpsText, fontPx);
        float pingNumW = textWidth(pingText, fontPx);
        float fpsPad = (numW - fpsNumW) * 0.5f;
        float pingPad = (numW - pingNumW) * 0.5f;

        float w = padding * 2
                + numW + s(2) + fpsSuffix
                + groupGap + sepW + groupGap
                + numW + s(2) + pingSuffix
                + groupGap + sepW + groupGap
                + userW;
        float h = s(14f);

        anchor(ctx, w, h, 0.5f, 0f);

        int xi = Math.round(x);
        int yi = Math.round(y);
        int wi = Math.round(w);
        int hi = Math.round(h);
        int radius = hi / 2;

        ui.roundedRect(xi, yi, wi, hi, radius, HudTheme.background());

        float textY = y + (h - 8f) * 0.5f;
        float cxf = x + padding;

        int textColor = HudTheme.text();
        int mutedColor = HudTheme.muted();
        int fpsColor = 0xFF9CC3E0;
        int pingColor = realPing < 80 ? 0xFF82D982 : realPing < 150 ? 0xFFE0C878 : 0xFFE08282;

        // fps
        ui.text(cxf + fpsPad, textY, fontPx, fpsColor, fpsText);
        cxf += numW + s(2);
        ui.text(cxf, textY + s(1f), smallPx, mutedColor, "fps");
        cxf += fpsSuffix + groupGap;
        ui.rect(Math.round(cxf), Math.round(y + h * 0.25f), 1, Math.round(h * 0.5f), 0x30FFFFFF);
        cxf += sepW + groupGap;

        // ping
        ui.text(cxf + pingPad, textY, fontPx, pingColor, pingText);
        cxf += numW + s(2);
        ui.text(cxf, textY + s(1f), smallPx, mutedColor, "ms");
        cxf += pingSuffix + groupGap;
        ui.rect(Math.round(cxf), Math.round(y + h * 0.25f), 1, Math.round(h * 0.5f), 0x30FFFFFF);
        cxf += sepW + groupGap;

        // user
        ui.text(cxf, textY, fontPx, textColor, user);

        width = w;
        height = h;
    }
}