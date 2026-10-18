package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.NotificationSystem;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class EntityAlertModule extends CheatModule {
    public final ModeSetting alertMode = add(new ModeSetting("Alert on", "All",
            "All", "Hostile", "Passive", "Players", "Crystals"));
    public final NumberSetting range = add(new NumberSetting("Range", 20, 5, 64, 1));
    public final BooleanSetting notifyOnSpawn = add(new BooleanSetting("Notify spawn", true));
    public final BooleanSetting notifyOnLeave = add(new BooleanSetting("Notify leave", true));

    private final List<AlertEntity> knownEntities = new ArrayList<>();

    public EntityAlertModule() {
        super("EntityAlert", "Alerts about nearby entities", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null || client.world == null) return;

        double rangeSq = range.get() * range.get();
        List<AlertEntity> currentEntities = new ArrayList<>();

        // Собираем все сущности в радиусе
        Box area = player.getBoundingBox().expand(range.get());
        for (Entity entity : client.world.getEntitiesByClass(Entity.class, area, e -> true)) {
            if (!shouldAlert(entity)) continue;

            AlertEntity alertEntity = new AlertEntity(entity);
            currentEntities.add(alertEntity);

            // Проверяем, новая ли это сущность
            boolean isNew = true;
            for (AlertEntity known : knownEntities) {
                if (known.matches(entity)) {
                    isNew = false;
                    known.lastSeen = System.currentTimeMillis();
                    break;
                }
            }

            if (isNew && notifyOnSpawn.get()) {
                String name = entity.getName().getString();
                String type = getEntityType(entity);
                NotificationSystem.push("Spawn: " + name + " (" + type + ")", 0xFF4CC3FF, 3000);
            }
        }

        // Проверяем ушедшие сущности
        if (notifyOnLeave.get()) {
            for (AlertEntity known : knownEntities) {
                boolean found = false;
                for (AlertEntity current : currentEntities) {
                    if (known.matches(current.entity)) {
                        found = true;
                        break;
                    }
                }
                if (!found && System.currentTimeMillis() - known.lastSeen > 1000) {
                    String name = known.name;
                    String type = known.type;
                    NotificationSystem.push("Left: " + name + " (" + type + ")", 0xFFB14DFF, 3000);
                }
            }
        }

        knownEntities.clear();
        knownEntities.addAll(currentEntities);
    }

    private boolean shouldAlert(Entity entity) {
        String mode = alertMode.get();
        if (entity instanceof PlayerEntity) return mode.equals("All") || mode.equals("Players");
        if (entity instanceof EndCrystalEntity) return mode.equals("All") || mode.equals("Crystals");

        if (entity instanceof HostileEntity || entity instanceof MobEntity) {
            return mode.equals("All") || mode.equals("Hostile");
        }
        if (entity instanceof PassiveEntity || entity instanceof AnimalEntity) {
            return mode.equals("All") || mode.equals("Passive");
        }
        return false;
    }

    private String getEntityType(Entity entity) {
        if (entity instanceof PlayerEntity) return "Player";
        if (entity instanceof EndCrystalEntity) return "Crystal";
        if (entity instanceof HostileEntity) return "Hostile";
        if (entity instanceof PassiveEntity) return "Passive";
        if (entity instanceof AnimalEntity) return "Animal";
        return entity.getClass().getSimpleName();
    }

    private static class AlertEntity {
        final Entity entity;
        final String name;
        final String type;
        long lastSeen;

        AlertEntity(Entity e) {
            entity = e;
            name = e.getName().getString();
            type = getEntityType(e);
            lastSeen = System.currentTimeMillis();
        }

        boolean matches(Entity other) {
            return entity != null && entity.equals(other);
        }

        private String getEntityType(Entity e) {
            if (e instanceof PlayerEntity) return "Player";
            if (e instanceof EndCrystalEntity) return "Crystal";
            if (e instanceof HostileEntity) return "Hostile";
            if (e instanceof PassiveEntity) return "Passive";
            if (e instanceof AnimalEntity) return "Animal";
            return e.getClass().getSimpleName();
        }
    }
}
