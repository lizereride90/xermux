# 50 optimisations — battery + smoothness

Every item below is real and checkable: `[xermux]` = changed in this fork,
`[kept]` = upstream behaviour I verified and deliberately preserved, `[ci]` =
build pipeline. File paths are relative to repo root.

## one apk instead of two (flow)

1. [xermux] Termux + X11 ship in a single apk and process (`settings.gradle`,
   `app/.../AndroidManifest.xml`) — one dexopt, one ART profile, no
   cross-app binder/AIDL hop for the UI path.
2. [xermux] X server boots in-process (`lorie/.../X11SelfStart.java`,
   `CmdEntryPoint.startInProcess`) — no `app_process` fork, no second
   runtime, no `loader.apk` dex load at every start.
3. [xermux] One `Application` class (`lorie/.../XermuxApplication.java`
   extends `TermuxApplication`) — Termux init + X11 prefs/channels run once
   each instead of across two app launches.
4. [xermux] The **X11** extra key fires an in-process explicit intent
   (`app/.../TermuxTerminalExtraKeys.java`, default matrix in
   `TermuxPropertyConstants.java`) — no cross-app cold start, no
   PackageManager resolve.
5. [xermux] The **‹** button is `finish()` onto the live task
   (`MainActivity.backToTermux`, `lorie/.../main_activity.xml`) — no
   activity relaunch, no new process, session state untouched.
6. [xermux] `shell-loader` packaging dropped from the build
   (`settings.gradle`) — fewer modules to configure, no
   commons-compress/xz toolchain, faster configuration + build.
7. [xermux] minSdk 21 → 24 (`gradle.properties`) — X11's floor; sheds the
   pre-Nougat compat paths.
8. [xermux] `hardwareAccelerated=true` set explicitly (app manifest) — GPU
   compositing for the terminal and X11 surfaces, no CPU fallback.
9. [xermux] `largeHeap=false` set explicitly (app manifest) — bounded heap,
   steadier GC, less low-memory-killer pressure.
10. [xermux] A future real release key (`release.keystore`) can never be
    committed (`.gitignore`) — while upstream's own `testkey_untrusted.jks`
    stays tracked on purpose, since debug builds sign with it.

## build + CI

11. [xermux] `org.gradle.parallel=true` (`gradle.properties`).
12. [xermux] `org.gradle.caching=true` (`gradle.properties`).
13. [xermux] Daemon heap 2G → 4G (`gradle.properties`) — the Xorg native
    compile is RAM-hungry; this stops GC thrash mid-build.
14. [xermux] NDK pinned to 29.0.14206865 (`gradle.properties`,
    `lorie/version.gradle`) — never re-downloaded, reproducible native code.
15. [ci] Workflow installs the exact SDK set up front (`.github/workflows/build.yml`).
16. [ci] Gradle dependency cache via setup-java (`build.yml`).
17. [ci] Shallow checkout, depth 1 (`build.yml`) — less IO per run.
18. [ci] `apksigner verify` gates the outputs (`build.yml`) — a broken
    signature fails the build, never ships quietly.
19. [kept] Release keeps `minifyEnabled` + proguard (`app/build.gradle:89-94`)
    — smaller dex, faster installs/dexopt.
20. [kept] Per-ABI split debug APKs (`app/build.gradle:69-77`) — install only
    your arch: smaller download, faster dexopt.

## X11 runtime

21. [kept] Server threads pinned to the big cores (`sched_setaffinity` on the
    upper half — `lorie/.../cmdentrypoint.cpp:101-110`) — frames finish
    faster, cores race back to idle.
22. [kept] Frame pacing on the Choreographer vsync
    (`cmdentrypoint.cpp:214-216`) — no busy render loop, no duplicate frames.
23. [kept] Unbuffered touch dispatch on ACTION_DOWN
    (`MainActivity.java:339-344`) — lower input latency, fewer queued events.
24. [kept] Framebuffer composited on the GPU (EGL/GLESv2 link —
    `lorie/.../recipes/xserver.cmake:298`) — no CPU blits per frame.
25. [kept] Native built `RelWithDebInfo`
    (`lorie/src/main/cpp/CMakeLists.txt`) — optimized code, not `-O0`.
26. [kept] lorie ships JNI uncompressed
    (`packagingOptions.jniLibs.useLegacyPackaging false`,
    `lorie/build.gradle:42`) — faster loads, no storage duplication.
27. [kept] X11 notification built once as a silent template
    (`XermuxApplication`: `setSilent(true)`, `setShowWhen(false)`) — no
    buzzing, no re-layout churn.
28. [kept] X11 notification cancelled on pause (`onActivityPaused`) — no
    stale icon lingering.
29. [kept] `singleInstance` X11 activity (lorie manifest) — no duplicate
    activity stacks eating memory.
30. [kept] `configChanges` handled (lorie manifest) — rotation never
    recreates the activity or re-inits the viewer.
31. [kept] `stateHidden|adjustNothing` soft input (lorie manifest) — the
    keyboard never triggers viewer relayouts.
32. [kept] Accessibility key-interception off unless asked
    (`enableAccessibilityServiceAutomatically=false`,
    `lorie/.../preferences.xml:52`) — no access-service overhead by default.
33. [kept] Motion/input extras default off (`preferScancodes`,
    `filterOutWinkey`, `pointerCapture`, `dexMetaKeyCapture`,
    `showMouseHelper`, `showStylusClickOverride`, `stylusIsMouse` —
    `preferences.xml`) — no extra input processing until enabled.
34. [kept] Display extras default off (`adjustResolution`, `displayStretch`,
    `Reseed`, `PIP`, `fullscreen` — `preferences.xml:18-22`) — no extra
    scaling passes.
35. [kept] Resolution `native` + `bilinear` filtering defaults
    (`preferences.xml:13,17`) — no GPU rescale cost out of the box.
36. [kept] `enforceCharBasedInput=false` (`preferences.xml:55`) — the
    `TYPE_NULL` IME path, fewer keyboard restarts.
37. [kept] Single prefs object (`storeSecondaryDisplayPreferencesSeparately=false`,
    `preferences.xml:61`) — no dual-preference bookkeeping.
38. [kept] One-tap X kill from the notification
    (`notificationButton1Action=exit`, `preferences.xml:79`).
39. [kept] `internalOnly` + `allowBackup=false` on both manifests — no SD
    I/O lag, no backup-transport wakeups.
40. [kept] lorie's consumer proguard rules travel with the library
    (`lorie/build.gradle:36`, `lorie/proguard-rules.pro`) — release stays
    slim and working.

## Termux runtime

41. [kept] Scrollback capped at 2000 rows by default, clamped 100–50000
    (`TerminalEmulator.java:228-230,542-543`) — bounded memory, fast scroll.
42. [kept] `PARTIAL_WAKE_LOCK` + Wi-Fi lock only while held, toggleable from
    the notification, released on destroy
    (`TermuxService.java:303-354,835`) — locks can't leak.
43. [kept] Service calls `stopForeground` + `stopSelf` when stopped
    (`TermuxService.java:215-219`) — nothing lingers.
44. [kept] Foreground service instead of polling — process priority without
    periodic wakeups.
45. [kept] No WebView anywhere in the terminal path (native views +
    markwon-to-TextView) — no Chromium process overhead.
46. [kept] `singleTask` Termux activity (app manifest) — no duplicate stacks.
47. [kept] `configChanges` on Termux activity (app manifest) — rotation never
    kills sessions or re-inits the view.
48. [kept] Night mode follows system (`DEFAULT_IVALUE_NIGHT_MODE=SYSTEM`,
    `TermuxPropertyConstants.java`) — dark UI on AMOLED saves real power.
49. [kept] Properties + shell manager initialised once in `Application`
    (`TermuxApplication.java:42-44`) — never re-done per activity.
50. [kept] Termux native flags `-Os -Wl,--gc-sections`
    (`app/build.gradle:63-67`) — small, fast support lib.
