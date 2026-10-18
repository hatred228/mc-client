package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class HitSoundModule extends CheatModule {

    public final ModeSetting sound = add(new ModeSetting("Sound", "Hit",
            "Hit", "Click", "Pop", "Bell", "Pling", "Fart", "Moan", "Skeet", "Custom"));

    public final ModeSetting trigger = add(new ModeSetting("Trigger", "All",
            "All", "Only kill", "Only crit"));

    public final NumberSetting volume = add(new NumberSetting("Volume", 0.6, 0.1, 1.0, 0.05));
    public final NumberSetting pitch = add(new NumberSetting("Pitch", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting pitchRange = add(new NumberSetting("Pitch range", 0.15, 0.0, 0.5, 0.02));
    public final NumberSetting cooldownMs = add(new NumberSetting("Cooldown (ms)", 30, 0, 500, 10));

    private static final Identifier FART = Identifier.of("undetected", "fart");
    private static final Identifier MOAN = Identifier.of("undetected", "moan");
    private static final Identifier CUSTOM = Identifier.of("undetected", "custom_hit");

    private static final Random RNG = new Random();
    private long lastPlay = 0L;

    public HitSoundModule() {
        super("HitSound", "Sound when you hit an entity", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    public void onHit() { onHit(false, false); }

    public void onHit(boolean crit, boolean kill) {
        long now = System.currentTimeMillis();
        if (now - lastPlay < cooldownMs.getInt()) return;
        lastPlay = now;

        String trig = trigger.get();
        if (trig.equals("Only kill") && !kill) return;
        if (trig.equals("Only crit") && !crit) return;

        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;

        float v = volume.getFloat();
        float basePitch = pitch.getFloat();
        float range = pitchRange.getFloat();
        float p = basePitch + (RNG.nextFloat() - 0.5f) * 2f * range;

        SoundEvent event = pickSound();
        if (event != null) {
            player.playSound(event, v, p);
        } else {
            // fallback для custom
            player.playSound(SoundEvent.of(CUSTOM), v, p);
        }
    }

    private SoundEvent pickSound() {
        return switch (sound.get()) {
            case "Click" -> net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value();
            case "Pop" -> net.minecraft.sound.SoundEvents.ENTITY_CHICKEN_EGG;
            case "Bell" -> net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case "Pling" -> net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case "Skeet" -> net.minecraft.sound.SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case "Fart" -> SoundEvent.of(FART);
            case "Moan" -> SoundEvent.of(MOAN);
            case "Custom" -> null;
            default -> net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
        };
    }
}