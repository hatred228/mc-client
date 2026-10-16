package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class ChatUtilsModule extends CheatModule {
    public final ModeSetting suffix = add(new ModeSetting("Suffix", "None", "None", "BFB", "Undetected", "Custom"));
    public final BooleanSetting antiSpam = add(new BooleanSetting("AntiSpam", true));
    public final NumberSetting antiSpamWindow = add(new NumberSetting("Spam window (s)", 5, 1, 30, 1));
    public final NumberSetting antiSpamRepeat = add(new NumberSetting("Repeat count", 2, 2, 10, 1));
    public final BooleanSetting antiAd = add(new BooleanSetting("AntiAd", true));
    public final BooleanSetting antiAdLinks = add(new BooleanSetting("AntiAd links", true));
    public final BooleanSetting antiAdDiscord = add(new BooleanSetting("AntiAd discord", true));
    public final BooleanSetting antiAdServers = add(new BooleanSetting("AntiAd server IPs", true));
    public final BooleanSetting antiAdKeyWords = add(new BooleanSetting("AntiAd keywords", true));

    public final BooleanSetting nameProtect = add(new BooleanSetting("NameProtect", false));
    public final BooleanSetting nameProtectSelf = add(new BooleanSetting("Protect self", true));
    public final BooleanSetting nameProtectFriends = add(new BooleanSetting("Protect friends", true));
    public final BooleanSetting nameProtectInChat = add(new BooleanSetting("In chat", true));
    public final BooleanSetting nameProtectInNametags = add(new BooleanSetting("In nametags", true));

    public final BooleanSetting antiReport = add(new BooleanSetting("AntiReport", true));
    public final BooleanSetting antiReportScreens = add(new BooleanSetting("Block screens", true));
    public final BooleanSetting antiReportCommands = add(new BooleanSetting("Block /report cmd", true));

    public final BooleanSetting autoTip = add(new BooleanSetting("AutoTip", false));
    public final NumberSetting autoTipInterval = add(new NumberSetting("AutoTip interval (s)", 60, 20, 300, 10));

    private static ChatUtilsModule instance;

    private final Deque<Long> _spamTimes = new ArrayDeque<>();
    private String _lastMessage = "";
    private long _lastMessageTime = 0L;

    private int _autoTipTimer = 0;

    // защитные имена (что подставляем вместо ников)
    private static final String SELF_PLACEHOLDER = "you";
    private static final String FRIEND_PLACEHOLDER = "friend";

    public ChatUtilsModule() {
        super("ChatUtils", "Chat helpers: suffix, anti-spam, name protect", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    public static ChatUtilsModule get() { return instance; }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        if (autoTip.get()) {
            _autoTipTimer++;
            int intervalTicks = autoTipInterval.getInt() * 20;
            if (_autoTipTimer >= intervalTicks) {
                _autoTipTimer = 0;
                var player = BfbMod.getClient().player;
                if (player != null && player.networkHandler != null) {
                    player.networkHandler.sendChatCommand("tip all");
                }
            }
        } else {
            _autoTipTimer = 0;
        }
    }

    // ============================================================
    // SUFFIX
    // ============================================================

    public String applySuffix(String message) {
        if (!isEnabled()) return message;
        if (message == null || message.isEmpty()) return message;
        if (message.startsWith("/") || message.startsWith(".")) return message;

        String suf = getSuffixText();
        if (suf.isEmpty()) return message;
        if (message.endsWith(suf)) return message;
        return message + " " + suf;
    }

    private String getSuffixText() {
        return switch (suffix.get()) {
            case "BFB" -> "| BFB";
            case "Undetected" -> "| Undetected";
            case "Custom" -> "| custom";
            default -> "";
        };
    }

    // ============================================================
    // ANTI-SPAM / ANTI-AD
    // ============================================================

    public boolean shouldHide(String message) {
        if (!isEnabled() || message == null || message.isEmpty()) return false;
        if (antiSpam.get() && isSpam(message)) return true;
        if (antiAd.get() && isAd(message)) return true;
        return false;
    }

    private boolean isSpam(String message) {
        long now = System.currentTimeMillis();
        long windowMs = antiSpamWindow.getInt() * 1000L;
        while (!_spamTimes.isEmpty() && now - _spamTimes.peekFirst() > windowMs) {
            _spamTimes.pollFirst();
        }
        boolean same = message.equals(_lastMessage) && (now - _lastMessageTime) < windowMs;
        _lastMessage = message;
        _lastMessageTime = now;
        _spamTimes.addLast(now);
        return same && _spamTimes.size() >= antiSpamRepeat.getInt();
    }

    private boolean isAd(String message) {
        String low = message.toLowerCase(Locale.ROOT);
        if (antiAdLinks.get()) {
            if (low.contains("http://") || low.contains("https://") || low.contains("www.")) return true;
            if (low.contains(".com") || low.contains(".net") || low.contains(".ru")
                    || low.contains(".org") || low.contains(".io") || low.contains(".gg")) return true;
        }
        if (antiAdDiscord.get()) {
            if (low.contains("discord.gg") || low.contains("discord.com")
                    || low.contains("discord.io") || low.contains("/discord")) return true;
        }
        if (antiAdServers.get()) {
            if (low.matches(".*\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?\\b.*")) return true;
            if (low.contains("mc.") || low.contains("play.") || low.contains("hub.")) return true;
        }
        if (antiAdKeyWords.get()) {
            String[] kw = {"купить", "продам", "shop", "магазин", "promo", "промокод",
                    "донат", "donate", "boost", "буст", "купи", "продажа"};
            for (String k : kw) if (low.contains(k)) return true;
        }
        return false;
    }

    // ============================================================
    // NAME PROTECT
    // ============================================================

    /** Заменяет твой ник и ники друзей на placeholder. */
    public String protectText(String message) {
        if (!isEnabled() || !nameProtect.get()) return message;
        if (message == null || message.isEmpty()) return message;

        MinecraftClient client = MinecraftClient.getInstance();
        String result = message;

        if (nameProtectSelf.get() && client.player != null) {
            String myName = client.player.getGameProfile().name();
            if (myName != null && !myName.isEmpty()) {
                result = replaceAllIgnoreCase(result, myName, SELF_PLACEHOLDER);
            }
        }

        if (nameProtectFriends.get() && client.world != null) {
            for (var entity : client.world.getPlayers()) {
                String name = entity.getGameProfile().name();
                if (name == null || name.isEmpty()) continue;
                if (com.example.bfb.FriendManager.isFriend(name)) {
                    result = replaceAllIgnoreCase(result, name, FRIEND_PLACEHOLDER);
                }
            }
        }

        return result;
    }

    /** Для nametags — прямой запрос защищённого ника. */
    public String protectName(String raw) {
        if (!isEnabled() || !nameProtect.get()) return raw;
        if (raw == null || raw.isEmpty()) return raw;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && nameProtectSelf.get()) {
            String myName = client.player.getGameProfile().name();
            if (myName != null && myName.equalsIgnoreCase(raw)) {
                return SELF_PLACEHOLDER;
            }
        }
        if (nameProtectFriends.get() && com.example.bfb.FriendManager.isFriend(raw)) {
            return FRIEND_PLACEHOLDER;
        }
        return raw;
    }

    public boolean shouldProtectInChat() {
        return isEnabled() && nameProtect.get() && nameProtectInChat.get();
    }

    public boolean shouldProtectInNametags() {
        return isEnabled() && nameProtect.get() && nameProtectInNametags.get();
    }

    /** Рекурсивная замена без потери регистра: заменяет все вхождения подстроки, игнорируя регистр. */
    private static String replaceAllIgnoreCase(String text, String find, String replace) {
        if (find.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerFind = find.toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < text.length()) {
            int idx = lowerText.indexOf(lowerFind, i);
            if (idx < 0) {
                sb.append(text, i, text.length());
                break;
            }
            sb.append(text, i, idx);
            sb.append(replace);
            i = idx + find.length();
        }
        return sb.toString();
    }

    // ============================================================
    // ANTI-REPORT
    // ============================================================

    /** Блокировать ли открытие Social Interactions / ReportPlayerScreen. */
    public boolean shouldBlockReportScreen(Object screen) {
        if (!isEnabled() || !antiReport.get() || !antiReportScreens.get()) return false;
        if (screen == null) return false;

        String name = screen.getClass().getName();
        // чтобы не тянуть сами классы (могут отсутствовать в маппингах)
        return name.endsWith(".SocialInteractionsScreen")
                || name.endsWith(".ReportPlayerScreen")
                || name.contains("SocialInteractions")
                || name.contains("ReportPlayer");
    }

    /** Блокировать ли исходящую команду/сообщение. */
    public boolean shouldBlockOutgoing(String message) {
        if (!isEnabled() || !antiReport.get() || !antiReportCommands.get()) return false;
        if (message == null || message.isEmpty()) return false;

        String low = message.toLowerCase(Locale.ROOT).trim();

        // команды /report, /wb, /watchdogreport и т.п.
        boolean isReportCmd = low.startsWith("/report")
                || low.startsWith("/wb ")
                || low.startsWith("/watchdogreport")
                || low.startsWith("/creport")
                || low.startsWith("/kick ")
                || low.startsWith("/tempban ");

        if (!isReportCmd) return false;

        // содержит ли наше имя (или защищённое)?
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String myName = client.player.getGameProfile().name();
            if (myName != null && low.contains(myName.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        // также блокируем попытки отправить репорт от нашего имени на любого друга
        if (nameProtect.get() && nameProtectFriends.get()) {
            for (var entity : client.world != null ? client.world.getPlayers() : java.util.List.<net.minecraft.entity.player.PlayerEntity>of()) {
                String name = entity.getGameProfile().name();
                if (name != null && com.example.bfb.FriendManager.isFriend(name)
                        && low.contains(name.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}