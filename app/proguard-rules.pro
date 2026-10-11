# termux drags in guava stuff, this empty stub keeps the duplicate-class error away
-keep class com.google.common.util.concurrent.ListenableFuture { *; }
-dontwarn com.google.common.**

# terminal libs are loaded via JNI-ish paths, don't let R8 eat them
-keep class com.termux.terminal.** { *; }
-keep class com.termux.view.** { *; }

# manifest references these by name
-keep class com.xermux.app.termux.TermuxSessionService { *; }
-keep class com.xermux.app.x11.XServerService { *; }

# drop verbose logs in release, keep warnings/errors
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
