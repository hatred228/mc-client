package com.example.bfb.setting;

public final class ColorSetting extends Setting {
    private int rgb;

    public ColorSetting(String name, int rgb) {
        super(name);
        this.rgb = rgb & 0xFFFFFF;
    }

    public int getRgb() {
        return rgb;
    }

    public int argb(int alpha) {
        return ((alpha & 0xFF) << 24) | rgb;
    }

    public int r() {
        return (rgb >> 16) & 0xFF;
    }

    public int g() {
        return (rgb >> 8) & 0xFF;
    }

    public int b() {
        return rgb & 0xFF;
    }

    public void setRgb(int rgb) {
        this.rgb = rgb & 0xFFFFFF;
    }

    public void setChannel(int channel, int value) {
        int clamped = Math.max(0, Math.min(255, value));
        int r = r();
        int g = g();
        int b = b();
        if (channel == 0) r = clamped;
        else if (channel == 1) g = clamped;
        else b = clamped;
        rgb = (r << 16) | (g << 8) | b;
    }

    @Override
    public String serialize() {
        return Integer.toString(rgb);
    }

    @Override
    public void deserialize(String raw) {
        try {
            rgb = Integer.parseInt(raw) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
        }
    }
}
