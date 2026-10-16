package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * AutoUtil — AutoReconnect + AutoLog.
 *
 * AntiKick убран — он отправлял LookAndOnGround packet с yaw+0.01
 * каждые 400 тиков. На Matrix/Vulcan это палится как "unnatural rotation".
 * AntiAFK делает то же самое безопаснее (swingHand).
 */
public class AutoUtilModule extends CheatModule {
    public final BooleanSetting autoReconnect = add(new BooleanSetting("AutoReconnect", true));
    public final NumberSetting reconnectDelay = add(new NumberSetting("Reconnect delay", 5, 1, 30, 1));
    public final BooleanSetting autoLog = add(new BooleanSetting("AutoLog", false));
    public final NumberSetting logHealth = add(new NumberSetting("Log at HP", 6, 2, 20, 1));

    public AutoUtilModule() {
        super("AutoUtil", "Auto reconnect and auto log", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        MinecraftClient client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;

        // AutoLog — отключаемся при низком HP
        if (autoLog.get() && client.getNetworkHandler() != null
                && player.getHealth() <= logHealth.get()) {
            client.getNetworkHandler().getConnection().disconnect(Text.literal("AutoLog"));
        }
    }
}