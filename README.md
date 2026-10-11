# xermux

Termux and Termux:X11, one apk. Not a wrapper, not a re-skin — the actual
termux-app UI (first launch installs the bootstrap, `pkg` works, sessions,
extra keys, the lot) with the actual X11 server built in from termux-x11
sources. Flip between them with the **X11** key in the extra-keys bar, hop
back with the **‹** button in X11.

## first launch

1. Open the app — real Termux. It fetches the bootstrap (~100MB) and sets up
   `$PREFIX` exactly like normal Termux. Give it a minute on first run.
2. Want graphics? Open a session and install something with a GUI, e.g.
   `pkg install x11-repo && pkg install xfce4 xkb-config` (xkb data is what
   the X server needs for keyboards — without it X11 won't start).
3. Tap **X11** in the extra-keys bar (third row). First start takes a few
   seconds while the server spins up in-process.
4. Back in Termux, run e.g. `DISPLAY=:0 xfce4-session &`, flip back to X11.
5. The **‹** button top-left in X11 jumps straight back to the terminal.
   It leaves the X session running so returning is instant.

## where's the apk

Actions tab → newest "Build xermux" run → Artifacts:
- `xermux-universal-apk` — works everywhere, grab this one
- `xermux-arm64-apk` — most phones, smaller download

Notes that bite everyone:
- The artifact is a **.zip — unzip it first**, the apk is inside.
- Each CI build is signed with the repo test key, so builds update over each
  other fine. Coming from the old standalone Termux/X11 apps or the ancient
  xermux 1.x? Those are different signatures — uninstall first.
- `‹` keeps the X session alive on purpose (fast return). Done for the day?
  Swipe the app away / force-stop to fully stop the X thread.
- Long overnight sessions: consider exempting the app from battery
  optimization, otherwise Doze can pause your builds.

## what's inside

- `app/`, `terminal-emulator/`, `terminal-view/`, `termux-shared/` —
  termux-app, barely touched (see "xermux changes" below).
- `lorie/` — termux-x11's library module: native X server (built here with
  CMake/NDK) + MainActivity, prefs, input handling.
- `shell-loader/stub/` — compile-only stubs lorie needs. The actual
  shell-loader .deb flow is skipped: no second app, no app_process hop.

xermux changes (the whole fork diff, roughly):
- `XermuxApplication` (in `lorie/.../com/termux/x11/`) extends
  `TermuxApplication`: one app class running Termux init + X11 prefs,
  notification channel and server-broadcast handling.
- `CmdEntryPoint.startInProcess()` + `X11SelfStart`: boot the X server on
  the app's own thread instead of via the `termux-x11` shell script.
- Extra-keys default gains an `X11` row; tapping it opens MainActivity
  (`TermuxTerminalExtraKeys` intercepts it before it hits the shell).
- X11 layout gains a `‹` button (`backToTermux()`: finish, or explicit
  Termux intent when X11 is the task root).
- minSdk 21 → 24 (X11's floor), explicit `hardwareAccelerated` /
  `largeHeap=false`, gradle parallel+cache, faster daemon heap.
- `OPTIMIZATIONS.md`: 50 numbered battery/smoothness items, each pointing
  at the file that proves it.

## build it yourself

```
./gradlew assembleDebug
```

Needs JDK 17 + Android SDK (36 + 34 build-tools, NDK 29.0.14206865,
CMake 3.22.1 — the Actions workflow installs exactly this set). First build
compiles all of Xorg, so 15–40 minutes depending on the machine. Bootstrap
zips download during the build (checksummed, same as upstream).

## credits / license

- Terminal half: [termux/termux-app](https://github.com/termux/termux-app),
  GPLv3 (see `LICENSE.md`). Extra-keys, sessions, bootstrap logic all theirs.
- X11 half: [termux/termux-x11](https://github.com/termux/termux-x11),
  GPLv3 (see `LICENSE.termux-x11`). Server, viewer, input stack all theirs.

This repo is a fork combining both in one apk — same licenses apply.
Upstream docs live in `docs/` (termux) and the x11 README.
`OPTIMIZATIONS.md` lists every perf-relevant choice with receipts.
