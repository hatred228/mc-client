package com.example.bfb;

import com.example.bfb.modules.*;

import java.util.ArrayList;
import java.util.List;
import com.example.bfb.modules.CrossbowChargeModule;

public final class ModuleManager {
    private static final List<CheatModule> modules = new ArrayList<>();

    private ModuleManager() {
    }

    public static void init() {
        modules.clear();
        modules.add(new KillAuraModule());
        modules.add(new KeystrokesModule());
        modules.add(new PointersModule());
        modules.add(new WaypointsModule());
        modules.add(new RingsModule());
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
        modules.add(new MacePvpModule());
        modules.add(new TriggerBotModule());
        modules.add(new VelocityModule());
        modules.add(new CriticalsModule());
        modules.add(new AutoClickerModule());
        modules.add(new AutoTotemModule());
        modules.add(new FlyModule());
        modules.add(new FreecamModule());
        modules.add(new NoFallModule());
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
        modules.add(new NametagsModule());
        modules.add(new TrajectoriesModule());
        modules.add(new NoRenderModule());
        modules.add(new BlockOverlayModule());
        modules.add(new DerpModule());
        modules.add(new FakePlayerModule());
        modules.add(new PlayerModelModule());
        modules.add(new HitBoxesModule());
        modules.add(new CameraModule());
        modules.add(new NoRotateModule());
        modules.add(new WorldVisualsModule());
        modules.add(new TrailsModule());
        modules.add(new TrajectoriesModule());
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
        modules.add(new DurabilityModule());
        modules.add(new DiscordModule());
        modules.add(new ArraylistModule());
        modules.add(new TrueItemNamesModule());
        modules.add(new ChatUtilsModule());
        modules.add(new CustomVisualsModule());
        modules.add(new PlayerListModule());
        modules.add(new AutoUtilModule()); 
        modules.add(new RadarModule());
        modules.add(new TargetHudModule());
        modules.add(new SwingAnimationsModule());
        modules.add(new ChinaHatModule());
        modules.add(new JumpCirclesModule());
        modules.add(new FriendsModule());
        modules.add(new ThemeModule());

        // ===== новые модули =====
        modules.add(new CooldownsModule());
        modules.add(new PotionsModule());
        modules.add(new KeybindsModule());
        modules.add(new WatermarkModule());
        modules.add(new DamageIndicatorModule());
        modules.add(new HitRippleModule());
        modules.add(new AmbientGlowModule());

        FriendManager.load();
        ScriptManager.load();
        BfbConfig.load();
        for (CheatModule module : modules) {
            if (module.isEnabled()) module.onEnable();
        }
    }

    public static void add(CheatModule module) {
        modules.add(module);
        if (BfbMod.bindsReady()) BfbMod.registerBind(module);
    }

    public static void removeLua() {
        var iterator = modules.iterator();
        while (iterator.hasNext()) {
            CheatModule module = iterator.next();
            if (!(module instanceof LuaModule)) continue;
            drop(module);
            iterator.remove();
        }
    }

    public static void removeLuaNamed(java.util.Collection<String> names) {
        var iterator = modules.iterator();
        while (iterator.hasNext()) {
            CheatModule module = iterator.next();
            if (!(module instanceof LuaModule) || !names.contains(module.getName())) continue;
            drop(module);
            iterator.remove();
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

    public static List<CheatModule> getModules() {
        return modules;
    }

    public static List<CheatModule> getByCategory(Category category) {
        List<CheatModule> list = new ArrayList<>();
        for (CheatModule module : modules) {
            if (module.getCategory() == category) list.add(module);
        }
        return list;
    }

    public static CheatModule getModule(String name) {
        for (CheatModule module : modules) {
            if (module.getName().equalsIgnoreCase(name)) return module;
        }
        return null;
    }

    public static <T extends CheatModule> T get(Class<T> type) {
        for (CheatModule module : modules) {
            if (type.isInstance(module)) return type.cast(module);
        }
        return null;
    }

    public static int enabledIn(Category category) {
        int count = 0;
        for (CheatModule module : modules) {
            if (module.getCategory() == category && module.isEnabled()) count++;
        }
        return count;
    }

    public static void onTick() {
        for (CheatModule module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }

    public static void onStartTick() {
        for (CheatModule module : modules) {
            if (module.isEnabled()) module.onStartTick();
        }
    }
}