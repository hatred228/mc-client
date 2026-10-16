package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.RotationManager;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class NeuroModule extends CheatModule {

    public final BooleanSetting loop = add(new BooleanSetting("Loop playback", true));
    public final NumberSetting playbackSpeed = add(new NumberSetting("Speed", 1.0, 0.25, 3.0, 0.25));
    public final BooleanSetting recordRotation = add(new BooleanSetting("Record rotation", true));
    public final BooleanSetting recordAttacks = add(new BooleanSetting("Record attacks", true));

    private static final Gson GSON = new GsonBuilder().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("undetected").resolve("neuro");

    private static NeuroModule instance;

    private boolean recording;
    private String recordingName = "";
    private final List<NeuroFrame> buffer = new ArrayList<>();

    private boolean playing;
    private String playingName = "";
    private List<NeuroFrame> playback = List.of();
    private int playbackTick;
    private int loopCount;
    private int playbackAccumulator;

    // читается миксином NeuroMovementMixin
    public float overrideForward;
    public float overrideSideways;
    public boolean overrideJump;
    public boolean overrideSneak;
    public boolean overrideSprint;

    public NeuroModule() {
        super("Neuro", "Record and replay combat movements", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    public static NeuroModule get() { return instance; }
    public boolean isRecording() { return recording; }
    public boolean isPlaying() { return playing; }

    @Override
    public void onDisable() {
        recording = false;
        playing = false;
        buffer.clear();
        playback = List.of();
        overrideForward = 0f;
        overrideSideways = 0f;
        overrideJump = false;
        overrideSneak = false;
        overrideSprint = false;
        RotationManager.release();
    }

    // ============================================================
    // COMMAND HANDLER
    // ============================================================

    public static void handleCommand(String args) {
        NeuroModule m = get();
        if (m == null) return;
        ClientPlayerEntity player = BfbMod.getClient().player;
        if (player == null) return;

        args = args.trim();
        if (args.isEmpty()) {
            feedback(player, "usage: .ai train/stop/play/off/list/delete");
            return;
        }

        String lower = args.toLowerCase(Locale.ROOT);
        if (lower.startsWith("train")) {
            m.startRecording(extractName(args, "train"));
        } else if (lower.startsWith("stop")) {
            m.stopRecording();
        } else if (lower.startsWith("play")) {
            m.startPlayback(extractName(args, "play"));
        } else if (lower.startsWith("off")) {
            m.stopPlayback();
            feedback(player, "neuro: playback off");
        } else if (lower.startsWith("list")) {
            List<String> names = m.listRecordings();
            feedback(player, "neuro: " + (names.isEmpty() ? "(empty)" : String.join(", ", names)));
        } else if (lower.startsWith("delete")) {
            String n = extractName(args, "delete");
            if (m.deleteRecording(n)) feedback(player, "neuro: deleted " + n);
            else feedback(player, "neuro: not found: " + n);
        } else {
            feedback(player, "unknown: " + args);
        }
    }

    private static String extractName(String args, String cmd) {
        String rest = args.substring(cmd.length()).trim();
        if (rest.startsWith("\"") && rest.endsWith("\"") && rest.length() > 1) {
            return rest.substring(1, rest.length() - 1);
        }
        return rest;
    }

    private static void feedback(ClientPlayerEntity player, String msg) {
        player.sendMessage(net.minecraft.text.Text.literal("§7[§dNeuro§7] §f" + msg), false);
    }

    // ============================================================
    // RECORD
    // ============================================================

    private void startRecording(String name) {
        if (name == null || name.isBlank()) {
            ClientPlayerEntity p = BfbMod.getClient().player;
            if (p != null) feedback(p, "need name: .ai train \"fight1\"");
            return;
        }
        recordingName = name;
        buffer.clear();
        recording = true;
        playing = false;
        setEnabled(true);
        ClientPlayerEntity p = BfbMod.getClient().player;
        if (p != null) feedback(p, "recording → " + name);
    }

    private void stopRecording() {
        if (!recording) return;
        recording = false;
        ClientPlayerEntity p = BfbMod.getClient().player;
        if (p == null) return;
        if (buffer.isEmpty()) {
            feedback(p, "nothing recorded");
            return;
        }
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve(safe(recordingName) + ".json");
            Files.writeString(file, GSON.toJson(buffer));
            feedback(p, "saved " + recordingName + " (" + buffer.size() + " frames)");
        } catch (IOException e) {
            feedback(p, "save failed: " + e.getMessage());
        }
        buffer.clear();
    }

    private void capture() {
        ClientPlayerEntity player = BfbMod.getClient().player;
        if (player == null) return;

        // forward/sideways/sneak — public поля LivingEntity
        float fwd = player.forwardSpeed;
        float side = player.sidewaysSpeed;
        boolean jump = player.isJumping();
        boolean sneak = player.isSneaking();
        boolean sprint = player.isSprinting();

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        boolean attack = recordAttacks.get() && player.handSwinging && player.handSwingTicks == 0;

        buffer.add(new NeuroFrame(fwd, side, jump, sneak, sprint, yaw, pitch, attack));
    }

    // ============================================================
    // PLAYBACK
    // ============================================================

    private void startPlayback(String name) {
        if (name == null || name.isBlank()) {
            ClientPlayerEntity p = BfbMod.getClient().player;
            if (p != null) feedback(p, "need name: .ai play \"fight1\"");
            return;
        }
        try {
            Path file = DIR.resolve(safe(name) + ".json");
            if (!Files.exists(file)) {
                ClientPlayerEntity p = BfbMod.getClient().player;
                if (p != null) feedback(p, "not found: " + name);
                return;
            }
            String json = Files.readString(file);
            List<NeuroFrame> data = GSON.fromJson(json, new TypeToken<List<NeuroFrame>>(){}.getType());
            if (data == null || data.isEmpty()) {
                ClientPlayerEntity p = BfbMod.getClient().player;
                if (p != null) feedback(p, "empty: " + name);
                return;
            }
            playback = data;
            playbackTick = 0;
            loopCount = 0;
            playing = true;
            recording = false;
            setEnabled(true);
            ClientPlayerEntity p = BfbMod.getClient().player;
            if (p != null) feedback(p, "playing " + name + " (" + data.size() + " frames)");
        } catch (Exception e) {
            ClientPlayerEntity p = BfbMod.getClient().player;
            if (p != null) feedback(p, "load failed: " + e.getMessage());
        }
    }

    private void stopPlayback() {
        playing = false;
        playbackTick = 0;
        overrideForward = 0f;
        overrideSideways = 0f;
        overrideJump = false;
        overrideSneak = false;
        overrideSprint = false;
        RotationManager.release();
    }

    private void advancePlayback() {
        if (playback.isEmpty()) { playing = false; return; }
        int idx = playbackTick % playback.size();
        NeuroFrame f = playback.get(idx);

        overrideForward = f.forward();
        overrideSideways = f.sideways();
        overrideJump = f.jump();
        overrideSneak = f.sneak();
        overrideSprint = f.sprint();

        ClientPlayerEntity p = BfbMod.getClient().player;

        if (recordRotation.get()) {
            // silent rotation + немедленная отправка, чтобы сервер увидел при attack
            RotationManager.setAndSend(f.yaw(), f.pitch(), 2);
            if (p != null) {
                p.setBodyYaw(f.yaw());
                p.setHeadYaw(f.yaw());
            }
        }

        if (f.attack() && recordAttacks.get()) {
            var target = pickAnyTarget();
            if (target != null) {
                // silent rotation уже ушла на сервер
                com.example.bfb.CombatUtil.attack(target);
            }
        }

        playbackAccumulator++;
        int step = Math.max(1, (int) Math.round(playbackSpeed.get()));
        if (playbackAccumulator >= step) {
            playbackAccumulator = 0;
            playbackTick++;
        }

        if (playbackTick >= playback.size()) {
            if (loop.get()) { playbackTick = 0; loopCount++; }
            else { playing = false; }
        }
    }

    private net.minecraft.entity.Entity pickAnyTarget() {
        var client = BfbMod.getClient();
        ClientPlayerEntity me = client.player;
        if (me == null) return null;
        var box = me.getBoundingBox().expand(3.5);
        var entities = client.world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, box, e ->
                e != me && e.isAlive() && !com.example.bfb.FriendManager.isFriend(e));
        return entities.isEmpty() ? null : entities.get(0);
    }

    // ============================================================
    // LIST / DELETE
    // ============================================================

    private List<String> listRecordings() {
        if (!Files.isDirectory(DIR)) return List.of();
        try (Stream<Path> stream = Files.list(DIR)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> {
                        String n = p.getFileName().toString();
                        return n.substring(0, n.length() - 5);
                    })
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private boolean deleteRecording(String name) {
        try { return Files.deleteIfExists(DIR.resolve(safe(name) + ".json")); }
        catch (IOException e) { return false; }
    }

    private static String safe(String s) {
        String c = s == null ? "" : s.trim().replaceAll("[\\\\/:*?\"<>|]", "");
        if (c.isEmpty()) c = "default";
        if (c.length() > 32) c = c.substring(0, 32);
        return c;
    }

    @Override
    public void onTick() {
        if (recording) capture();
        if (playing) advancePlayback();
    }

    public record NeuroFrame(
            float forward, float sideways,
            boolean jump, boolean sneak, boolean sprint,
            float yaw, float pitch,
            boolean attack
    ) {}
}