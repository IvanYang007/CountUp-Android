# CountUp R8 / ProGuard Optimization & Keep Rules

# Keep data models used in JSON persistence & StateFlow
-keep class com.countup.app.CountUpItem { *; }
-keep class com.countup.app.BackgroundTheme { *; }
-keep class com.countup.app.SortOrder { *; }
-keep class com.countup.app.CountUpBackupPayload { *; }
-keep class com.countup.app.RestoreStrategy { *; }

# Keep Enum values and valueOf methods for reflection/serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BroadcastReceivers, RemoteViewsService, and AppWidgetProvider instantiated by Android OS
-keep class com.countup.app.CountUpWidgetReceiver { *; }
-keep class com.countup.app.ResetCountReceiver { *; }
-keep class com.countup.app.CountUpWidgetService { *; }
-keep class com.countup.app.HeroWidgetReceiver { *; }
-keep class com.countup.app.HeroWidgetConfigureActivity { *; }
-keep class com.countup.app.ZenHorizonWidgetReceiver { *; }
-keep class com.countup.app.ZenHorizonConfigureActivity { *; }
-keep class com.countup.app.ZenPebbleWidgetReceiver { *; }
-keep class com.countup.app.ZenPebbleConfigureActivity { *; }
-keep class com.countup.app.SolarRhythmWidgetReceiver { *; }
-keep class com.countup.app.SolarRhythmConfigureActivity { *; }
-keep class com.countup.app.MidnightAlarmReceiver { *; }

# WorkManager / Room Database reflection safety (required by transitive Glance dependency)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}

# Compose Runtime stability
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
