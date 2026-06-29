# Keep Kotlin metadata (required for coroutines)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Keep Kotlin serialization
-keepclassmembers class * {
    @kotlin.jvm.JvmField *;
}

# Keep Gson model classes (so JSON parsing works)
-keepclassmembers class com.example.petquest.** {
    <fields>;
}
-keep class com.example.petquest.data.model.** { *; }
-keep class com.example.petquest.data.repository.** { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep OkHttp (Supabase networking)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Coil (image loading)
-dontwarn coil.**

# Keep ML Kit (image labeling)
-keep class com.google.mlkit.** { *; }

# Keep WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# Keep CameraX
-dontwarn androidx.camera.**

# Keep Compose
-keep class androidx.compose.** { *; }