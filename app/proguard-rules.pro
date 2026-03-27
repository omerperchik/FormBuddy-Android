# ==========================================
# FormBuddy ProGuard / R8 Rules
# Aggressive optimization for APK size + runtime speed
# ==========================================

# --- R8 Full Mode ---
-allowaccessmodification
-repackageclasses ''
-overloadaggressively

# --- SQLCipher (JNI bindings) ---
-keep class net.zetetic.database.** { *; }

# --- Firebase (reflection-based serialization) ---
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Room (annotation-based) ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# --- Gson (reflection on data models) ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.formbuddy.android.data.model.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.formbuddy.android.data.model.** {
    <fields>;
    <init>(...);
}

# --- PDFBox (reflection + JNI) ---
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.tom_roush.**

# --- Generative AI / Gemini ---
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Hilt (generated code) ---
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Compose (keep stability annotations) ---
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# --- Coil (OkHttp reflection) ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- ML Kit ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --- Aggressive optimizations ---
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 7

# --- Remove logging in release ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# --- Strip Kotlin metadata (saves ~200KB) ---
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
