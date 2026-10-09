package com.example.bfb;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public final class EntityCache {
    private static List<LivingEntity> living = List.of();
    private static List<Entity> all = List.of();
    private static long tick = -1;

    private EntityCache() {}

    public static void refresh(MinecraftClient client, long tickCount) {
        if (tick == tickCount || client.world == null || client.player == null) return;
        tick = tickCount;
        Box area = client.player.getBoundingBox().expand(64.0);
        List<Entity> entities = client.world.getEntitiesByClass(Entity.class, area, e -> true);
        List<LivingEntity> liv = new ArrayList<>(entities.size());
        for (Entity e : entities) if (e instanceof LivingEntity le) liv.add(le);
        all = entities;
        living = liv;
    }

    public static List<LivingEntity> living() { return living; }
    public static List<Entity> all() { return all; }
    public static List<PlayerEntity> players(MinecraftClient client) {
        List<PlayerEntity> out = new ArrayList<>();
        for (LivingEntity e : living) if (e instanceof PlayerEntity p && p != client.player) out.add(p);
        return out;
    }
}