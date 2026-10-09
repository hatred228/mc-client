package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.lwjgl.glfw.GLFW;

public class NoRenderModule extends CheatModule {
    public final BooleanSetting fire = add(new BooleanSetting("Fire", true));
    public final BooleanSetting explosions = add(new BooleanSetting("Explosions", true));
    public final BooleanSetting particles = add(new BooleanSetting("Particles", true));
    public final BooleanSetting fog = add(new BooleanSetting("Fog", true));

    public NoRenderModule() {
        super("NoRender", "Hides fire, explosions, particles and fog", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    public boolean hideParticle(ParticleEffect effect) {
        if (!isEnabled()) return false;
        if (particles.get()) return true;
        if (!explosions.get() || effect == null) return false;
        var type = effect.getType();
        return type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER;
    }
}
