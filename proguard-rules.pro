# ============================================================
#  Ank / undetected — ProGuard rules for Fabric 1.21.11
# ============================================================

# --- базовое ---
-dontshrink
-dontoptimize
-dontpreverify

# --- сохраняем аннотации (mixin, accessor, redirect...) ---
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# source file name переименовываем в Src, line number table сохраняем
# (если нужно полностью убрать — закомментируй следующую строку)
-keepattributes SourceFile,LineNumberTable

# ---------- ENTRYPOINT ----------
# fabric.mod.json → entrypoints.client = com.example.bfb.BfbMod
# этот класс вызывается по имени из fabric-loader'а
-keep class com.example.bfb.BfbMod { *; }
-keep interface com.example.bfb.BfbMod { *; }

# ---------- STRING DECRYPTOR ----------
# transformer генерирует вызовы com/example/bfb/AnkStrings.decrypt
# в 264 классах. класс обязан остаться по своему имени.
-keep class com.example.bfb.AnkStrings { *; }

# ---------- MIXIN PACKAGE ----------
# fabric.mixins.json → package com.example.bfb.mixin
# mixin loader ищет классы по имени. Все accessor/invoker интерфейсы
# должны сохранить свои имена — иначе refmap не сматчится.
-keep class com.example.bfb.mixin.** { *; }
-keep interface com.example.bfb.mixin.** { *; }
-keep class * implements com.example.bfb.mixin.** { *; }

# ---------- FABRIC / MIXIN FRAMEWORK ----------
-keep class net.fabricmc.** { *; }
-keep class org.spongepowered.asm.** { *; }

# ---------- LUA (SCRIPTS) ----------
# ScriptManager вызывает LuaModule методы напрямую из Java,
# но имена строковые (table.get("onTick")). Классы должны
# сохранить публичные методы чтобы LuaModule.this.call(...) не сломался.
-keep class com.example.bfb.LuaModule { public *; }
-keep class com.example.bfb.ScriptManager { public *; }

# ---------- CONFIG / SERIALIZATION ----------
# BfbConfig сериализует Setting.serialize() → String.
# Ключи json — Setting.getName(). ЭТО СТРОКИ, они не меняются.
# Но keep'им на всякий случай весь setting + связанные.
-keep class com.example.bfb.setting.** { *; }
-keep class com.example.bfb.CheatModule { *; }
-keep class com.example.bfb.ModuleManager { *; }
-keep class com.example.bfb.Category { *; }
-keep class com.example.bfb.BfbConfig { *; }
-keep class com.example.bfb.FriendManager { *; }
-keep class com.example.bfb.HudEditor { *; }

# ---------- ЛОКАЛИЗАЦИЯ / УТИЛИТЫ ----------
-keep class com.example.bfb.Lang { *; }
-keep class com.example.bfb.Loc { *; }

# ---------- MIXIN TARGETS ----------
# наши accessor'ы и mixin'ы уже keep'нуты через mixin.**

# ---------- LWJGL / NANOVG ----------
# не keep'им — эти классы в libraryjars, proguard не может их трогать.
# но -dontwarn чтобы не ругался на unresolved references внутри них.

# ---------- ГЛОБАЛЬНОЕ ----------
-dontwarn **
-ignorewarnings

# ---- REPACKAGING ----
-repackageclasses 'ank'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames

# ---- REMOVE DEBUG ----
-renamesourcefileattribute Src

# ---- СТРОКИ ----
# не удаляем строковые литералы (иначе AnkStrings.decrypt получит пустой вход)
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF