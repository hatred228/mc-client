package com.example.bfb;

/**
 * Локализация EN/RU для кастомных элементов.
 * Для стандартных мобов/предметов используется системный перевод игры через
 * Text.translatable() — они уже на текущем языке клиента.
 */
public final class Loc {
    private static boolean ru = false;

    private Loc() {}

    public static void setRussian(boolean value) { ru = value; }
    public static boolean isRussian() { return ru; }

    /** EN по умолчанию, RU если включён. */
    public static String t(String en, String ruText) {
        return ru ? ruText : en;
    }

    /** Имя предмета — берём локализованное самой игрой. */
    public static String itemName(net.minecraft.item.ItemStack stack) {
        return stack.getName().getString();
    }

    /** Имя сущности (моба) — тоже локализовано игрой. */
    public static String entityName(net.minecraft.entity.Entity entity) {
        return entity.getName().getString();
    }
}