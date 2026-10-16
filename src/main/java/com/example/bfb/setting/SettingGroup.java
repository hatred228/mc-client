package com.example.bfb.setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Группа настроек — контейнер для поднастроек.
 * Сериализуется как "name1=val1;name2=val2;...".
 */
public class SettingGroup extends Setting {
    private final List<Setting> children = new ArrayList<>();

    public SettingGroup(String name, Setting... children) {
        super(name);
        for (Setting s : children) this.children.add(s);
    }

    public List<Setting> children() { return children; }

    public void add(Setting s) { children.add(s); }

    public Setting get(String name) {
        for (Setting s : children) {
            if (s.getName().equals(name)) return s;
        }
        return null;
    }

    public boolean bool(String name) {
        Setting s = get(name);
        return s instanceof BooleanSetting b && b.get();
    }

    public float num(String name) {
        Setting s = get(name);
        return s instanceof NumberSetting n ? n.getFloat() : 0f;
    }

    public int getInt(String name) {
        Setting s = get(name);
        return s instanceof NumberSetting n ? n.getInt() : 0;
    }

    public int color(String name) {
        Setting s = get(name);
        return s instanceof ColorSetting c ? (0xFF000000 | c.getRgb()) : 0xFFFFFFFF;
    }

    public int colorRgb(String name) {
        Setting s = get(name);
        return s instanceof ColorSetting c ? c.getRgb() : 0xFFFFFF;
    }

    public String mode(String name) {
        Setting s = get(name);
        return s instanceof ModeSetting m ? m.get() : "";
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Setting s : children) {
            if (!first) sb.append(';');
            first = false;
            sb.append(s.getName()).append('=').append(s.serialize());
        }
        return sb.toString();
    }

    @Override
    public void deserialize(String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            String name = part.substring(0, eq);
            String value = part.substring(eq + 1);
            for (Setting s : children) {
                if (s.getName().equals(name)) {
                    s.deserialize(value);
                    break;
                }
            }
        }
    }
}