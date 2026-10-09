package com.example.bfb;

public enum Category {
    COMBAT("Combat", 0xFFFF4D6D),
    MOVEMENT("Movement", 0xFF3DFFB0),
    RENDER("Render", 0xFFB14DFF),
    WORLD("World", 0xFFFFC14D),
    PLAYER("Player", 0xFF4DA3FF),
    MISC("Misc", 0xFF9AA0B5),
    SCRIPT("Script", 0xFF7CFFC4);

    private final String displayName;
    private final int color;

    Category(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
