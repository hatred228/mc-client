package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.CombatUtil;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

public class NametagsModule extends CheatModule {
    public static NametagsModule instance;
    public final ModeSetting style = add(new ModeSetting("Style", "Celestial", "Celestial", "Simple", "Box"));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 256, 1));
    public final NumberSetting size = add(new NumberSetting("Size", 1.0, 0.5, 2.5, 0.1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFF));
    public final ColorSetting backgroundColor = add(new ColorSetting("Background", 0x101018));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final BooleanSetting showBar = add(new BooleanSetting("Show bar", true));
    public final BooleanSetting health = add(new BooleanSetting("Health numbers", true));
    public final BooleanSetting hearts = add(new BooleanSetting("Hearts", false));
    public final BooleanSetting armor = add(new BooleanSetting("Armor", false));
    public final NumberSetting armorSize = add(new NumberSetting("Armor size", 12, 8, 16, 1));
    public final NumberSetting romanSize = add(new NumberSetting("Roman size", 6, 4, 10, 1));
    public final BooleanSetting romanRubick = add(new BooleanSetting("Roman Rubick", true));
    public final BooleanSetting quickSlots = add(new BooleanSetting("Quick slots", false));
    public final BooleanSetting items = add(new BooleanSetting("Items", false));
    public final BooleanSetting model = add(new BooleanSetting("Model", false));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", true));
    public final BooleanSetting thirdPerson = add(new BooleanSetting("Third Person", true));
    public final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
    public final BooleanSetting fade = add(new BooleanSetting("Fade", true));
    public final BooleanSetting autoScale = add(new BooleanSetting("Auto scale", false));

    public NametagsModule() {
        super("Nametags", "Shows player info above head", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        instance = this;
    }

    public boolean hideVanilla(Entity entity) {
        if (!isEnabled() || !(entity instanceof LivingEntity living)) return false;
        var player = BfbMod.getClient().player;
        if (player == null) return false;
        if (!CombatUtil.accept(living, player, players.get(), mobs.get(), animals.get())) return false;
        double reach = range.get();
        return player.squaredDistanceTo(living) <= reach * reach;
    }

    public String healthSuffix(Entity entity) {
        if (!isEnabled() || !health.get() || !(entity instanceof LivingEntity living)) return "";
        return " " + Math.round(living.getHealth());
    }
}