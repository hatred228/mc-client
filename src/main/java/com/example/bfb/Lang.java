package com.example.bfb;

import java.util.HashMap;
import java.util.Map;

/** Перевод имён модулей и строк GUI. EN — ключ, RU — значение. */
public final class Lang {
    private static final Map<String, String> RU = new HashMap<>();
    private static boolean russian = false;

    static {
        // GUI
        RU.put("undetected", "большой-толстый-мальчик");
        // Combat
        RU.put("KillAura", "АураУбийства"); RU.put("TriggerBot", "ТриггерБот");
        RU.put("CrystalPvP", "КристаллПвП"); RU.put("Criticals", "Криты");
        RU.put("AutoClicker", "АвтоКликер"); RU.put("HitBoxes", "Хитбоксы");
        RU.put("Velocity", "Отброс"); RU.put("HitSound", "ЗвукУдара");
        RU.put("AutoGap", "АвтоГэп"); RU.put("WaterSpeed", "ВодаСпид");
        RU.put("HitEffects", "ЭффектыУбийства");
        RU.put("AutoTotem", "АвтоТотем"); RU.put("AutoArmor", "АвтоБроня");
        RU.put("MacePvP", "БулаваПвП"); RU.put("AttackAura", "АураАтаки");
        // Movement
        RU.put("Speed", "Скорость"); RU.put("Sprint", "Спринт"); RU.put("Fly", "Полёт");
        RU.put("Step", "Шаг"); RU.put("NoSlow", "БезЗамедления"); RU.put("NoFall", "БезПадения");
        RU.put("Spider", "Паук"); RU.put("Jesus", "Иисус"); RU.put("FastFall", "БыстроеПадение");
        RU.put("TargetStrafe", "ОблётЦели"); RU.put("InventoryMove", "ДвижениеВИнвентаре");
        RU.put("NoJumpDelay", "БезЗадержкиПрыжка"); RU.put("AirJump", "ПрыжокВВоздухе");
        RU.put("SafeWalk", "БезопаснаяХодьба"); RU.put("AutoWalk", "АвтоХодьба");
        RU.put("KeepSprint", "УдержаниеСпринта"); RU.put("Scaffold", "Скаффолд");
        // Render
        RU.put("ESP", "ЕСП"); RU.put("Chams", "Чамс"); RU.put("Tracers", "Трейсеры");
        RU.put("Skeleton", "Скелет"); RU.put("Nametags", "Неймтаги"); RU.put("StorageESP", "ЕСПХранилищ");
        RU.put("Trajectories", "Траектории"); RU.put("NoRender", "БезРендера");
        RU.put("BlockOverlay", "ПодсветкаБлока"); RU.put("Fullbright", "ПолнаяЯркость");
        RU.put("XRay", "ИксРей"); RU.put("WorldVisuals", "ВизуалыМира");
        RU.put("Camera", "Камера"); RU.put("Freecam", "СвободнаяКамера");
        RU.put("Trails", "Шлейфы"); RU.put("PlayerModel", "МодельИгрока");
        RU.put("Potions", "Зелья"); RU.put("Coords", "Координаты"); RU.put("Info", "Инфо");
        RU.put("Durability", "Прочность"); RU.put("Keystrokes", "Клавиши");
        RU.put("Pointers", "Указатели"); RU.put("Waypoints", "ТочкиМаршрута");
        RU.put("Rings", "Кольца"); RU.put("ItemTags", "ТегиПредметов");
        // Player
        RU.put("AntiAFK", "АнтиАФК"); RU.put("AutoEat", "АвтоЕда"); RU.put("AutoFish", "АвтоРыбалка");
        RU.put("AutoRespawn", "АвтоВозрождение"); RU.put("AutoTool", "АвтоИнструмент");
        RU.put("FastBreak", "БыстрыйСлом"); RU.put("FastPlace", "БыстраяПостановка");
        RU.put("ChestStealer", "ВорСундуков"); RU.put("ClickPearl", "ЖемчугПоКлику");
        RU.put("Nuker", "Нюкер"); RU.put("Derp", "Дерп"); RU.put("FakePlayer", "ФейкИгрок");
        RU.put("NoRotate", "БезПоворота"); RU.put("AutoSword", "АвтоМеч");
        RU.put("AutoGap", "АвтоГэпл"); RU.put("FastEat", "БыстраяЕда");
        // Client
        RU.put("Client", "Клиент"); RU.put("DiscordRPC", "Дискорд");
        RU.put("Language", "Язык"); RU.put("English", "Английский"); RU.put("Russian", "Русский");
        RU.put("ClickGUI", "КликГУИ"); RU.put("Search", "Поиск"); RU.put("Configs", "Конфиги");
        RU.put("Save", "Сохранить"); RU.put("Load", "Загрузить"); RU.put("Refresh", "Обновить");
        RU.put("New", "Новый"); RU.put("Name", "Название"); RU.put("Delete", "Удалить");
        RU.put("Clear binds", "Снять бинды"); RU.put("bind", "клавиша");
        RU.put("Modules", "Модули"); RU.put("Enabled", "Включено"); RU.put("Disabled", "Выключено");
        RU.put("Settings", "Настройки"); RU.put("Category", "Категория");
        RU.put("Combat", "Бой"); RU.put("Movement", "Движение"); RU.put("Render", "Рендер");
        RU.put("Player", "Игрок"); RU.put("Client", "Клиент"); RU.put("World", "Мир");
        RU.put("Misc", "Разное"); RU.put("Script", "Скрипты");
        RU.put("Main", "Основное"); RU.put("Other", "Другие");
        RU.put("Exit", "Выйти");
        RU.put("Potions", "Зелья");
        RU.put("Cooldowns", "Перезарядки");
        RU.put("Keybinds", "Клавиши");
        RU.put("Watermark", "Водяной знак");
        RU.put("TargetHUD", "ХП цели");
        RU.put("Arraylist", "Список");
        RU.put("Radar", "Радар");
        RU.put("Minimap", "Миникарта");
    }

    private Lang() {}

    public static void setRussian(boolean value) { russian = value; }
    public static boolean isRussian() { return russian; }

    public static String tr(String key) {
        if (!russian || key == null) return key;
        return RU.getOrDefault(key, key);
    }
}
