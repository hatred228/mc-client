package com.example.bfb;

import com.example.bfb.gui.ClickGuiScreen;
import com.example.bfb.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class BfbConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("undetected.json");
    public static final Path PROFILES = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("configs");
    public static final Path EXPORTS = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("exports");
    private static String active = "default";

    private BfbConfig() {}

    public static String active() { return active; }

    // ================= load / save =================

    public static void load() {
        JsonObject root = read(FILE);
        if (root == null) return;
        readMeta(root);
        applyAll(root, false);
    }

    public static void save() {
        JsonObject previous = read(FILE);
        JsonObject root = new JsonObject();
        root.addProperty("profile", active);
        if (ClickGuiScreen.hasLayout()) root.add("gui", ClickGuiScreen.exportGui());
        else if (previous != null && previous.has("gui")) root.add("gui", previous.get("gui"));

        // HUD позиции (для Potions, Cooldowns, Keybinds и т.д.)
        root.add("hud_positions", HudEditor.toJson());

        root.add("modules", modules());
        write(FILE, root);
        if (!active.isBlank()) write(PROFILES.resolve(safe(active) + ".json"), root);
    }

    public static void saveProfile(String name) {
        active = safe(name);
        save();
    }

    public static boolean loadProfile(String name) {
        JsonObject root = read(PROFILES.resolve(safe(name) + ".json"));
        if (root == null) return false;
        active = safe(name);
        applyAll(root, true);
        save();
        return true;
    }

    public static void deleteProfile(String name) {
        try { Files.deleteIfExists(PROFILES.resolve(safe(name) + ".json")); }
        catch (IOException ignored) {}
        if (active.equalsIgnoreCase(safe(name))) active = "default";
    }

    public static List<String> profiles() {
        if (!Files.isDirectory(PROFILES)) return List.of();
        try (Stream<Path> stream = Files.list(PROFILES)) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(path -> {
                        String file = path.getFileName().toString();
                        return file.substring(0, file.length() - 5);
                    })
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public static void reapplyLua() {
        JsonObject root = read(FILE);
        if (root == null || !root.has("modules")) return;
        JsonObject modules = root.getAsJsonObject("modules");
        for (CheatModule module : ModuleManager.getModules()) {
            if (!(module instanceof LuaModule) || !modules.has(module.getName())) continue;
            applyOne(module, modules.getAsJsonObject(module.getName()), true);
        }
    }

    private static void applyAll(JsonObject root, boolean hooks) {
        if (root.has("gui") && root.get("gui").isJsonObject()) {
            ClickGuiScreen.applyGui(root.getAsJsonObject("gui"));
        }
        if (root.has("hud_positions") && root.get("hud_positions").isJsonObject()) {
            HudEditor.fromJson(root.getAsJsonObject("hud_positions"));
        }
        if (!root.has("modules")) return;
        JsonObject modules = root.getAsJsonObject("modules");
        for (CheatModule module : ModuleManager.getModules()) {
            if (!modules.has(module.getName())) continue;
            applyOne(module, modules.getAsJsonObject(module.getName()), hooks);
        }
    }

    private static void applyOne(CheatModule module, JsonObject data, boolean hooks) {
        if (data.has("key")) {
            module.applyKey(data.get("key").getAsInt());
            BfbMod.updateBind(module);
        }
        if (data.has("settings")) {
            JsonObject settings = data.getAsJsonObject("settings");
            for (Setting setting : module.getSettings()) {
                JsonElement value = settings.get(setting.getName());
                if (value != null && value.isJsonPrimitive()) setting.deserialize(value.getAsString());
            }
        }
        if (!data.has("enabled")) return;
        boolean next = data.get("enabled").getAsBoolean();
        if (!hooks) {
            module.applyEnabled(next);
            return;
        }
        if (module.isEnabled() == next) return;
        module.applyEnabled(next);
        if (next) module.onEnable();
        else module.onDisable();
    }

    private static JsonObject modules() {
        JsonObject modules = new JsonObject();
        for (CheatModule module : ModuleManager.getModules()) {
            JsonObject data = new JsonObject();
            data.addProperty("enabled", module.isEnabled());
            data.addProperty("key", module.getKeybind());
            JsonObject settings = new JsonObject();
            for (Setting setting : module.getSettings()) {
                settings.addProperty(setting.getName(), setting.serialize());
            }
            data.add("settings", settings);
            modules.add(module.getName(), data);
        }
        return modules;
    }

    private static void readMeta(JsonObject root) {
        if (root.has("profile") && root.get("profile").isJsonPrimitive()) {
            active = safe(root.get("profile").getAsString());
        }
    }

    private static JsonObject read(Path path) {
        if (!Files.exists(path)) return null;
        try {
            return GSON.fromJson(Files.readString(path), JsonObject.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void write(Path path, JsonObject root) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException ignored) {}
    }

    private static String safe(String name) {
        String cleaned = name == null ? "" : name.trim().replaceAll("[\\\\/:*?\"<>|]", "");
        if (cleaned.isEmpty()) cleaned = "default";
        if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
        return cleaned;
    }

    // ================= Обмен конфигами =================

    /** Возвращает base64-строку со всем конфигом — можно отправить другу. */
    public static String exportAsCode() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("profile", active);
            if (ClickGuiScreen.hasLayout()) root.add("gui", ClickGuiScreen.exportGui());
            root.add("hud_positions", HudEditor.toJson());
            root.add("modules", modules());
            String json = GSON.toJson(root);
            byte[] compressed = compress(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(compressed);
        } catch (Exception e) {
            return null;
        }
    }

    /** Загружает конфиг из base64-строки. */
    public static boolean importFromCode(String code) {
        try {
            byte[] compressed = Base64.getDecoder().decode(code.trim());
            byte[] raw = decompress(compressed);
            String json = new String(raw, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return false;
            applyAll(root, true);
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Сохраняет конфиг в отдельный .json-файл. */
    public static boolean exportToFile(Path file) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("profile", active);
            if (ClickGuiScreen.hasLayout()) root.add("gui", ClickGuiScreen.exportGui());
            root.add("hud_positions", HudEditor.toJson());
            root.add("modules", modules());
            Files.writeString(file, GSON.toJson(root));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Загружает конфиг из отдельного .json-файла. */
    public static boolean importFromFile(Path file) {
        try {
            if (!Files.exists(file)) return false;
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (root == null) return false;
            applyAll(root, true);
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
            return gzip.readAllBytes();
        }
    }
}