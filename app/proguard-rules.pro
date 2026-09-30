# MGN release ProGuard rules. R8 full mode is on; these keep the
# reflection-based frameworks working after shrinking.

# --- kotlinx.serialization: keep serializers looked up by generated code ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep all @Serializable model classes and their Companions/serializers.
-keep @kotlinx.serialization.Serializable class studio.mgn.** { *; }
-keepclassmembers class studio.mgn.** {
    *** Companion;
    *** serializer(...);
}
-keepnames class studio.mgn.model.** { *; }

# --- Room: keep entities, DAOs and the generated implementation ---
-keep class studio.mgn.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# --- Media3: keep player surface hooks used via reflection ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- App entry points (referenced from the manifest) ---
-keep class studio.mgn.mgn.MgnApp { *; }
-keep class studio.mgn.mgn.MainActivity { *; }

# --- Lottie: model classes parsed by Moshi-style reflection ---
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**
