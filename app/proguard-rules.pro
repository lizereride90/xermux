# Termux libs bring Guava ListenableFuture transitively - keep the empty stub, drop full Guava.
-keep class com.google.common.util.concurrent.ListenableFuture { *; }
-dontwarn com.google.common.**
# Terminal emulator uses reflection-free JNI, keep entry points
-keep class com.termux.terminal.** { *; }
-keep class com.termux.view.** { *; }
# Keep our services (referenced from manifest)
-keep class com.xermux.app.termux.TermuxSessionService { *; }
-keep class com.xermux.app.x11.XServerService { *; }
# Optimisation: allow R8 full mode to strip logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
