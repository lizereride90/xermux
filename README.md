# xermux

Got tired of juggling two apps so I smashed them together — Termux + Termux:X11 in one apk with a bottom bar to flip between them.

Two tabs, that's it:
- **Termux** — a real terminal inside the app. Uses the actual termux libs (`terminal-view` / `terminal-emulator` 0.118.0 off JitPack), so it feels like termux and not some fake web shell. Opens `/system/bin/sh` straight away so there's nothing to download, and there's a "Get full Linux" button that fetches the real bootstrap (~100MB, experimental) — after that it picks up bash automatically. Extra-keys bar included (ESC/TAB/CTRL/ALT/arrows, the stuff phone keyboards can't type).
- **Termux X11** — viewer + start/stop for the X server. Right now it runs in companion mode (same flow as the official X11 app), native `libXlorie.so` support is stubbed in for later so I can drop the .so in without reworking the service.

I don't build locally much, Actions does it. Push to main and grab the apk from the run.

## where's my apk (+ "package appears invalid" fix)

Actions tab -> newest "Build Xermux APK" run -> bottom of the page, Artifacts:
- `xermux-debug-apk` for testing
- `xermux-release-apk` (R8'd, smaller)

Three things that trip everyone up:

1. **The artifact downloads as a .zip — unzip it first.** The apk is inside. Trying to install the zip itself gives "package appears invalid".
2. **If you installed the very first release build, uninstall it first.** That one went out unsigned before I wired up signing, and Android won't update unsigned -> signed. Any build from v1.0.1 on updates normally.
3. **Allow unknown apps** for your browser/files app, and check free space (the bootstrap adds ~500MB after unpacking).

## signing

Release builds sign automatically in CI. No secrets set? It borrows the debug key so the apk still installs (fine for sideloading, not for Play). Want a stable Play-ready signature? Add these repo secrets and it'll use your key instead:

- `XERMUX_KEYSTORE_B64` — your `.jks`/`.keystore`, base64'd (`base64 -w0 mykey.jks`)
- `XERMUX_KEYSTORE_PASSWORD`, `XERMUX_KEY_ALIAS`, `XERMUX_KEY_PASSWORD`

The workflow also runs `apksigner verify --print-certs` on every release so a broken signature fails the build instead of you. Keep that key safe — lose it and Play won't accept updates.

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

Icon's hand-made: dark terminal card + green prompt with an orange X11 badge. Adaptive icon (`mipmap-anydpi-v26`), bg `#0B1F14`.

## layout

```
app/src/main/java/com/xermux/app/
  XermuxApp.kt              app class, holds the thread pool + prefs
  ui/MainActivity.kt        bottom nav, keeps both fragments alive
  ui/TermuxFragment.kt      terminalview wiring + extra keys bar
  ui/X11Fragment.kt         spinner + start/stop buttons
  termux/TermuxRunner.kt    builds the TerminalSession (shell + env)
  termux/BootstrapInstaller.kt  downloads + unpacks the bootstrap (experimental)
  termux/TermuxSessionService.kt  stub service so the session has somewhere to live
  x11/XServerService.kt     start/stop logic, tries native lib then falls back
  util/Prefs.kt             tiny sharedprefs wrapper
```

## building on your own machine if you want

```
./gradlew assembleDebug
```

Needs JDK 17 + Android SDK 34. First build pulls the termux stuff from JitPack, takes a bit.

## todo

- [x] icon that isn't the android placeholder
- [x] sign the release apk so it installs
- [x] extra keys bar
- [x] one-tap bootstrap download (experimental — symlinks via java zip are lossy, verify before trusting it)
- [ ] bundle `libXlorie.so` + `LorieView` for true embedded X (needs the full NDK X server build, that's a weekend by itself)
- [ ] Publish track: Play signing + internal testing workflow
