# XYRecorder ProGuard Rules

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep our data classes (for settings serialization)
-keep class com.xyverse.xyrecorder.data.** { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Don't warn about missing optional deps
-dontwarn sun.misc.Unsafe
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# MediaCodec / MediaProjection (already in SDK)
-keep class android.media.** { *; }
