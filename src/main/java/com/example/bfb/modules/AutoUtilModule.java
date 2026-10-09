package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoUtilModule extends CheatModule {
    public final BooleanSetting autoReconnect = add(new BooleanSetting("AutoReconnect", true));
    public final NumberSetting reconnectDelay = add(new NumberSetting("Reconnect delay", 5, 1, 30, 1));
    public final BooleanSetting autoLog = add(new BooleanSetting("AutoLog", false));
    public final NumberSetting logHealth = add(new NumberSetting("Log at HP", 6, 2, 20, 1));
    public final BooleanSetting antiKick = add(new BooleanSetting("AntiKick", true));

    private int kickTicks;

    public AutoUtilModule() {
        super("AutoUtil", "Auto reconnect, auto log, anti-kick", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;

        if (autoLog.get() && client.getNetworkHandler() != null
                && player.getHealth() <= logHealth.get()) {
            client.getNetworkHandler().getConnection().disconnect(Text.literal("AutoLog"));
            return;
        }

        if (antiKick.get() && ++kickTicks >= 400) {
            kickTicks = 0;
            // 1.21.11: sneak через PlayerInput — просто поворачиваем камеру на 0.01°
            // (сервер видит активность, не палится как анти-афк-чит)
            float yaw = player.getYaw() + 0.01f;
            player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(
                    yaw, player.getPitch(), player.isOnGround(), player.horizontalCollision));
        }
    }
}