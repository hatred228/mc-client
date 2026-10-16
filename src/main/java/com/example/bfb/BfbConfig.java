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
    private static final Path ACTIVE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("active_profile.txt");
    public static final Path PROFILES = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("configs");
    public static final Path EXPORTS = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("exports");
    public static final Path BACKUPS = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("backups");
    private static String active = "default";

    private BfbConfig() {}

    public static String active() { return active; }

    // ================= load / save =================

    /**
     * Загрузка при старте:
     *   1. Читает имя активного профиля из active_profile.txt (надёжнее чем json)
     *   2. Читает undetected.json для fallback
     *   3. Если профиль существует — грузит ЕГО
     *   4. Иначе — undetected.json как есть
     */
    public static void load() {
        // 1. имя активного профиля
        String fromFile = readActiveFile();
        if (fromFile != null) active = safe(fromFile);

        JsonObject root = read(FILE);
        if (root != null) {
            readMeta(root);
        }

        // 2. если профиль существует — грузим его
        Path profileFile = PROFILES.resolve(safe(active) + ".json");
        if (Files.exists(profileFile)) {
            JsonObject profile = read(profileFile);
            if (profile != null) {
                applyAll(profile, false);
                writeActiveFile();
                return;
            }
        }

        // 3. fallback
        if (root != null) applyAll(root, false);
        writeActiveFile();
    }

    /** Сохраняет текущее состояние в undetected.json. Профили НЕ трогает. */
    public static void save() {
        JsonObject previous = read(FILE);
        JsonObject root = new JsonObject();
        root.addProperty("profile", active);
        if (ClickGuiScreen.hasLayout()) root.add("gui", ClickGuiScreen.exportGui());
        else if (previous != null && previous.has("gui")) root.add("gui", previous.get("gui"));

        root.add("hud_positions", HudEditor.toJson());
        root.add("modules", modules());
        write(FILE, root);
        writeActiveFile();
        autoBackup();
    }

    /** Явно сохраняет профиль в configs/<name>.json. */
    public static void saveProfile(String name) {
        active = safe(name);

        JsonObject root = new JsonObject();
        root.addProperty("profile", active);
        if (ClickGuiScreen.hasLayout()) root.add("gui", ClickGuiScreen.exportGui());
        root.add("hud_positions", HudEditor.toJson());
        root.add("modules", modules());

        write(FILE, root);
        write(PROFILES.resolve(safe(name) + ".json"), root);
        writeActiveFile();
        autoBackup();
    }

    /** Загружает профиль, сбрасывает все модули перед применением. */
    public static boolean loadProfile(String name) {
        JsonObject root = read(PROFILES.resolve(safe(name) + ".json"));
        if (root == null) return false;

        // сброс — отключаем модули, сбрасываем бинды
        for (CheatModule module : ModuleManager.getModules()) {
            if (module.isEnabled()) {
                module.applyEnabled(false);
                try { module.onDisable(); } catch (Throwable ignored) {}
            }
            module.applyKey(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
        }

        active = safe(name);
        applyAll(root, true);
        save();
        writeActiveFile();
        return true;
    }

    public static void deleteProfile(String name) {
        try { Files.deleteIfExists(PROFILES.resolve(safe(name) + ".json")); }
        catch (IOException ignored) {}
        if (active.equalsIgnoreCase(safe(name))) {
            active = "default";
            writeActiveFile();
        }
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

    // ================= active profile file =================

    private static String readActiveFile() {
        try {
            if (!Files.exists(ACTIVE_FILE)) return null;
            String s = Files.readString(ACTIVE_FILE).trim();
            return s.isEmpty() ? null : s;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeActiveFile() {
        try {
            Files.createDirectories(ACTIVE_FILE.getParent());
            Files.writeString(ACTIVE_FILE, active);
        } catch (IOException ignored) {}
    }

    // ================= Backup system =================

    private static void autoBackup() {
        try {
            Files.createDirectories(BACKUPS);
            long now = System.currentTimeMillis();
            Path backupFile = BACKUPS.resolve("backup_" + now + ".json");
            JsonObject root = new JsonObject();
            root.addProperty("profile", active);
            root.addProperty("timestamp", now);
            if (ClickGuiScreen.hasLayout()) root.add("gui", ClickGuiScreen.exportGui());
            root.add("hud_positions", HudEditor.toJson());
            root.add("modules", modules());
            write(backupFile, root);

            keepLatestBackups(10);
        } catch (Exception ignored) {}
    }

    private static void keepLatestBackups(int max) {
        try {
            if (!Files.isDirectory(BACKUPS)) return;
            var paths = Files.list(BACKUPS)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .sorted((a, b) -> b.compareTo(a))
                    .skip(max)
                    .toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        } catch (Exception ignored) {}
    }

    public static boolean loadBackup(Path file) {
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

    private static String safe(String name) {
        String cleaned = name == null ? "" : name.trim().replaceAll("[\\\\/:*?\"<>|]", "");
        if (cleaned.isEmpty()) cleaned = "default";
        if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
        return cleaned;
    }

    // ================= Обмен конфигами =================

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