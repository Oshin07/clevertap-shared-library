# ProGuard rules for sharedeventlib
# Keep the public API intact
-keep public class com.clevertap.sharedeventlib.SharedEventTracker { *; }

# Obfuscate internal classes so reflection bypass is harder
# -keep class com.clevertap.sharedeventlib.internal.** { *; }

# Keep CleverTap SDK classes
-keep class com.clevertap.android.sdk.** { *; }
