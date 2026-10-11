# Xermux — Termux + Termux:X11 All-in-One

Android app with **2 menus**:
- **Termux** — inbuilt terminal using official `terminal-view` / `terminal-emulator` / `termux-shared` libs (`0.118.0` via JitPack). Zero download by default (`/system/bin/sh`), bootstrap-ready (`files/usr`).
- **Termux X11** — embedded viewer + `XServerService`. Companion mode works now (`pkg i x11-repo && pkg i termux-x11-nightly && termux-x11 :0`); drop `libXlorie.so` from `termux/termux-x11` into `app/src/main/jniLibs/<abi>/` later for zero-setup native mode (stub `nativeStart/nativeStop` already wired).

No local compile needed — **GitHub Actions builds the APK**.

## Get the APK
1. Push to `main` (or Run workflow manually).
2. Actions tab → latest run → Artifacts → `xermux-debug-apk` / `xermux-release-apk`.

## Optimisations included
- R8 full mode + resource shrinking (`shrinkResources`, `resConfigs en`), `proguard-android-optimize.txt`, log stripping.
- Single shared `bgExecutor` (4 threads), session reuse across menu switches (show/hide fragments, not replace).
- Transcript capped, `largeHeap=false`, `hardwareAccelerated=true`, foreground service only while X11 connected.
- Gradle: parallel, caching, configuration-cache, ABI universal APK, `useLegacyPackaging` for Termux `.so`.
- Guava conflict fixed via `listenablefuture:9999.0-empty-to-avoid-conflict-with-guava` (per Termux wiki).
- `minSdk 26` (X11 requirement), `target/compile 34`, ViewBinding, lifecycle-aware.

## Project layout
```
app/src/main/java/com/xermux/app/
  XermuxApp.kt               app-wide executor + prefs
  ui/MainActivity.kt         bottom nav: Termux | X11 (show/hide, state-safe)
  ui/TermuxFragment.kt       TerminalView + session lifecycle
  ui/X11Fragment.kt          resolution spinner, start/stop, help
  termux/TermuxRunner.kt     shell/bootstrap session factory
  termux/TermuxSessionService.kt  keeps shell alive across menus
  x11/XServerService.kt      X server lifecycle + native hook
  util/Prefs.kt              resolution/shell/bootstrap prefs
```

## Build locally (optional)
```
./gradlew assembleDebug
# needs Android SDK 34 + JDK 17
```

## Roadmap
- [ ] Bundle `bootstrap-*.zip` via `downloadBootstraps` task for offline Termux
- [ ] Bundle `libXlorie.so` + `LorieView` for true embedded X (no companion pkg)
- [ ] Extra-keys bar + Termux:API intents
- [ ] Signed release with `apksigner` + Play publishing workflow
