# ============================================================
#  Ank / undetected — ProGuard rules
# ============================================================

-dontshrink
-dontoptimize

# Запрещаем ProGuard трогать фреймы (это главная причина VerifyError)
# НЕ добавляй сюда -dontpreverify!

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

# Защита главного класса (entrypoint)
-keep class com.example.bfb.BfbMod { *; }
-keep class com.example.bfb.AnkStrings { *; }
-keep class com.example.bfb.PanicManager { *; }

# Защита миксинов (если их переименовать, игра падает с MixinApplyError)
-keep class com.example.bfb.mixin.** { *; }
-keep interface com.example.bfb.mixin.** { *; }
-keep class * implements com.example.bfb.mixin.** { *; }

# Защита остальных твоих классов (чтобы не ломались касты и рефлексия)
-keep class com.example.bfb.** { *; }

# Fabric / Mixin / библиотеки
-keep class net.fabricmc.** { *; }
-keep class org.spongepowered.asm.** { *; }
-keep class org.luaj.** { *; }
-keep class org.lwjgl.** { *; }

# Обновляем ссылки внутри ресурсов
-adaptresourcefilecontents **.properties,**.json,META-INF/MANIFEST.MF
-adaptresourcefilenames **.properties,**.txt,**.xml,**.json

-dontwarn **
-ignorewarnings

# Перепаковка
-repackageclasses 'ank'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames
-renamesourcefileattribute Src