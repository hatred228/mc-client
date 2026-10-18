package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.NotificationSystem;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ServerAlert — listens to chat and highlights events.
 *
 * Rules are stored in alerts.json inside config dir:
 *   (minecraft config)/undetected/alerts.json
 *   On Windows: %APPDATA%/.minecraft/config/undetected/alerts.json
 *
 * Format:
 *   [
 *     { "pattern": "...", "label": "...", "color": "FF5CA8", "sound": true }
 *   ]
 *
 * Groups () in pattern are substituted into label via $1, $2.
 */
public class ServerAlertModule extends CheatModule {
    public final BooleanSetting playSound = add(new BooleanSetting("Sound", true));
    public final BooleanSetting chatFeedback = add(new BooleanSetting("Chat feedback", false));
    public final NumberSetting cooldownTicks = add(new NumberSetting("Cooldown (ticks)", 40, 10, 200, 5));
    public final ColorSetting defaultColor = add(new ColorSetting("Default color", 0xFF5CA8));

    private static ServerAlertModule instance;
    private final List<Rule> rules = new ArrayList<>();
    private final List<Long> recentTriggers = new ArrayList<>();

    public ServerAlertModule() {
        super("ServerAlert", "Alerts on custom chat events", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    public static ServerAlertModule instance() { return instance; }

    public record Rule(Pattern pattern, String label, int color, boolean sound) {}

    @Override
    public void onEnable() {
        loadRules();
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        recentTriggers.removeIf(t -> now - t > 5000);
    }

    /** Called from ChatHudMixin for each incoming chat message. */
    public void onChatMessage(String text) {
        if (!isEnabled() || text == null || text.isEmpty()) return;
        if (rules.isEmpty()) return;

        for (Rule rule : rules) {
            var matcher = rule.pattern().matcher(text);
            if (!matcher.find()) continue;

            long now = System.currentTimeMillis();
            if (recentTriggers.size() >= 3
                    && now - recentTriggers.get(recentTriggers.size() - 1) < cooldownTicks.getInt() * 50L) {
                continue;
            }
            recentTriggers.add(now);

            String label = rule.label();
            for (int g = 1; g <= matcher.groupCount() && g <= 9; g++) {
                String val = matcher.group(g);
                if (val != null) label = label.replace("$" + g, val);
            }

            com.example.bfb.hud.Notifications.push(label, rule.color());

            if (playSound.get() && rule.sound()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.7f, 1.8f);
                }
            }
            break;
        }
    }

    private void loadRules() {
        rules.clear();
        try {
            var dir = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getConfigDir().resolve("undetected");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path file = dir.resolve("alerts.json");

            if (!java.nio.file.Files.exists(file)) {
                String example = """
                        [
                          {
                            "pattern": "runa (.+) was moved",
                            "label": "Runa: $1",
                            "color": "FF5CA8",
                            "sound": true
                          },
                          {
                            "pattern": "event[!: ]+(.+)",
                            "label": "Event: $1",
                            "color": "FFCC44",
                            "sound": true
                          },
                          {
                            "pattern": "(.+) joined the game",
                            "label": "+ $1",
                            "color": "55FF55",
                            "sound": false
                          }
                        ]
                        """;
                java.nio.file.Files.writeString(file, example);
            }

            String json = java.nio.file.Files.readString(file);
            var arr = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
            for (var el : arr) {
                var obj = el.getAsJsonObject();
                String pat = obj.get("pattern").getAsString();
                String lbl = obj.has("label") ? obj.get("label").getAsString() : pat;
                String col = obj.has("color") ? obj.get("color").getAsString() : "FF5CA8";
                boolean snd = !obj.has("sound") || obj.get("sound").getAsBoolean();
                int color = Integer.parseInt(col.replace("#", ""), 16) & 0xFFFFFF;
                rules.add(new Rule(Pattern.compile(pat), lbl, color, snd));
            }
        } catch (Exception e) {
            System.err.println("[BFB] ServerAlert: failed to load rules: " + e.getMessage());
        }
    }
}