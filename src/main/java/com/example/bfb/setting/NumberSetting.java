package com.example.bfb.setting;

public final class NumberSetting extends Setting {
    private double value;
    public final double min;
    public final double max;
    public final double step;

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    public double get() {
        return value;
    }

    public float getFloat() {
        return (float) value;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public void set(double next) {
        double snapped = Math.round(next / step) * step;
        value = Math.max(min, Math.min(max, snapped));
    }

    public double ratio() {
        return (value - min) / (max - min);
    }

    public void setRatio(double ratio) {
        double clamped = Math.max(0, Math.min(1, ratio));
        set(min + (max - min) * clamped);
    }

    @Override
    public String serialize() {
        return Double.toString(value);
    }

    @Override
    public void deserialize(String raw) {
        try {
            set(Double.parseDouble(raw));
        } catch (NumberFormatException ignored) {
        }
    }
}
