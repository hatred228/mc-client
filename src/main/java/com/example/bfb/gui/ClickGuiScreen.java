package com.example.bfb.gui;

import com.example.bfb.BfbConfig;
import com.example.bfb.SalFont;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.HudEditor;
import com.example.bfb.ModuleManager;
import com.example.bfb.PanicManager;
import com.example.bfb.ScriptManager;
import com.example.bfb.modules.ThemeModule;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.ColorSetting;
import com.example.bfb.setting.ModeSetting;
import com.example.bfb.setting.NumberSetting;
import com.example.bfb.setting.Setting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 128;
    private static final int SCRIPT_W = 156;
    private static final int HEADER = 18;
    private static final int ROW = 15;
    private static final int PANELS = Category.values().length;

    private static int BG = 0xE60C0C12;
    private static int HEADER_BG = 0xF0121220;
    private static int ON = 0xFF1C1830;
    private static int TEXT = 0xFFF4F6FF;
    private static int MUTED = 0xFF9AA0B4;
    private static int ACCENT = 0xFF7C5CFF;
    private static int ACCENT_2 = 0xFF4CC3FF;

    private static final int KIND_HEADER = 0;
    private static final int KIND_COLLAPSE = 1;
    private static final int KIND_MODULE = 2;
    private static final int KIND_SETTING = 3;
    private static final int KIND_FILE = 4;
    private static final int KIND_RUN = 10;
    private static final int KIND_DELETE = 11;
    private static final int KIND_STOP = 12;
    private static final int KIND_NEW = 5;
    private static final int KIND_RELOAD = 6;
    private static final int KIND_SAVE = 7;
    private static final int KIND_PROFILE = 8;
    private static final int KIND_FIELD = 9;
    private static final int KIND_EXPORT_CODE = 13;
    private static final int KIND_IMPORT_CODE = 14;
    private static final int KIND_EXPORT_FILE = 15;
    private static final int KIND_IMPORT_FILE = 16;

    private static final int[] PRESETS = {
            0xFF4D6D, 0xFF8A3D, 0xFFE14D, 0x3DFF8A, 0x3DE1FF, 0x7C5CFF, 0xFFFFFF
    };

    private static final int[] panelX = new int[PANELS];
    private static final int[] panelY = new int[PANELS];
    private static final float[] scroll = new float[PANELS];
    private static final boolean[] collapsed = new boolean[PANELS];
    private static final CheatModule[] opened = new CheatModule[PANELS];
    private static boolean placed;

    private static final Map<CheatModule, Float> glowMap = new HashMap<>();
    private static final Map<CheatModule, Float> hoverMap = new HashMap<>();
    private static long lastFrameMs;
    private static float lastDt = 0.016f;
    private static int lastMouseX;
    private static int lastMouseY;

    private final List<Hit> hits = new ArrayList<>();
    private Category dragging;
    private double dragX;
    private double dragY;
    private boolean moved;
    private boolean searchFocus;
    private boolean configOpen;
    private boolean configFocus;
    private static String search = "";
    private String configName = "default";
    private CheatModule binding;
    private NumberSetting dragNumber;
    private ColorSetting dragColor;
    private int dragChannel = -1;
    private int sliderX;
    private int sliderW;
    private Hit hover;

    public ClickGuiScreen() {
        super(Text.literal("Library Utils"));
    }

    public boolean blocksMovement() {
        return searchFocus || configFocus || ScriptEditor.isOpen();
    }

    public static boolean hasLayout() {
        return placed;
    }

    public static JsonObject exportGui() {
        JsonObject gui = new JsonObject();
        JsonArray xs = new JsonArray();
        JsonArray ys = new JsonArray();
        JsonArray closed = new JsonArray();
        for (int i = 0; i < PANELS; i++) {
            xs.add(panelX[i]);
            ys.add(panelY[i]);
            closed.add(collapsed[i]);
        }
        gui.add("x", xs);
        gui.add("y", ys);
        gui.add("collapsed", closed);
        return gui;
    }

    public static void applyGui(JsonObject gui) {
        placeDefaults();
        if (gui == null) return;
        int[] xs = ints(gui.getAsJsonArray("x"));
        int[] ys = ints(gui.getAsJsonArray("y"));
        boolean[] closed = bools(gui.getAsJsonArray("collapsed"));
        for (int i = 0; i < PANELS && i < xs.length && i < ys.length; i++) {
            panelX[i] = xs[i];
            panelY[i] = ys[i];
        }
        for (int i = 0; i < PANELS && i < closed.length; i++) collapsed[i] = closed[i];
    }

    private static void refreshTheme() {
        ThemeModule theme = ThemeModule.get();
        if (theme == null) return;
        int a = Math.max(0, Math.min(255, theme.menuAlpha.getInt()));
        BG = (a << 24) | (theme.bgColor.getRgb() & 0xFFFFFF);
        int ha = Math.min(255, a + 20);
        HEADER_BG = (ha << 24) | (theme.headerColor.getRgb() & 0xFFFFFF);
        ON = 0xFF000000 | (theme.onColor.getRgb() & 0xFFFFFF);
        TEXT = 0xFF000000 | (theme.textColor.getRgb() & 0xFFFFFF);
        MUTED = 0xFF000000 | (theme.mutedColor.getRgb() & 0xFFFFFF);
        ACCENT = 0xFF000000 | (theme.accentColor.getRgb() & 0xFFFFFF);
        ACCENT_2 = 0xFF000000 | (theme.accentColor2.getRgb() & 0xFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (PanicManager.isPanicked()) {
            this.close();
            return;
        }
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameMs) / 1000f));
        lastFrameMs = now;
        lastDt = dt;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        refreshTheme();

        int swBg = context.getScaledWindowWidth();
        int shBg = context.getScaledWindowHeight();
        context.fillGradient(0, 0, swBg, shBg, 0xD0080A10, 0xF005060B);

        GuiBackdrop.draw(context, width, height);

        ensurePlaced();
        applyDrag(mouseX);
        drawTopBar(context, mouseX, mouseY);

        hover = null;
        hits.clear();
        for (Category category : Category.values()) {
            int index = category.ordinal();
            int x = panelX[index];
            int y = panelY[index];
            int w = panelW(category);
            int body = contentHeight(category);
            int view = collapsed[index] ? 0 : Math.min(body, Math.max(40, height - y - HEADER - 16));

            GuiDraw.round(context, x, y, w, HEADER + view, 6, BG);
            GuiDraw.roundBorder(context, x, y, w, HEADER + view, 6, 0x1AFFFFFF);
            drawHeader(context, category, x, y, w);
            if (collapsed[index]) continue;
            context.enableScissor(x, y + HEADER, x + w, y + HEADER + view);
            if (category == Category.SCRIPT) drawScript(context, x, y, w, view);
            else drawModules(context, category, x, y, w, view);
            context.disableScissor();
            if (body > view) scroll[index] = clamp(scroll[index], 0, body - view);
            if (category == Category.SCRIPT && !ScriptManager.error().isEmpty()) {
                SalFont.draw(context, SalFont.fit(ScriptManager.error(), w), x, y + HEADER + view + 2, 0xFFFF6A7A);
            }
        }
        if (configOpen) drawConfigs(context);
        drawTooltip(context, mouseX, mouseY);
        ScriptEditor.render(context, width, height);
        SalFont.draw(context, SalFont.fit("LMB toggle   RMB settings   v collapse   bind right   DEL clear", width - 28),
                18, height - SalFont.height() - 6, MUTED);
    }

    private void drawTopBar(DrawContext context, int mouseX, int mouseY) {
        boolean hClear = inside(mouseX, mouseY, 216, 8, 96, 18);
        boolean hConf = inside(mouseX, mouseY, 318, 8, 82, 18);
        GuiDraw.round(context, 18, 8, 192, 18, 4, HEADER_BG);
        GuiDraw.round(context, 216, 8, 96, 18, 4, hClear ? 0xFF232438 : HEADER_BG);
        GuiDraw.round(context, 318, 8, 82, 18, 4, configOpen || hConf ? 0xFF232438 : HEADER_BG);
        context.fill(18, 25, 114, 27, ACCENT);
        context.fill(114, 25, 210, 27, ACCENT_2);
        int barText = 8 + (18 - SalFont.height()) / 2;
        String query = search.isEmpty() && !searchFocus ? "search" : search + (searchFocus ? "_" : "");
        SalFont.draw(context, SalFont.fit(query, 170), 24, barText, search.isEmpty() && !searchFocus ? MUTED : TEXT);
        SalFont.draw(context, SalFont.fit("Clear binds", 88), 222, barText, hClear ? TEXT : MUTED);
        SalFont.draw(context, SalFont.fit("Configs", 74), 328, barText, configOpen ? ACCENT_2 : hConf ? TEXT : MUTED);
    }

    private void drawTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hover == null || hover.module == null || binding != null || configOpen) return;
        String desc = hover.module.getDescription();
        if (desc == null || desc.isEmpty()) return;
        int tw = SalFont.width(desc) + 12;
        int tx = (int) Math.min(width - tw - 6, mouseX + 10);
        int ty = (int) (mouseY + 8);
        if (ty + 20 > height) ty = (int) (mouseY - 24);
        GuiDraw.round(context, tx, ty, tw, 18, 4, 0xF2131420);
        GuiDraw.roundBorder(context, tx, ty, tw, 18, 4, 0x33FFFFFF);
        SalFont.draw(context, desc, tx + 6, ty + 5, 0xFFB9C4FF);
    }

    private void drawHeader(DrawContext context, Category category, int x, int y, int w) {
        int index = category.ordinal();
        GuiDraw.roundTop(context, x, y, w, HEADER, 6, HEADER_BG);
        context.fill(x + 8, y + HEADER - 2, x + w / 2, y + HEADER - 1, ACCENT);
        context.fill(x + w / 2, y + HEADER - 1, x + w - 8, y + HEADER, ACCENT_2);

        int textY = y + (HEADER - SalFont.height()) / 2;
        SalFont.draw(context, collapsed[index] ? ">" : "v", x + 5, textY, category.getColor());
        hits.add(new Hit(x, y, 16, HEADER, category, null, null, KIND_COLLAPSE, null));

        String count = ModuleManager.enabledIn(category) + "";
        int countW = SalFont.width(count);
        SalFont.draw(context, SalFont.fit(com.example.bfb.Lang.tr(category.getDisplayName()), w - countW - 32),
                x + 16, textY, TEXT);
        SalFont.draw(context, count, x + w - 8 - countW, textY, category.getColor());
        hits.add(new Hit(x + 16, y, w - 16, HEADER, category, null, null, KIND_HEADER, null));
    }

    private void drawModules(DrawContext context, Category category, int x, int y, int w, int view) {
        int rowY = y + HEADER - (int) scroll[category.ordinal()];
        int viewBottom = y + HEADER + view;
        for (CheatModule module : modules(category)) {
            if (rowY + ROW > y + HEADER && rowY < viewBottom) {
                drawModuleRow(context, category, module, x, rowY, w);
            }
            rowY += ROW;
            if (opened[category.ordinal()] == module) {
                rowY = drawSettings(context, category, module, x, rowY, w, y, viewBottom);
            }
        }
    }

    private void drawScript(DrawContext context, int x, int y, int w, int view) {
        int index = Category.SCRIPT.ordinal();
        int rowY = y + HEADER - (int) scroll[index];
        int viewBottom = y + HEADER + view;
        for (CheatModule module : modules(Category.SCRIPT)) {
            if (rowY + ROW > y + HEADER && rowY < viewBottom) drawModuleRow(context, Category.SCRIPT, module, x, rowY, w);
            rowY += ROW;
            if (opened[index] == module) rowY = drawSettings(context, Category.SCRIPT, module, x, rowY, w, y, viewBottom);
        }
        row(context, x, rowY, w, y, viewBottom, "Refresh", ACCENT_2, KIND_RELOAD, null);
        rowY += ROW;
        for (String file : ScriptManager.files()) {
            if (!matches(file)) continue;
            scriptFile(context, x, rowY, w, y, viewBottom, file);
            rowY += ROW;
        }
        row(context, x, rowY, w, y, viewBottom, "+ new", 0xFF3DFFB0, KIND_NEW, null);
    }

    private void scriptFile(DrawContext context, int x, int rowY, int w, int panelY, int viewBottom, String file) {
        if (rowY + ROW <= panelY + HEADER || rowY >= viewBottom) return;
        int textY = rowY + Math.max(1, (ROW - SalFont.height()) / 2);
        int color = file.equals(ScriptEditor.file()) ? TEXT : MUTED;
        SalFont.draw(context, SalFont.fit(file, w - 60), x + 4, textY, color);
        hits.add(new Hit(x, rowY, w - 58, ROW - 1, Category.SCRIPT, null, null, KIND_FILE, file));
        context.fill(x + w - 46, rowY + 2, x + w - 33, rowY + ROW - 3, 0xFF2A1220);
        SalFont.draw(context, "[]", x + w - 43, textY, 0xFFFF6A7A);
        hits.add(new Hit(x + w - 46, rowY, 14, ROW - 1, Category.SCRIPT, null, null, KIND_STOP, file));
        context.fill(x + w - 30, rowY + 2, x + w - 17, rowY + ROW - 3, 0xFF12202A);
        SalFont.draw(context, ">", x + w - 26, textY, 0xFF3DFFB0);
        hits.add(new Hit(x + w - 30, rowY, 14, ROW - 1, Category.SCRIPT, null, null, KIND_RUN, file));
        context.fill(x + w - 14, rowY + 2, x + w - 2, rowY + ROW - 3, 0xFF2A1220);
        SalFont.draw(context, "x", x + w - 11, textY, 0xFFFF6A7A);
        hits.add(new Hit(x + w - 14, rowY, 12, ROW - 1, Category.SCRIPT, null, null, KIND_DELETE, file));
    }

    private void row(DrawContext context, int x, int rowY, int w, int panelY, int viewBottom, String label, int color, int kind, String id) {
        if (rowY + ROW <= panelY + HEADER || rowY >= viewBottom) return;
        int textY = rowY + Math.max(1, (ROW - SalFont.height()) / 2);
        SalFont.draw(context, SalFont.fit(label, w - 12), x + 6, textY, color);
        hits.add(new Hit(x, rowY, w, ROW - 1, Category.SCRIPT, null, null, kind, id));
    }

    private int drawSettings(DrawContext context, Category category, CheatModule module, int x, int rowY, int w, int panelY, int viewBottom) {
        for (Setting setting : module.getSettings()) {
            int h = settingHeight(setting);
            if (rowY + h > panelY + HEADER && rowY < viewBottom) {
                drawSetting(context, setting, x + 4, rowY, w - 8, h);
                hits.add(new Hit(x + 4, rowY, w - 8, h, category, module, setting, KIND_SETTING, null));
            }
            rowY += h;
        }
        return rowY;
    }

    private void drawModuleRow(DrawContext context, Category category, CheatModule module, int x, int rowY, int w) {
        boolean enabled = module.isEnabled();

        float g = glowMap.getOrDefault(module, 0f);
        glowMap.put(module, GuiDraw.approach(g, enabled ? 1f : 0f, lastDt, 14f));

        boolean hovered = inside(lastMouseX, lastMouseY, x, rowY, w, ROW - 1);
        ThemeModule themeMod = ThemeModule.get();
        boolean hoverAnim = themeMod == null || themeMod.hoverAnim.get();

        float hoverAmt = hoverMap.getOrDefault(module, 0f);
        if (hoverAnim) {
            hoverAmt = GuiDraw.approach(hoverAmt, hovered ? 1f : 0f, lastDt, 14f);
            hoverMap.put(module, hoverAmt);
        } else {
            hoverAmt = hovered ? 1f : 0f;
        }

        if (g > 0.02f) {
            int a = (int) (g * 0xEB);
            context.fill(x + 2, rowY + 1, x + w - 2, rowY + ROW - 1, (a << 24) | (ON & 0xFFFFFF));
            context.fill(x + 2, rowY + 3, x + 4, rowY + ROW - 3, (a << 24) | (category.getColor() & 0xFFFFFF));
        }
        if (hoverAmt > 0.02f) {
            int hA = (int) (hoverAmt * 0x1E);
            context.fill(x + 2, rowY + 1, x + w - 2, rowY + ROW - 1, (hA << 24) | 0xFFFFFF);
        }

        String key = binding == module ? "..." : keyName(module.getKeybind());
        int keyW = SalFont.width(key);
        int keyX = x + w - 26 - keyW;
        int textY = rowY + Math.max(1, (ROW - SalFont.height()) / 2);

        SalFont.draw(context, SalFont.fit(com.example.bfb.Lang.tr(module.getName()), keyX - x - 12),
                x + 8, textY, enabled || hovered ? TEXT : MUTED);

        context.fill(keyX - 3, rowY + 3, keyX + keyW + 3, rowY + ROW - 3,
                binding == module ? 0xFF3A2458 : 0xFF101018);
        SalFont.draw(context, key, keyX, textY, binding == module ? 0xFFFFC14D : ACCENT_2);

        int toggleW = 14;
        int toggleH = 8;
        int toggleX = x + w - toggleW - 4;
        int toggleY = rowY + (ROW - toggleH) / 2;
        int trackCol = enabled
                ? GuiDraw.lerpColor(0xFF2A1F44, ACCENT, g)
                : 0xFF2A2A38;
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, trackCol);
        int knobOff = toggleX + 1;
        int knobOn = toggleX + toggleW - 7;
        int knobX = (int) (knobOff + (knobOn - knobOff) * g);
        context.fill(knobX, toggleY + 1, knobX + 6, toggleY + 7, 0xFFF4F6FF);

        Hit hit = new Hit(x, rowY, w, ROW - 1, category, module, null, KIND_MODULE, null);
        hits.add(hit);
        if (hovered) hover = hit;
    }

    private void drawConfigs(DrawContext context) {
        int x = 318;
        int y = 32;
        int w = 160;
        List<String> profiles = BfbConfig.profiles();
        int h = 18 + ROW * 5 + profiles.size() * ROW + 6;
        GuiDraw.round(context, x, y, w, h, 5, BG);
        GuiDraw.roundBorder(context, x, y, w, h, 5, 0x2AFFFFFF);
        GuiDraw.roundTop(context, x, y, w, 16, 5, HEADER_BG);

        String shown = configName.isEmpty() && !configFocus ? "name" : configName + (configFocus ? "_" : "");
        SalFont.draw(context, SalFont.fit(shown, w - 12), x + 6, y + 4,
                configName.isEmpty() && !configFocus ? MUTED : TEXT);
        hits.add(new Hit(x, y, w, 16, null, null, null, KIND_FIELD, null));

        int rowY = y + 18;

        context.fill(x + 4, rowY, x + w - 4, rowY + ROW - 2, 0xFF1C1830);
        SalFont.draw(context, "Save profile", x + 8, rowY + 2, 0xFF3DFFB0);
        hits.add(new Hit(x, rowY, w, ROW - 1, null, null, null, KIND_SAVE, null));
        rowY += ROW;

        context.fill(x + 4, rowY, x + w - 4, rowY + ROW - 2, 0xFF122430);
        SalFont.draw(context, "Export code", x + 8, rowY + 2, 0xFF4CC3FF);
        hits.add(new Hit(x, rowY, w, ROW - 1, null, null, null, KIND_EXPORT_CODE, null));
        rowY += ROW;

        context.fill(x + 4, rowY, x + w - 4, rowY + ROW - 2, 0xFF122430);
        SalFont.draw(context, "Import code", x + 8, rowY + 2, 0xFF4CC3FF);
        hits.add(new Hit(x, rowY, w, ROW - 1, null, null, null, KIND_IMPORT_CODE, null));
        rowY += ROW;

        context.fill(x + 4, rowY, x + w - 4, rowY + ROW - 2, 0xFF231830);
        SalFont.draw(context, "Export file", x + 8, rowY + 2, 0xFFB14DFF);
        hits.add(new Hit(x, rowY, w, ROW - 1, null, null, null, KIND_EXPORT_FILE, null));
        rowY += ROW;

        context.fill(x + 4, rowY, x + w - 4, rowY + ROW - 2, 0xFF231830);
        SalFont.draw(context, "Import file", x + 8, rowY + 2, 0xFFB14DFF);
        hits.add(new Hit(x, rowY, w, ROW - 1, null, null, null, KIND_IMPORT_FILE, null));
        rowY += ROW;

        for (String profile : profiles) {
            int color = profile.equalsIgnoreCase(BfbConfig.active()) ? ACCENT_2 : MUTED;
            SalFont.draw(context, SalFont.fit(profile, w - 12), x + 6, rowY + 2, color);
            hits.add(new Hit(x, rowY, w, ROW - 1, null, null, null, KIND_PROFILE, profile));
            rowY += ROW;
        }
    }

    private void drawSetting(DrawContext context, Setting setting, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h - 1, 0xC808080E);
        int textY = y + 2;

        if (setting instanceof BooleanSetting bool) {
            SalFont.draw(context, SalFont.fit(bool.getName(), w - 26), x + 5, textY, bool.get() ? TEXT : MUTED);
            int swW = 14, swH = 8;
            int swX = x + w - swW - 5;
            int swY = y + (h - swH) / 2 - 1;
            context.fill(swX, swY, swX + swW, swY + swH, bool.get() ? ACCENT : 0xFF2A2A38);
            int knobX = bool.get() ? swX + swW - 7 : swX + 1;
            context.fill(knobX, swY + 1, knobX + 6, swY + 7, 0xFFF4F6FF);

        } else if (setting instanceof ModeSetting mode) {
            String value = mode.get();
            int valueW = SalFont.width(value);
            SalFont.draw(context, SalFont.fit(mode.getName(), w - valueW - 14), x + 5, textY, MUTED);
            context.fill(x + w - valueW - 8, textY - 1, x + w - 2, textY + SalFont.height() + 1, 0xFF1A1A28);
            SalFont.draw(context, value, x + w - valueW - 5, textY, ACCENT_2);

        } else if (setting instanceof NumberSetting number) {
            String value = number.step >= 1 ? Integer.toString(number.getInt()) : String.format(Locale.US, "%.2f", number.get());
            int valueW = SalFont.width(value);
            SalFont.draw(context, SalFont.fit(number.getName(), w - valueW - 14), x + 5, textY, MUTED);
            SalFont.draw(context, value, x + w - valueW - 5, textY, TEXT);

            int barY = textY + SalFont.height() + 2;
            int barW = w - 10;
            int barX = x + 5;
            context.fill(barX, barY, barX + barW, barY + 3, 0xFF262630);
            int fill = (int) (barW * number.ratio());
            if (fill > 1) context.fill(barX, barY, barX + fill, barY + 3, ACCENT);
            int knobX = barX + fill - 2;
            if (knobX < barX) knobX = barX;
            if (knobX > barX + barW - 4) knobX = barX + barW - 4;
            context.fill(knobX, barY - 2, knobX + 4, barY + 5, 0xFFF4F6FF);

        } else if (setting instanceof ColorSetting color) {
            SalFont.draw(context, SalFont.fit(color.getName(), w - 24), x + 5, textY, MUTED);
            context.fill(x + w - 16, y + 2, x + w - 4, y + 11, 0xFF000000 | color.getRgb());
            context.fill(x + w - 16, y + 2, x + w - 4, y + 3, 0x66FFFFFF);
            for (int i = 0; i < PRESETS.length; i++) {
                context.fill(x + 5 + i * 12, y + 13, x + 15 + i * 12, y + 21, 0xFF000000 | PRESETS[i]);
            }
            colorStrip(context, x + 5, y + 24, w - 10, color.r() / 255.0, 0xFFFF5A6A);
            colorStrip(context, x + 5, y + 29, w - 10, color.g() / 255.0, 0xFF3DFF8A);
            colorStrip(context, x + 5, y + 34, w - 10, color.b() / 255.0, 0xFF4CC3FF);
        }
    }

    private void colorStrip(DrawContext context, int x, int y, int w, double ratio, int color) {
        context.fill(x, y, x + w, y + 3, 0xFF262630);
        int fill = (int) (w * ratio);
        if (fill > 1) context.fill(x, y, x + fill, y + 3, color);
        int knobX = x + fill - 2;
        if (knobX < x) knobX = x;
        if (knobX > x + w - 4) knobX = x + w - 4;
        context.fill(knobX, y - 2, knobX + 4, y + 5, 0xFFF4F6FF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ensurePlaced();
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        if (ScriptEditor.isOpen()) return ScriptEditor.mouseClicked(click, width, height);
        if (binding != null && button >= GLFW.GLFW_MOUSE_BUTTON_MIDDLE && button <= GLFW.GLFW_MOUSE_BUTTON_8) {
            binding.bind(button);
            binding = null;
            return true;
        }
        if (inside(mx, my, 216, 8, 96, 18)) {
            ModuleManager.clearBinds();
            binding = null;
            return true;
        }
        if (inside(mx, my, 318, 8, 82, 18)) {
            configOpen = !configOpen;
            configName = BfbConfig.active();
            searchFocus = false;
            return true;
        }
        if (inside(mx, my, 18, 8, 192, 18)) {
            searchFocus = true;
            configFocus = false;
            return true;
        }
        searchFocus = false;
        configFocus = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!inside(mx, my, hit.x, hit.y, hit.w, hit.h)) continue;
            if (hit.kind == KIND_COLLAPSE || (hit.kind == KIND_HEADER && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                int index = hit.category.ordinal();
                collapsed[index] = !collapsed[index];
                BfbConfig.save();
                return true;
            }
            if (hit.kind == KIND_HEADER) {
                dragging = hit.category;
                dragX = mx - panelX[hit.category.ordinal()];
                dragY = my - panelY[hit.category.ordinal()];
                moved = false;
                return true;
            }
            if (hit.kind == KIND_RELOAD) {
                ScriptManager.reload();
                return true;
            }
            if (hit.kind == KIND_NEW) {
                ScriptEditor.open(ScriptManager.create());
                return true;
            }
            if (hit.kind == KIND_FILE) {
                ScriptEditor.open(hit.label);
                return true;
            }
            if (hit.kind == KIND_STOP) {
                ScriptManager.stop(hit.label);
                return true;
            }
            if (hit.kind == KIND_RUN) {
                ScriptManager.launch(hit.label);
                return true;
            }
            if (hit.kind == KIND_DELETE) {
                ScriptManager.delete(hit.label);
                return true;
            }
            if (hit.kind == KIND_FIELD) {
                configFocus = true;
                return true;
            }
            if (hit.kind == KIND_SAVE) {
                BfbConfig.saveProfile(configName);
                notification("Profile saved: " + configName, 0x55FF55);
                return true;
            }
            if (hit.kind == KIND_EXPORT_CODE) {
                String code = BfbConfig.exportAsCode();
                if (code != null) {
                    net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(code);
                    notification("Config code copied to clipboard (" + code.length() + " chars)", 0x55FF55);
                } else {
                    notification("Export failed", 0xFF5050);
                }
                return true;
            }
            if (hit.kind == KIND_IMPORT_CODE) {
                String code = net.minecraft.client.MinecraftClient.getInstance().keyboard.getClipboard();
                if (code != null && !code.isEmpty() && BfbConfig.importFromCode(code)) {
                    notification("Config imported from clipboard", 0x55FF55);
                } else {
                    notification("Invalid config code", 0xFF5050);
                }
                return true;
            }
            if (hit.kind == KIND_EXPORT_FILE) {
                try {
                    java.nio.file.Files.createDirectories(BfbConfig.EXPORTS);
                    String fname = (configName == null || configName.isEmpty() ? "export" : configName) + ".json";
                    java.nio.file.Path file = BfbConfig.EXPORTS.resolve(fname);
                    if (BfbConfig.exportToFile(file)) {
                        notification("Exported to " + file.getFileName(), 0x55FF55);
                    } else {
                        notification("Export failed", 0xFF5050);
                    }
                } catch (Exception e) {
                    notification("Export error: " + e.getMessage(), 0xFF5050);
                }
                return true;
            }
            if (hit.kind == KIND_IMPORT_FILE) {
                String fname = (configName == null || configName.isEmpty() ? "export" : configName) + ".json";
                java.nio.file.Path file = BfbConfig.EXPORTS.resolve(fname);
                if (BfbConfig.importFromFile(file)) {
                    notification("Imported from " + fname, 0x55FF55);
                } else {
                    notification("File not found: " + fname, 0xFF5050);
                }
                return true;
            }
            if (hit.kind == KIND_PROFILE) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    BfbConfig.deleteProfile(hit.label);
                    notification("Deleted: " + hit.label, 0xFF5050);
                } else {
                    BfbConfig.loadProfile(hit.label);
                    configName = hit.label;
                    notification("Loaded: " + hit.label, 0x55FF55);
                }
                return true;
            }
            if (hit.kind == KIND_MODULE) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    int index = hit.category.ordinal();
                    opened[index] = opened[index] == hit.module ? null : hit.module;
                    return true;
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    String key = binding == hit.module ? "..." : keyName(hit.module.getKeybind());
                    int bindLeft = bindX(hit);
                    int bindRight = bindLeft + SalFont.width(key) + 8;
                    if (mx >= bindLeft && mx <= bindRight) {
                        if (binding == hit.module) {
                            hit.module.unbind();
                            binding = null;
                        } else {
                            binding = hit.module;
                        }
                        return true;
                    }
                    if (binding != null) {
                        binding = null;
                        return true;
                    }
                    hit.module.toggle();
                    return true;
                }
            }
            if (hit.setting instanceof BooleanSetting bool) {
                bool.toggle();
                BfbConfig.save();
                return true;
            }
            if (hit.setting instanceof ModeSetting mode) {
                mode.cycle();
                BfbConfig.save();
                return true;
            }
            if (hit.setting instanceof NumberSetting number && my >= hit.y + 11) {
                dragNumber = number;
                sliderX = hit.x + 5;
                sliderW = hit.w - 10;
                applyDrag(mx);
                return true;
            }
            if (hit.setting instanceof ColorSetting color) {
                if (my >= hit.y + 12 && my <= hit.y + 22) {
                    int index = (int) ((mx - (hit.x + 5)) / 12);
                    if (index >= 0 && index < PRESETS.length) {
                        color.setRgb(PRESETS[index]);
                        BfbConfig.save();
                    }
                    return true;
                }
                if (my >= hit.y + 23) {
                    dragColor = color;
                    dragChannel = my < hit.y + 28 ? 0 : my < hit.y + 33 ? 1 : 2;
                    sliderX = hit.x + 5;
                    sliderW = hit.w - 10;
                    applyDrag(mx);
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void notification(String text, int color) {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(net.minecraft.text.Text.literal("§7[Undetected] §f" + text), true);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            int index = dragging.ordinal();
            panelX[index] = (int) (click.x() - dragX);
            panelY[index] = (int) (click.y() - dragY);
            moved = true;
            return true;
        }
        applyDrag(click.x());
        return dragNumber != null || dragColor != null || super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean wasDragging = dragging != null;
        dragging = null;
        if (dragNumber != null || dragColor != null || (wasDragging && moved)) BfbConfig.save();
        moved = false;
        dragNumber = null;
        dragColor = null;
        dragChannel = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        ensurePlaced();
        if (ScriptEditor.mouseScrolled(mouseX, mouseY, vertical)) return true;
        for (Category category : Category.values()) {
            int index = category.ordinal();
            if (collapsed[index]) continue;
            int x = panelX[index];
            int y = panelY[index];
            int w = panelW(category);
            int body = contentHeight(category);
            int view = Math.min(body, Math.max(40, this.height - y - HEADER - 16));
            if (!inside(mouseX, mouseY, x, y, w, HEADER + view)) continue;
            scroll[index] = clamp(scroll[index] - (float) vertical * 12f, 0, Math.max(0, body - view));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (PanicManager.isPanicked()) return true;
        if (ScriptEditor.isOpen()) return ScriptEditor.keyPressed(input);
        if (binding != null) {
            int code = input.key();
            if (code == GLFW.GLFW_KEY_ESCAPE || code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                binding = null;
                return true;
            }
            if (code == GLFW.GLFW_KEY_DELETE || code == GLFW.GLFW_KEY_BACKSPACE) {
                binding.unbind();
                binding = null;
                return true;
            }
            binding.bind(code);
            binding = null;
            return true;
        }
        if (configFocus) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE || input.key() == GLFW.GLFW_KEY_ENTER) {
                if (input.key() == GLFW.GLFW_KEY_ENTER) BfbConfig.saveProfile(configName);
                configFocus = false;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !configName.isEmpty()) {
                configName = configName.substring(0, configName.length() - 1);
                return true;
            }
            return true;
        }
        if (searchFocus) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE || input.key() == GLFW.GLFW_KEY_ENTER) {
                searchFocus = false;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
        }
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT && client != null) {
            client.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ScriptEditor.isOpen()) return ScriptEditor.charTyped(input);
        if (configFocus && input.isValidChar() && configName.length() < 24) {
            configName += input.asString();
            return true;
        }
        if (searchFocus && input.isValidChar() && search.length() < 24) {
            search += input.asString();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        BfbConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void applyDrag(double mouseX) {
        if (sliderW <= 0) return;
        double ratio = (mouseX - sliderX) / (double) sliderW;
        if (dragNumber != null) dragNumber.setRatio(ratio);
        if (dragColor != null && dragChannel >= 0) {
            dragColor.setChannel(dragChannel, (int) Math.round(Math.max(0, Math.min(1, ratio)) * 255));
        }
    }

    private int contentHeight(Category category) {
        if (category != Category.SCRIPT) return bodyHeight(modules(category), opened[category.ordinal()]);
        int total = 0;
        for (CheatModule module : modules(category)) {
            total += ROW;
            if (module == opened[category.ordinal()]) {
                for (Setting setting : module.getSettings()) total += settingHeight(setting);
            }
        }
        total += ROW * 2;
        for (String file : ScriptManager.files()) if (matches(file)) total += ROW;
        return Math.max(total, ROW);
    }

    private static List<CheatModule> modules(Category category) {
        String query = search.toLowerCase(Locale.ROOT);
        List<CheatModule> list = new ArrayList<>();
        for (CheatModule module : ModuleManager.getByCategory(category)) {
            if (query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(query)) list.add(module);
        }
        return list;
    }

    private boolean matches(String name) {
        return search.isEmpty() || name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private static int bodyHeight(List<CheatModule> modules, CheatModule opened) {
        int total = 0;
        for (CheatModule module : modules) {
            total += ROW;
            if (module == opened) {
                for (Setting setting : module.getSettings()) total += settingHeight(setting);
            }
        }
        return Math.max(total, ROW);
    }

    private static int settingHeight(Setting setting) {
        if (setting instanceof NumberSetting) return SalFont.height() + 8;
        if (setting instanceof ColorSetting) return 40;
        return 14;
    }

    private static int panelW(Category category) {
        return category == Category.SCRIPT ? SCRIPT_W : PANEL_W;
    }

    private static void ensurePlaced() {
        if (placed) return;
        placeDefaults();
    }

    private static void placeDefaults() {
        placed = true;
        int x = 18;
        int y = 40;
        int limit = 18 + 6 * (PANEL_W + 8);
        for (int i = 0; i < PANELS; i++) {
            int w = panelW(Category.values()[i]);
            if (x > 18 && x + w > limit) {
                x = 18;
                y += 220;
            }
            panelX[i] = x;
            panelY[i] = y;
            x += w + 8;
        }
    }

    private static int[] ints(JsonArray array) {
        if (array == null) return new int[0];
        int[] values = new int[array.size()];
        for (int i = 0; i < array.size(); i++) values[i] = array.get(i).getAsInt();
        return values;
    }

    private static boolean[] bools(JsonArray array) {
        if (array == null) return new boolean[0];
        boolean[] values = new boolean[array.size()];
        for (int i = 0; i < array.size(); i++) values[i] = array.get(i).getAsBoolean();
        return values;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int bindX(Hit hit) {
        String key = binding == hit.module ? "..." : keyName(hit.module.getKeybind());
        return hit.x + hit.w - 26 - SalFont.width(key);
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        if (key >= GLFW.GLFW_MOUSE_BUTTON_MIDDLE && key <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return switch (key) {
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                default -> "M" + (key + 1);
            };
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSH";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSH";
            case GLFW.GLFW_KEY_SPACE -> "SPC";
            default -> Integer.toString(key);
        };
    }

    private static final class Hit {
        private final int x, y, w, h;
        private final Category category;
        private final CheatModule module;
        private final Setting setting;
        private final int kind;
        private final String label;

        private Hit(int x, int y, int w, int h, Category category, CheatModule module, Setting setting, int kind, String label) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.category = category;
            this.module = module;
            this.setting = setting;
            this.kind = kind;
            this.label = label;
        }
    }
}