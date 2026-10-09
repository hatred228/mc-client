package com.example.bfb.gui;

import com.example.bfb.modules.ThemeModule;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

public final class GuiBackdrop {
    private static Particle[] particles = new Particle[0];
    private static String modeCache = "";
    private static long lastNanos;
    private static final Random RNG = new Random();

    private GuiBackdrop() {
    }

    public static void draw(DrawContext context, int width, int height) {
        if (width <= 0 || height <= 0) return;
        ThemeModule theme = ThemeModule.get();
        if (theme == null) return;

        String mode = theme.particles.get();
        if ("Off".equals(mode)) return;

        int count = theme.particleCount.getInt();
        float speed = theme.particleSpeed.getFloat();
        boolean glow = theme.particleGlow.get();

        if (particles.length != count || !mode.equals(modeCache)) {
            particles = new Particle[count];
            for (int i = 0; i < count; i++) particles[i] = new Particle(width, height, RNG);
            modeCache = mode;
            lastNanos = System.nanoTime();
        }

        long now = System.nanoTime();
        float dt = Math.min((now - lastNanos) / 1.0e9f, 0.05f);
        lastNanos = now;

        for (Particle p : particles) {
            p.update(dt, width, height, mode, speed, RNG);
            drawParticle(context, p, mode, glow);
        }
    }

    private static void drawParticle(DrawContext context, Particle p, String mode, boolean glow) {
        int x = (int) p.x;
        int y = (int) p.y;
        int alpha = p.alpha();
        switch (mode) {
            case "Snow" -> {
                int c = (alpha << 24) | 0xFFFFFF;
                context.fill(x, y, x + 2, y + 2, c);
                if (p.size > 0.6f) context.fill(x + 1, y - 1, x + 2, y + 2, c);
            }
            case "Rain" -> {
                int c = ((alpha / 2) << 24) | 0xAAD4FF;
                context.fill(x, y, x + 1, y + 6, c);
            }
            case "Stars" -> {
                int c = (alpha << 24) | 0xFFFFDD;
                context.fill(x, y, x + 1, y + 1, c);
                if (glow) {
                    int g = ((alpha / 4) << 24) | 0xFFFFAA;
                    context.fill(x - 1, y, x + 2, y + 1, g);
                    context.fill(x, y - 1, x + 1, y + 2, g);
                }
            }
            case "Bubbles" -> {
                int c = (alpha << 24) | 0x88CCFF;
                context.fill(x, y, x + 3, y + 3, c);
            }
            case "Hearts" -> {
                int c = (alpha << 24) | 0xFF4488;
                context.fill(x, y, x + 2, y + 2, c);
                context.fill(x - 1, y + 1, x + 1, y + 2, c);
                context.fill(x + 1, y + 1, x + 3, y + 2, c);
            }
        }
    }

    private static final class Particle {
        float x, y, vx, vy, size, life;
        float alphaBase;

        Particle(int width, int height, Random rng) {
            x = rng.nextFloat() * width;
            y = rng.nextFloat() * height;
            vx = (rng.nextFloat() - 0.5f) * 0.3f;
            vy = 0.3f + rng.nextFloat() * 0.7f;
            size = rng.nextFloat();
            life = rng.nextFloat();
            alphaBase = 100 + rng.nextInt(155);
        }

        int alpha() {
            return (int) (alphaBase * (0.5f + life * 0.5f));
        }

        void update(float dt, int width, int height, String mode, float speed, Random rng) {
            y += vy * speed * dt * 60f;
            x += vx * speed * dt * 60f;
            life += dt * 0.5f;
            if (life > 1f) life = 0f;

            switch (mode) {
                case "Snow" -> x += (float) Math.sin(y * 0.05f) * 0.3f;
                case "Rain" -> vy = 1.5f + rng.nextFloat() * 0.5f;
                case "Bubbles" -> vy = -0.4f - rng.nextFloat() * 0.3f;
                case "Hearts" -> {
                    vy = -0.3f - rng.nextFloat() * 0.2f;
                    x += (float) Math.sin(y * 0.08f) * 0.5f;
                }
            }

            if (y > height + 4) { y = -4; x = rng.nextFloat() * width; }
            if (y < -4) { y = height + 4; x = rng.nextFloat() * width; }
            if (x < -4) x = width + 4;
            if (x > width + 4) x = -4;
        }
    }
}   