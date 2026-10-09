package com.example.bfb;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ScriptManager {
    private static final String EXAMPLE = """
            -- Custom scripts: Script section in the menu (Right Shift)
            -- or files in config/undetected/scripts
            -- bfb.module("Name", "description", function(mod)
            --   mod.onEnable = function()
            --     bfb.chat("script enabled")
            --   end
            --   mod.onTick = function()
            --     local x, y, z = bfb.pos()
            --     if bfb.health() < 8 then
            --       bfb.chat(string.format("low hp  %.0f %.0f %.0f", x, y, z))
            --     end
            --   end
            -- end)
            """;

    private static final Map<String, List<String>> OWNED = new HashMap<>();
    private static String loading;
    private static String error = "";

    private ScriptManager() {
    }

    public static Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve("undetected").resolve("scripts");
    }

    public static String error() {
        return error;
    }

    public static void report(String script, String hook, Exception e) {
        String message = script + "." + hook + ": " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        error = message;
        var client = BfbMod.getClient();
        var player = client != null ? client.player : null;
        if (player != null) {
            player.sendMessage(net.minecraft.text.Text.literal("§c[Script] " + message), true);
        }
    }

    public static void load() {
        try {
            Files.createDirectories(folder());
            Path example = folder().resolve("example.lua");
            if (!Files.exists(example)) Files.writeString(example, EXAMPLE);
            runAll();
        } catch (Exception e) {
            error = message(e);
        }
    }

    public static void reload() {
        ModuleManager.removeLua();
        error = "";
        runAll();
        BfbConfig.reapplyLua();
    }

    public static List<String> files() {
        if (!Files.isDirectory(folder())) return List.of();
        try (Stream<Path> stream = Files.list(folder())) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".lua"))
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            error = message(e);
            return List.of();
        }
    }

    public static String read(String name) {
        try {
            return Files.readString(folder().resolve(safeFile(name)));
        } catch (IOException e) {
            error = message(e);
            return "";
        }
    }

    public static void write(String name, String text) {
        try {
            Files.createDirectories(folder());
            Files.writeString(folder().resolve(safeFile(name)), text == null ? "" : text);
            error = "";
            reload();
        } catch (IOException e) {
            error = message(e);
        }
    }

    public static void delete(String name) {
        String safe = safeFile(name);
        removeOwned(safe);
        try {
            Files.deleteIfExists(folder().resolve(safe));
        } catch (IOException e) {
            error = message(e);
        }
        if (safe.equals(com.example.bfb.gui.ScriptEditor.file())) com.example.bfb.gui.ScriptEditor.close();
    }

    /** Остановка скрипта — убирает модули файла, НЕ удаляя сам файл. */
    public static void stop(String name) {
        removeOwned(safeFile(name));
    }

    /** Событие убийства — вызывается из TriggerBot/KillAura. */
    public static void kill(String victim) {
        int fired = 0;
        int totalLua = 0;
        for (CheatModule module : ModuleManager.getModules()) {
            if (module instanceof LuaModule lua) {
                totalLua++;
                if (lua.isEnabled()) {
                    lua.onKill(victim);
                    fired++;
                }
            }
        }
        var p = BfbMod.getClient().player;
        if (p != null) {
            p.sendMessage(net.minecraft.text.Text.literal(
                    "§7[kill] §f" + victim + " §7total=" + totalLua + " fired=" + fired), true);
        }
    }

    public static void launch(String name) {
        String safe = safeFile(name);
        removeOwned(safe);
        loading = safe;
        OWNED.put(safe, new ArrayList<>());
        try {
            run(folder().resolve(safe));
            error = "";
        } catch (Exception e) {
            error = safe + ": " + message(e);
        } finally {
            loading = null;
        }
        BfbConfig.reapplyLua();
    }

    public static String create() {
        try {
            Files.createDirectories(folder());
            for (int i = 1; i < 100; i++) {
                String name = "script" + i + ".lua";
                Path path = folder().resolve(name);
                if (Files.exists(path)) continue;
                Files.writeString(path, "-- " + name + "\n\n");
                return name;
            }
        } catch (IOException e) {
            error = message(e);
        }
        return "script.lua";
    }

    private static void runAll() {
        OWNED.clear();
        List<String> failures = new ArrayList<>();
        for (String name : files()) {
            loading = name;
            OWNED.put(name, new ArrayList<>());
            try {
                run(folder().resolve(name));
            } catch (Exception e) {
                failures.add(name + ": " + message(e));
            } finally {
                loading = null;
            }
        }
        if (!failures.isEmpty()) error = failures.get(0);
    }

    private static void removeOwned(String file) {
        List<String> names = OWNED.remove(file);
        if (names != null && !names.isEmpty()) ModuleManager.removeLuaNamed(names);
    }

    private static void run(Path file) throws IOException {
        Globals globals = JsePlatform.standardGlobals();
        LuaTable api = new LuaTable();
        api.set("module", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String name = args.checkjstring(1);
                String description = "Lua";
                LuaValue setup = args.arg(2);
                if (args.narg() >= 3 && args.arg(2).isstring()) {
                    description = args.checkjstring(2);
                    setup = args.arg(3);
                }
                LuaTable table = new LuaTable();
                if (setup.isfunction()) setup.call(table);
                ModuleManager.add(new LuaModule(name, description, Category.SCRIPT, table));
                if (loading != null) OWNED.computeIfAbsent(loading, key -> new ArrayList<>()).add(name);
                return table;
            }
        });
        api.set("chat", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ClientPlayerEntity player = BfbMod.getClient().player;
                if (player != null) {
                    player.sendMessage(net.minecraft.text.Text.literal(args.tojstring(1)), false);
                }
                return LuaValue.NIL;
            }
        });
        api.set("say", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ClientPlayerEntity player = BfbMod.getClient().player;
                if (player != null && player.networkHandler != null) {
                    player.networkHandler.sendChatMessage(args.tojstring(1));
                }
                return LuaValue.NIL;
            }
        });
        api.set("enabled", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                CheatModule module = ModuleManager.getModule(args.checkjstring(1));
                return LuaValue.valueOf(module != null && module.isEnabled());
            }
        });
        api.set("toggle", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                CheatModule module = ModuleManager.getModule(args.checkjstring(1));
                if (module != null) module.toggle();
                return LuaValue.NIL;
            }
        });
        api.set("health", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ClientPlayerEntity player = BfbMod.getClient().player;
                return LuaValue.valueOf(player == null ? 0 : player.getHealth());
            }
        });
        api.set("pos", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ClientPlayerEntity player = BfbMod.getClient().player;
                if (player == null) return LuaValue.varargsOf(new LuaValue[]{LuaValue.ZERO, LuaValue.ZERO, LuaValue.ZERO});
                return LuaValue.varargsOf(new LuaValue[]{
                        LuaValue.valueOf(player.getX()),
                        LuaValue.valueOf(player.getY()),
                        LuaValue.valueOf(player.getZ())
                });
            }
        });
        api.set("look", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var player = BfbMod.getClient().player;
                if (player == null) return LuaValue.NIL;
                if (args.narg() == 0) {
                    return LuaValue.varargsOf(new LuaValue[]{
                            LuaValue.valueOf(player.getYaw()), LuaValue.valueOf(player.getPitch())});
                }
                player.setYaw((float) args.checkdouble(1));
                if (args.narg() >= 2) player.setPitch((float) args.checkdouble(2));
                return LuaValue.NIL;
            }
        });
        api.set("mine", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var client = BfbMod.getClient();
                var player = client.player;
                if (player == null || client.interactionManager == null || client.world == null) return LuaValue.FALSE;
                net.minecraft.util.math.BlockPos pos = net.minecraft.util.math.BlockPos.ofFloored(
                        args.checkdouble(1), args.checkdouble(2), args.checkdouble(3));
                if (client.world.getBlockState(pos).isAir()) return LuaValue.FALSE;
                net.minecraft.util.math.Vec3d center = net.minecraft.util.math.Vec3d.ofCenter(pos);
                double dx = center.x - player.getX();
                double dz = center.z - player.getZ();
                double dy = center.y - (player.getY() + player.getStandingEyeHeight());
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1.0e-4) return LuaValue.FALSE;
                player.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
                player.setPitch((float) Math.toDegrees(Math.asin(
                        net.minecraft.util.math.MathHelper.clamp(-dy / len, -1, 1))));
                client.interactionManager.updateBlockBreakingProgress(pos, net.minecraft.util.math.Direction.UP);
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                return LuaValue.TRUE;
            }
        });
        api.set("block", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var client = BfbMod.getClient();
                if (client.world == null) return LuaValue.valueOf("minecraft:air");
                net.minecraft.util.math.BlockPos pos = net.minecraft.util.math.BlockPos.ofFloored(
                        args.checkdouble(1), args.checkdouble(2), args.checkdouble(3));
                return LuaValue.valueOf(net.minecraft.registry.Registries.BLOCK
                        .getId(client.world.getBlockState(pos).getBlock()).toString());
            }
        });
        api.set("forward", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var player = BfbMod.getClient().player;
                if (player == null || player.input == null) return LuaValue.NIL;
                boolean hold = args.optboolean(1, true);
                player.input.playerInput = new net.minecraft.util.PlayerInput(hold, false, false, false, false, false, false);
                return LuaValue.NIL;
            }
        });
        api.set("grounded", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var player = BfbMod.getClient().player;
                return LuaValue.valueOf(player != null && player.isOnGround());
            }
        });
        api.set("inGui", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(BfbMod.getClient().currentScreen != null);
            }
        });
        globals.set("bfb", api);
        globals.load(Files.readString(file), file.getFileName().toString()).call();
    }

    private static String safeFile(String name) {
        String cleaned = name == null ? "" : name.trim().replace("\\", "").replace("/", "");
        if (cleaned.isEmpty()) cleaned = "script.lua";
        if (!cleaned.toLowerCase(Locale.ROOT).endsWith(".lua")) cleaned += ".lua";
        return cleaned;
    }

    private static String message(Exception e) {
        String text = e.getMessage();
        return text == null || text.isBlank() ? e.getClass().getSimpleName() : text;
    }
}