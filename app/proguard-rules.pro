# CountUp R8 / ProGuard Optimization & Keep Rules

# Keep data models used in JSON persistence & StateFlow
-keep class com.countup.app.CountUpItem { *; }
-keep class com.countup.app.BackgroundTheme { *; }
-keep class com.countup.app.SortOrder { *; }
-keep class com.countup.app.ThemeMode { *; }
-keep class com.countup.app.CountUpBackupPayload { *; }
-keep class com.countup.app.RestoreStrategy { *; }
-keep class com.countup.app.WidgetResetRecord { *; }
-keep class com.countup.app.ResetSnapshot { *; }
-keep class com.countup.app.BackupValidationResult* { *; }

# Keep Enum values and valueOf methods for reflection/serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers,allowoptimization enum com.countup.app.TimeDisplayMode, com.countup.app.ZenWidgetDisplayUnit {
    <fields>;
}

