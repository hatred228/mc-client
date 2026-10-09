package com.example.bfb.setting;

public final class ModeSetting extends Setting {
    private final String[] modes;
    private int index;

    public ModeSetting(String name, String value, String... modes) {
        super(name);
        this.modes = modes;
        this.index = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(value)) {
                index = i;
                break;
            }
        }
    }

    public String get() {
        return modes[index];
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    public void cycle() {
        index = (index + 1) % modes.length;
    }

    @Override
    public String serialize() {
        return get();
    }

    @Override
    public void deserialize(String raw) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(raw)) {
                index = i;
                return;
            }
        }
    }
}
