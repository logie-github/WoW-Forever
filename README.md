# WoW Forever

An Android app that plays **World of Warcraft: Forever** through Blizzard's own Battle.net app, on Winlator
(Wine, Box64 and an X server), set up like logie-github/Blizzard and YeaBwoi's Warcraft app.

| | |
| :--- | :--- |
| Package | `com.logie.wowforever` |
| Releases | this repository's GitHub releases, `WoW Forever 2.0.N` (`wowforever-build-N`), one per push |
| Runtime | Winlator 11.2 (`brunodev85/winlator-app` `a030f55`), built into the app (`scripts/prepare-winlator.sh`) |

> Unofficial. Not affiliated with or endorsed by Blizzard Entertainment or the Winlator project. You need your
> own Battle.net account with WoW Forever. The app and this repository contain no Blizzard files.

## Blizzard's terms first

The app is built around Blizzard's End User License Agreement (last revised March 21, 2024), section 1.A
(the account) and 1.C (license limitations):

| Rule (EULA) | What the app does |
| :--- | :--- |
| No bots or automated control (1.C.2.b); no unauthorized software that changes or facilitates gameplay (1.C.2.d) | **One touch is one input**: one click, one key, one held key or button, as a mouse and keyboard give them. Nothing is chained, repeated, timed or done on the player's behalf. The movement stick holds one key at a time. |
| No hacks: no accessing or modifying the Platform's software (1.C.2.c); no reverse engineering or modifying (1.C.1) | Blizzard's programs run as Blizzard ships them. The app writes no game file, no `Config.wtf`, no `.build.info`, and patches nothing. |
| No unauthorized software reading the game's information (1.C.6) | The app reads nothing from the game. It looks only at the X window's title (to switch the touch layout) and the mouse cursor's picture the X server draws (to tell a talk/loot cursor from the plain arrow). |
| No intercepting, emulating or redirecting Blizzard's communications (1.C.8, 1.C.9) | The app never talks to Blizzard's servers. Battle.net and the game connect as they do on a PC. The one download the app makes is Blizzard's own Battle.net installer from Blizzard's own link. |
| Keep the login confidential (1.A.5) | The app never sees, stores or types a saved password. You sign in to Battle.net yourself; Battle.net keeps the sign-in ("Remember me"). |
| The game is obtained and run through Blizzard's Platform | Battle.net installs, updates and starts World of Warcraft. The app only asks Battle.net to start the game, with Battle.net's own `--exec` option. |

## First launch

1. Install the APK from the latest release.
2. Open the app and tap **Install Battle.net**. The first time, the app installs Winlator's Windows (a few minutes),
   downloads Blizzard's Battle.net installer from Blizzard, and runs it.
3. In Battle.net: sign in (the keyboard is a three-finger tap), then install **World of Warcraft Forever**, as on a PC.
4. Leave (Back twice → Leave). From now on **Play** starts the game through Battle.net, and opening the app starts
   Play by itself after a 3-second countdown (**Stay here** stops it).

**Open Battle.net** (beside Play, or the switch in Settings) goes back to the launcher: for updates, another account,
or Battle.net's own settings.

## Windows settings the app makes

Battle.net checks Windows for things Wine's Windows doesn't have. The app sets them in its own Windows' registry
before Wine starts (Windows settings, not Blizzard files):

| Setting | Why |
| :--- | :--- |
| The Secondary Logon service (`seclogon`), enabled | Battle.net's installer stops with "Battle.net requires the Windows Secondary Logon service to be enabled" (BLZBNTBTS00000025) without it; Wine has no such service (Wine bug 39261). |

## Controls

Every touch is one input. The layout follows the window on top.

**In World of Warcraft**

| Gesture | Where | Does |
| :--- | :--- | :--- |
| Swipe and hold | Left half | Walk: one key at a time, W forward, S back, Q and E sideways |
| Drag | Right half | Turn the camera and the character (right button held) |
| Tap | Anywhere | One left click |
| Double tap | On what the first tap found / action bars / elsewhere | The second tap: one right click (attack, talk, loot) / one more press / one jump |
| Hold, then lift | Anywhere | One right click |
| Hold, then move | Anywhere | Drag with the left button (items, windows) |
| Two fingers | Pinch / tap / swipe up | Zoom (mouse wheel) / Tab / Enter (chat) and the keyboard |

**In Battle.net and everything else**

| Gesture | Does |
| :--- | :--- |
| Tap | One left click |
| Hold, then lift | One right click |
| Drag | Drag with the left button |
| Two fingers up or down | Scroll |

**Both:** three-finger tap = the keyboard (a prompt: what you type is typed in, one key per character; "Hide what
I type" for passwords), four-finger tap or Back twice = the menu (Keyboard, Backspace, Escape, Leave), Back = Escape.
A mouse, keyboard or controller works as on a PC.

## Settings (the gear or •••)

| Setting | |
| :--- | :--- |
| Open Battle.net | Play opens Battle.net instead of starting the game |
| Start on open | Play starts by itself when the app opens |
| Windows screen | 1280×720, 1600×900 or 1920×1080, scaled to the phone's screen |
| Box64 | Performance (Winlator's default) or Stability (if Battle.net or the game crashes) |
| Battle.net game code | The code in `--exec="launch <code>"`, `wowf` by default. If the game doesn't start by itself, Battle.net opens: press Play there. |
| Run the installer again | Blizzard's Battle.net installer, again |

## What is and isn't tested

- Tested in CI: the touch layouts (unit tests for both, and that the stick never holds two keys), the app builds,
  Winlator's patches apply.
- Checked by hand: Blizzard's installer link answers and serves Battle.net's 32-bit Windows installer.
- **Not tested on a phone**: Battle.net's window under Box64, the game under Box64, and whether
  `--exec="launch wowf"` starts WoW Forever (the code isn't documented by Blizzard). Battle.net installs the 64-bit
  Windows client, which Box64 translates: expect it to be slower than a native client.

## How it's built

| Path | |
| :--- | :--- |
| `winlator-version.env` | Winlator's repository and commit, the app's package and the runtime's path |
| `patches/winlator/` | The app's changes to Winlator: its runtime in the app's folder, installed without Winlator's screens, the app's touch controls and loading screen over the game screen, Back |
| `scripts/prepare-winlator.sh` | Fetches Winlator, applies the patches, copies it into `app/winlator` (git-ignored), rewrites the runtime's path (`scripts/winlator-rewrite.py`) |
| `app/src/main/java/com/logie/wowforever/` | The launcher (`Home`, `MainActivity`), the runtime (`Runtime`), the touch controls (`GestureView`, `input/`) |
| `branding/` | The icon |
| `fonts/` | Cinzel and Jost licenses (SIL OFL) |

```sh
scripts/prepare-winlator.sh          # needs python3-zstandard
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

JDK 21, Android SDK 35, NDK 24.0.8215888, CMake 3.22.1.

## License

GPL-3.0. Winlator is LGPL-2.1. World of Warcraft and Battle.net are Blizzard Entertainment's.
