package com.example.bfb.render.ui;

import net.minecraft.util.Identifier;

public sealed interface UiCommand
        permits UiCommand.Rect, UiCommand.RoundedRect, UiCommand.GradientRect,
                UiCommand.Outline, UiCommand.Circle, UiCommand.CircleGradient,
                UiCommand.Ring, UiCommand.Line, UiCommand.Triangle,
                UiCommand.PushScissor, UiCommand.PopScissor,
                UiCommand.Text, UiCommand.Icon, UiCommand.Image,
                UiCommand.BackdropBlur {

    enum Align { LEFT, CENTER, RIGHT }

    record Rect(float x, float y, float w, float h, int color) implements UiCommand {}
    record RoundedRect(float x, float y, float w, float h, float r, int color) implements UiCommand {}
    record GradientRect(float x, float y, float w, float h, float r, int topColor, int bottomColor) implements UiCommand {}
    record Outline(float x, float y, float w, float h, float r, float thickness, int color) implements UiCommand {}
    record Circle(float cx, float cy, float radius, int color) implements UiCommand {}
    record CircleGradient(float cx, float cy, float radius, int innerColor, int outerColor) implements UiCommand {}
    record Ring(float cx, float cy, float radius, float thickness, float progress, int trackColor, int progressColor) implements UiCommand {}
    record Line(float x1, float y1, float x2, float y2, float thickness, int color) implements UiCommand {}
    record Triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) implements UiCommand {}
    record PushScissor(float x, float y, float w, float h) implements UiCommand {}
    record PopScissor() implements UiCommand {}
    record Text(float x, float y, float size, int color, String value, Align align, boolean strong) implements UiCommand {}
    record Icon(float x, float y, float size, int color, int codePoint) implements UiCommand {}
    record Image(float x, float y, float w, float h, float r, Identifier id,
                 float u0, float v0, float u1, float v1, int color) implements UiCommand {}
    record BackdropBlur(float x, float y, float w, float h, float r, float strength, int color) implements UiCommand {}
}