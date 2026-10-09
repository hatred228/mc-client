package com.example.bfb.gui;

import com.example.bfb.SalFont;
import com.example.bfb.ScriptManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

public final class ScriptEditor {
    private static final int BG = 0xF00C0C12;
    private static final int HEADER = 0xF0121220;
    private static final int TEXT = 0xFFF4F6FF;
    private static final int MUTED = 0xFF9AA0B4;
    private static final int ACCENT = 0xFF7C5CFF;

    private static boolean open;
    private static String file = "";
    private static String text = "";
    private static int cursor;
    private static int mark = -1;
    private static int scroll;
    private static boolean dirty;
    private static int boxX;
    private static int boxY;
    private static int boxW;
    private static int boxH;

    private ScriptEditor() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static String file() {
        return file;
    }

    public static void open(String name) {
        file = name;
        text = ScriptManager.read(name);
        cursor = text.length();
        mark = -1;
        scroll = 0;
        dirty = false;
        open = true;
    }

    public static void close() {
        open = false;
    }

    public static void render(DrawContext context, int screenW, int screenH) {
        if (!open) return;
        boxW = Math.min(460, Math.max(280, screenW - 40));
        boxH = Math.min(280, Math.max(180, screenH - 48));
        boxX = (screenW - boxW) / 2;
        boxY = Math.max(28, (screenH - boxH) / 2);
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, BG);
        context.fill(boxX, boxY, boxX + boxW, boxY + 18, HEADER);
        context.fill(boxX, boxY + 16, boxX + boxW / 2, boxY + 18, ACCENT);
        context.fill(boxX + boxW / 2, boxY + 16, boxX + boxW, boxY + 18, 0xFF4CC3FF);
        String title = (dirty ? "* " : "") + file;
        SalFont.draw(context, SalFont.fit(title, boxW - 16), boxX + 6, boxY + 5, TEXT);

        int lineH = SalFont.height() + 2;
        int codeY = boxY + 22;
        int codeH = boxH - 58;
        int visible = Math.max(1, codeH / lineH);
        follow(visible);
        context.enableScissor(boxX + 2, codeY, boxX + boxW - 2, codeY + codeH);
        int index = 0;
        int line = 0;
        while (index <= text.length()) {
            int end = text.indexOf('\n', index);
            if (end < 0) end = text.length();
            if (line >= scroll && line < scroll + visible) {
                int y = codeY + (line - scroll) * lineH;
                String row = text.substring(index, end);
                SalFont.draw(context, Integer.toString(line + 1), boxX + 6, y, MUTED);
                if (hasSelection()) {
                    int from = Math.max(index, selectionFrom());
                    int to = Math.min(end, selectionTo());
                    if (from < to) {
                        int x0 = boxX + 28 + SalFont.width(text.substring(index, from));
                        int x1 = boxX + 28 + SalFont.width(text.substring(index, to));
                        context.fill(x0, y, Math.max(x1, x0 + 1), y + SalFont.height(), 0x663C2E78);
                    }
                }
                SalFont.draw(context, row, boxX + 28, y, TEXT);
                if (cursor >= index && cursor <= end && ((System.currentTimeMillis() / 450) % 2) == 0) {
                    int caret = boxX + 28 + SalFont.width(text.substring(index, cursor));
                    context.fill(caret, y, caret + 1, y + SalFont.height(), 0xFFFFC14D);
                }
            }
            if (end >= text.length()) break;
            index = end + 1;
            line++;
        }
        context.disableScissor();

        int buttons = boxY + boxH - 28;
        context.fill(boxX + 6, buttons, boxX + 78, buttons + 16, 0xFF1C1830);
        context.fill(boxX + 84, buttons, boxX + 150, buttons + 16, 0xFF1C1830);
        SalFont.draw(context, "Save", boxX + 12, buttons + 4, 0xFF3DFFB0);
        SalFont.draw(context, "Close", boxX + 96, buttons + 4, TEXT);
        String hint = dirty ? "unsaved" : "Ctrl+C V";
        SalFont.draw(context, hint, boxX + boxW - 8 - SalFont.width(hint), buttons + 4, MUTED);
    }

    public static boolean mouseClicked(Click click, int screenW, int screenH) {
        if (!open) return false;
        double mx = click.x();
        double my = click.y();
        int buttons = boxY + boxH - 28;
        if (inside(mx, my, boxX + 6, buttons, 72, 16)) {
            save();
            return true;
        }
        if (inside(mx, my, boxX + 84, buttons, 66, 16)) {
            close();
            return true;
        }
        int codeY = boxY + 22;
        int codeH = boxH - 58;
        if (inside(mx, my, boxX, codeY, boxW, codeH)) {
            placeCursor(mx, my, codeY);
            mark = -1;
            return true;
        }
        if (!inside(mx, my, boxX, boxY, boxW, boxH)) close();
        return true;
    }

    public static boolean keyPressed(KeyInput input) {
        if (!open) return false;
        int key = input.key();
        boolean command = (input.modifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        boolean selecting = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        if (command && key == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        if (command && key == GLFW.GLFW_KEY_C) {
            copy();
            return true;
        }
        if (command && key == GLFW.GLFW_KEY_V) {
            paste();
            return true;
        }
        if (command && key == GLFW.GLFW_KEY_A) {
            mark = text.isEmpty() ? -1 : 0;
            cursor = text.length();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) deleteSelection();
            else if (cursor > 0) {
                int from = Character.offsetByCodePoints(text, cursor, -1);
                text = text.substring(0, from) + text.substring(cursor);
                cursor = from;
                dirty = true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) deleteSelection();
            else if (cursor < text.length()) {
                int to = Character.offsetByCodePoints(text, cursor, 1);
                text = text.substring(0, cursor) + text.substring(to);
                dirty = true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            insert("\n");
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            insert("  ");
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            nudge(selecting, -1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            nudge(selecting, 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            beginSelection(selecting);
            moveLine(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            beginSelection(selecting);
            moveLine(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_HOME) {
            beginSelection(selecting);
            cursor = lineStart(lineOf(cursor));
            return true;
        }
        if (key == GLFW.GLFW_KEY_END) {
            beginSelection(selecting);
            cursor = lineEnd(lineOf(cursor));
            return true;
        }
        return true;
    }

    public static boolean charTyped(CharInput input) {
        if (!open || !input.isValidChar()) return open;
        insert(input.asString());
        return true;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!open || !inside(mouseX, mouseY, boxX, boxY, boxW, boxH)) return false;
        scroll = Math.max(0, scroll - (int) Math.signum(vertical));
        return true;
    }

    private static void save() {
        ScriptManager.write(file, text);
        dirty = false;
    }

    private static void copy() {
        String value;
        if (hasSelection()) value = text.substring(selectionFrom(), selectionTo());
        else {
            int line = lineOf(cursor);
            value = text.substring(lineStart(line), lineEnd(line));
        }
        MinecraftClient.getInstance().keyboard.setClipboard(value);
    }

    private static void paste() {
        String clip = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        insert(clip.replace("\r\n", "\n").replace('\r', '\n'));
    }

    private static void insert(String value) {
        if (value == null || value.isEmpty()) return;
        deleteSelection();
        int room = 20000 - text.length();
        if (room <= 0) return;
        if (value.length() > room) value = value.substring(0, room);
        text = text.substring(0, cursor) + value + text.substring(cursor);
        cursor += value.length();
        dirty = true;
    }

    private static void nudge(boolean selecting, int direction) {
        if (!selecting && hasSelection()) {
            cursor = direction < 0 ? selectionFrom() : selectionTo();
            mark = -1;
            return;
        }
        beginSelection(selecting);
        if (direction < 0 && cursor > 0) cursor = Character.offsetByCodePoints(text, cursor, -1);
        if (direction > 0 && cursor < text.length()) cursor = Character.offsetByCodePoints(text, cursor, 1);
    }

    private static void beginSelection(boolean selecting) {
        if (selecting) {
            if (mark < 0) mark = cursor;
        } else {
            mark = -1;
        }
    }

    private static boolean hasSelection() {
        return mark >= 0 && mark != cursor;
    }

    private static int selectionFrom() {
        return Math.min(mark, cursor);
    }

    private static int selectionTo() {
        return Math.max(mark, cursor);
    }

    private static void deleteSelection() {
        if (!hasSelection()) return;
        int from = selectionFrom();
        int to = selectionTo();
        text = text.substring(0, from) + text.substring(to);
        cursor = from;
        mark = -1;
        dirty = true;
    }

    private static void placeCursor(double mouseX, double mouseY, int codeY) {
        int lineH = SalFont.height() + 2;
        int line = scroll + (int) ((mouseY - codeY) / lineH);
        int count = lineCount();
        if (line < 0) line = 0;
        if (line >= count) line = count - 1;
        int start = lineStart(line);
        int end = lineEnd(line);
        int col = start;
        int target = (int) mouseX - (boxX + 28);
        while (col < end && SalFont.width(text.substring(start, col + 1)) < target) col++;
        cursor = col;
    }

    private static void moveLine(int delta) {
        int line = lineOf(cursor);
        int column = cursor - lineStart(line);
        int next = line + delta;
        if (next < 0 || next >= lineCount()) return;
        int start = lineStart(next);
        int end = lineEnd(next);
        cursor = Math.min(end, start + column);
    }

    private static void follow(int visible) {
        int line = lineOf(cursor);
        if (line < scroll) scroll = line;
        if (line >= scroll + visible) scroll = line - visible + 1;
        if (scroll < 0) scroll = 0;
    }

    private static int lineOf(int index) {
        int line = 0;
        for (int i = 0; i < index && i < text.length(); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static int lineStart(int line) {
        int index = 0;
        int current = 0;
        while (current < line && index < text.length()) {
            if (text.charAt(index) == '\n') current++;
            index++;
        }
        return index;
    }

    private static int lineEnd(int line) {
        int index = lineStart(line);
        while (index < text.length() && text.charAt(index) != '\n') index++;
        return index;
    }

    private static int lineCount() {
        int count = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') count++;
        return count;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
