package com.example.bfb.setting;

public final class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean value) {
        super(name);
        this.value = value;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        this.value = value;
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public String serialize() {
        return Boolean.toString(value);
    }

    @Override
    public void deserialize(String raw) {
        value = Boolean.parseBoolean(raw);
    }
}
