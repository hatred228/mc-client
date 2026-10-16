package com.example.bfb;

import com.example.bfb.modules.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModuleManager {
    private static final List<CheatModule> modules = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        modules.clear();
        modules.add(new KillAuraModule());
        modules.add(new KeystrokesModule());
        modules.add(new PointersModule());
        modules.add(new WaypointsModule());
        modules.add(new HitSoundModule());
        modules.add(new AimAssistModule());
        modules.add(new AutoGapModule());
        modules.add(new TargetEspModule());
        modules.add(new WaterSpeedModule());
        modules.add(new HitEffectsModule());
        modules.add(new AttackAuraModule());
        modules.add(new ClientModule());
        modules.add(new BowAimModule());
        modules.add(new ArbaletAimModule());
        modules.add(new FastBowModule());
        modules.add(new FastArbaletModule());
        modules.add(new TargetStrafeModule());
        modules.add(new CrystalPvpModule());
        modules.add(new MacePvPModule());
        modules.add(new TriggerBotModule());
        modules.add(new VelocityModule());
        modules.add(new CriticalsModule());
        modules.add(new AutoClickerModule());
        modules.add(new AutoTotemModule());
        modules.add(new FlyModule());
        modules.add(new FreecamModule());
        modules.add(new NoFallModule());
        modules.add(new NoWebModule());
        modules.add(new WebTrapModule());
        modules.add(new JesusModule());
        modules.add(new SpeedModule());
        modules.add(new StepModule());
        modules.add(new SprintModule());
        modules.add(new FastFallModule());
        modules.add(new NoSlowdownModule());
        modules.add(new AirJumpModule());
        modules.add(new SpiderModule());
        modules.add(new SafeWalkModule());
        modules.add(new InventoryMoveModule());
        modules.add(new NoJumpDelayModule());
        modules.add(new ChamsModule());
        modules.add(new EspModule());
        modules.add(new FullbrightModule());
        modules.add(new StorageEspModule());
        modules.add(new TrajectoriesModule());
        modules.add(new NoRenderModule());
        modules.add(new BlockOverlayModule());
        modules.add(new DerpModule());
        modules.add(new FakePlayerModule());
        modules.add(new PlayerModelModule());
        modules.add(new HitBoxesModule());
        modules.add(new CameraModule());
        modules.add(new NoRotateModule());
        modules.add(new TrailsModule());
        modules.add(new CrossbowChargeModule());
        modules.add(new XRayModule());
        modules.add(new NukerModule());
        modules.add(new FastBreakModule());
        modules.add(new ScaffoldModule());
        modules.add(new AutoArmorModule());
        modules.add(new AutoEatModule());
        modules.add(new FastPlaceModule());
        modules.add(new AutoToolModule());
        modules.add(new ClickPearlModule());
        modules.add(new AutoRespawnModule());
        modules.add(new AntiAFKModule());
        modules.add(new ChestStealerModule());
        modules.add(new InfoModule());
        modules.add(new CoordsModule());
        modules.add(new AutoFishModule());
        modules.add(new DiscordModule());
        modules.add(new TrueItemNamesModule());
        modules.add(new ChatUtilsModule());
        modules.add(new PlayerWatcherModule());
        modules.add(new NeuroModule());
        modules.add(new CustomVisualsModule());
        modules.add(new PlayerListModule());
        modules.add(new AutoUtilModule());
        modules.add(new RadarModule());
        modules.add(new SwingAnimationsModule());
        modules.add(new HandsVisualModule());
        modules.add(new HitImpactModule());
        modules.add(new ChinaHatModule());
        modules.add(new JumpCirclesModule());
        modules.add(new FriendsModule());
        modules.add(new ThemeModule());

        modules.add(new ServerAlertModule());
        modules.add(new SkyBoxModule());
        modules.add(new DamageIndicatorModule());
        modules.add(new HitRippleModule());
        modules.add(new AmbientGlowModule());
        modules.add(new EntityAlertModule());
        VisualsModule visuals = new VisualsModule();
modules.add(new VisualsSubModule("Nametags", "HUD nametags settings", visuals.nametags));
modules.add(new VisualsSubModule("Tracers", "Tracer lines settings", visuals.tracers));
modules.add(new VisualsSubModule("Watermark", "Watermark settings", visuals.watermark));
modules.add(new VisualsSubModule("Potions", "Potion effects HUD settings", visuals.potions));
modules.add(new VisualsSubModule("Cooldowns", "Item cooldowns HUD settings", visuals.cooldowns));
modules.add(new VisualsSubModule("Keybinds", "Keybind list settings", visuals.keybinds));
modules.add(new VisualsSubModule("TargetHud", "Target info panel settings", visuals.targetHud));
modules.add(new VisualsSubModule("Armor", "Armor HUD settings", visuals.armorHud));
modules.add(new VisualsSubModule("Notifications", "Toast notification settings", visuals.notifications));
modules.add(new VisualsSubModule("StaffDetector", "Staff detection HUD settings", visuals.staff));
modules.add(new VisualsSubModule("ArrayList", "Enabled modules list settings", visuals.arraylist));

        // ─── перенос визуалов в категорию VISUALS ─────────────
        moveToVisuals("ESP", "Chams", "TargetESP", "StorageESP", "BlockOverlay",
                "Trails", "Trajectories", "CustomVisuals", "AmbientGlow",
                "HitRipple", "JumpCircles", "ChinaHat", "SwingAnimations",
                "HitEffects", "HitSound", "Visuals");

        FriendManager.load();
        ScriptManager.load();
        BfbConfig.load();
        for (CheatModule module : modules) {
            if (module.isEnabled()) module.onEnable();
        }
    }

    private static void moveToVisuals(String... names) {
        Set<String> set = new HashSet<>(Arrays.asList(names));
        for (CheatModule m : modules) {
            if (set.contains(m.getName())) m.setCategory(Category.VISUALS);
        }
    }

    public static void add(CheatModule module) {
        modules.add(module);
        if (BfbMod.bindsReady()) BfbMod.registerBind(module);
    }

    public static void removeLua() {
        var it = modules.iterator();
        while (it.hasNext()) {
            CheatModule m = it.next();
            if (!(m instanceof LuaModule)) continue;
            drop(m);
            it.remove();
        }
    }

    public static void removeLuaNamed(java.util.Collection<String> names) {
        var it = modules.iterator();
        while (it.hasNext()) {
            CheatModule m = it.next();
            if (!(m instanceof LuaModule) || !names.contains(m.getName())) continue;
            drop(m);
            it.remove();
        }
    }

    private static void drop(CheatModule module) {
        if (module.isEnabled()) module.onDisable();
        module.applyEnabled(false);
        BfbMod.forget(module);
    }

    public static void clearBinds() {
        for (CheatModule module : modules) module.unbind();
    }

    public static List<CheatModule> getModules() { return modules; }

    public static List<CheatModule> getByCategory(Category category) {
        List<CheatModule> list = new ArrayList<>();
        for (CheatModule module : modules) {
            if (module.getCategory() == category) list.add(module);
        }
        return list;
    }

    public static CheatModule getModule(String name) {
        for (CheatModule m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public static <T extends CheatModule> T get(Class<T> type) {
        for (CheatModule m : modules) {
            if (type.isInstance(m)) return type.cast(m);
        }
        return null;
    }

    public static int enabledIn(Category category) {
        int count = 0;
        for (CheatModule m : modules) {
            if (m.getCategory() == category && m.isEnabled()) count++;
        }
        return count;
    }

    public static void onTick() {
        for (CheatModule m : modules) {
            if (m.isEnabled()) m.onTick();
        }
    }

    public static void onStartTick() {
        for (CheatModule m : modules) {
            if (m.isEnabled()) m.onStartTick();
        }
    }
}