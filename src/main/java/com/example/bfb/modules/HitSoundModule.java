package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HitSoundModule extends CheatModule {
    public final ModeSetting sound = add(new ModeSetting("Sound", "Hit",
            "Hit", "Click", "Pop", "Fart", "Moan"));
    public final NumberSetting volume = add(new NumberSetting("Volume", 0.5, 0.1, 1.0, 0.05));
    public final NumberSetting pitch = add(new NumberSetting("Pitch", 1.0, 0.5, 2.0, 0.05));

    private static final Identifier FART = Identifier.of("libbase", "fart");
    private static final Identifier MOAN = Identifier.of("libbase", "moan");

    public HitSoundModule() {
        super("HitSound", "Sound when you hit an entity", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
    }

    public void onHit() {
        var client = BfbMod.getClient();
        var player = client.player;
        if (player == null) return;
        float v = volume.getFloat();
        float p = pitch.getFloat();
        switch (sound.get()) {
            case "Click" -> player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), v, 1.2f * p);
            case "Pop" -> player.playSound(net.minecraft.sound.SoundEvents.ENTITY_CHICKEN_EGG, v, 1.4f * p);
            case "Fart" -> custom(FART, p);
            case "Moan" -> custom(MOAN, p);
            default -> player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, v, 1.0f * p);
        }
    }

    /** Кастомный звук из ресурсов мода (assets/undetected/sounds/*.ogg). */
    private void custom(Identifier id, float pitch) {
        var player = BfbMod.getClient().player;
        if (player != null) player.playSound(SoundEvent.of(id), 1f, pitch);
    }
}
