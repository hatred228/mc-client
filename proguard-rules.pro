# ============================================================
#  Ank / undetected — ProGuard rules
# ============================================================

# Отключаем сжатие и оптимизацию, чтобы не ломать байткод
-dontshrink
-dontoptimize

# Обязательные атрибуты для Fabric и Java 21
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes StackMapTable

# Защита entrypoint (мод должен найти свой главный класс)
-keep class com.example.bfb.BfbMod { *; }
-keep interface com.example.bfb.BfbMod { *; }
-keep class com.example.bfb.mixin.** { *; }
-keep class * implements com.example.bfb.mixin.** { *; }

# Fabric и библиотеки (не трогаем, чтобы не сломать загрузчик)
-keep class net.fabricmc.** { *; }
-keep class org.spongepowered.asm.** { *; }
-keep class org.luaj.** { *; }
-keep class org.lwjgl.** { *; }

# Обновляем ресурсы (fabric.mod.json)
-adaptresourcefilecontents **.properties,**.json,META-INF/MANIFEST.MF
-adaptresourcefilenames **.properties,**.txt,**.xml,**.json

-dontwarn **
-ignorewarnings

# Настройки обфускации
-repackageclasses 'ank'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames
-renamesourcefileattribute Src