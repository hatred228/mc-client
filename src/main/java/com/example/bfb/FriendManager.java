package com.example.bfb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class FriendManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("friends.json");
    private static final Set<String> FRIENDS = new HashSet<>();

    private FriendManager() {}

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE);
            Set<String> loaded = GSON.fromJson(json, new TypeToken<Set<String>>(){}.getType());
            if (loaded != null) {
                FRIENDS.clear();
                FRIENDS.addAll(loaded);
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(FRIENDS));
        } catch (IOException ignored) {}
    }

    public static boolean isFriend(Entity entity) {
        if (!(entity instanceof PlayerEntity pe)) return false;
        String name = pe.getGameProfile().name();
        return name != null && FRIENDS.contains(name.toLowerCase());
    }

    public static boolean isFriend(String name) {
        return name != null && FRIENDS.contains(name.toLowerCase());
    }

    public static boolean toggle(String name) {
        if (name == null) return false;
        String key = name.toLowerCase();
        if (FRIENDS.contains(key)) {
            FRIENDS.remove(key);
            save();
            return false;
        }
        FRIENDS.add(key);
        save();
        return true;
    }

    public static void add(String name) {
        if (name == null) return;
        FRIENDS.add(name.toLowerCase());
        save();
    }

    public static void remove(String name) {
        if (name == null) return;
        FRIENDS.remove(name.toLowerCase());
        save();
    }

    public static Set<String> all() {
        return new HashSet<>(FRIENDS);
    }

    public static int size() {
        return FRIENDS.size();
    }
}