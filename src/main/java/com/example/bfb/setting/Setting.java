package com.example.bfb.setting;

public abstract class Setting {
    private final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String serialize();

    public abstract void deserialize(String raw);
}
