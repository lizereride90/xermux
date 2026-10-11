# xermux

Got tired of juggling two apps so I smashed them together — Termux + Termux:X11 in one apk with a bottom bar to flip between them.

Two tabs, that's it:
- **Termux** — a real terminal inside the app. Uses the actual termux libs (`terminal-view` / `terminal-emulator` 0.118.0 off JitPack), so it feels like termux and not some fake web shell. Opens `/system/bin/sh` straight away so there's nothing to download, and if you've got the bootstrap under `files/usr` it'll pick up bash instead.
- **Termux X11** — viewer + start/stop for the X server. Right now it runs in companion mode (same flow as the official X11 app), native `libXlorie.so` support is stubbed in for later so I can drop the .so in without reworking the service.

I don't build locally much, Actions does it. Push to main and grab the apk from the run.

## where's my apk

Actions tab -> newest "Build Xermux APK" run -> bottom of the page, Artifacts:
- `xermux-debug-apk` for testing
- `xermux-release-apk` (R8'd, smaller)

## stuff I did to keep it light

Not going to pretend I benchmarked all of this, just things that helped on my old phone:

- R8 + shrinkResources on release, only kept `en` resources. debug stays unshrunk so stacktraces are readable.
- One shared thread pool (4 threads) instead of spawning threads per session. Low-ram phones hated the old way.
- Fragments get added once and hidden/shown. Replacing them kept killing the shell every time I switched tabs, which was annoying.
- Transcript capped so `cat` on a huge file doesn't eat all memory. `largeHeap` left off on purpose.
- X11 service only goes foreground while the server is actually up. Stop means really stop — no lingering wakelock.
- Gradle parallel + cache + config cache on. Universal apk for now, can split by abi later if size becomes a problem.
- That weird guava `listenablefuture:9999.0-empty...` line is straight from the termux wiki, without it the build blows up with duplicate class errors.

minSdk 26 (X11 won't go lower), target/compile 34. ViewBinding because findViewById everywhere got old.

## layout

```
app/src/main/java/com/xermux/app/
  XermuxApp.kt              app class, holds the thread pool + prefs
  ui/MainActivity.kt        bottom nav, keeps both fragments alive
  ui/TermuxFragment.kt      terminalview wiring
  ui/X11Fragment.kt         spinner + start/stop buttons
  termux/TermuxRunner.kt    builds the TerminalSession (shell + env)
  termux/TermuxSessionService.kt  stub service so the session has somewhere to live
  x11/XServerService.kt     start/stop logic, tries native lib then falls back
  util/Prefs.kt             tiny sharedprefs wrapper
```

## building on your own machine if you want

```
./gradlew assembleDebug
```

Needs JDK 17 + Android SDK 34. First build pulls the termux stuff from JitPack, takes a bit.

## still to do

- [ ] bundle the bootstrap zips so termux works fully offline (right now it leans on system shell until bootstrap is there)
- [ ] actually bundle libXlorie + LorieView instead of companion mode
- [ ] extra keys bar, the terminal really needs one
- [ ] signed release + play track. debug signing for now.
