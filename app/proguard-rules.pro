# CountUp R8 / ProGuard Optimization & Keep Rules

# Keep data models used in JSON persistence & StateFlow
-keep class com.countup.app.CountUpItem { *; }
-keep class com.countup.app.BackgroundTheme { *; }
-keep class com.countup.app.SortOrder { *; }

# Keep Enum values and valueOf methods for reflection/serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BroadcastReceivers and AppWidgetProvider instantiated by Android OS
-keep class com.countup.app.CountUpWidget { *; }
-keep class com.countup.app.WidgetUpdateReceiver { *; }

# Compose Runtime stability
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
