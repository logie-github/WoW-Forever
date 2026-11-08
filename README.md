# WoW Forever

Android launcher for **World of Warcraft: Forever** on Snapdragon devices. Package: `com.logie.wowforever`.

Runs Blizzard's Windows ARM64 WoW client under Wine with DXVK and a Turnip Vulkan driver. The game binaries are not modified.

> Unofficial. Not affiliated with Blizzard Entertainment. Requires your own Battle.net account with WoW Forever access. Contains no Blizzard game files.

## Origin

Source imported from [jaredgei/wow-forever-android](https://github.com/jaredgei/wow-forever-android) (GPL-3.0), itself derived from [GameNative](https://github.com/utkarshdalal/GameNative). This repository is maintained independently and is licensed GPL-3.0 (see `LICENSE`, `THIRD_PARTY_NOTICES`).

Changes from the imported source:

| Area | Change |
| :--- | :--- |
| Application ID | `app.wowforever` → `com.logie.wowforever` (separate install, separate private data) |
| Branding | Launcher icon, round icon, adaptive icon, TV banner, store icon replaced (`branding/`) |
| Setup screen | Blizzard launcher's modern home layout |
| In-game touch | Blizzard launcher's WoW gestures |
| In-app updater | Checks `logie-github/WoW-Forever` releases only |
| Versioning | Restarted at 1.0.0 |

## Data handling

Audit of the imported source:

- **Credentials:** stored only on-device in SharedPreferences, password AES-encrypted with an Android Keystore key. Written to `_forever_/login.txt` at boot for the client's auto-login, then zeroed and deleted once the client has started.
- **No analytics, telemetry, or crash reporting SDKs.** Crash logs are written locally only.
- **Outbound network requests:**

| Host | Purpose | Data sent |
| :--- | :--- | :--- |
| `us.patch.battle.net`, Blizzard CDN hosts | Version check, client and index downloads | None (plain GET) |
| `downloads.gamenative.app` | Wine image and runtime component downloads | None (plain GET) |
| `raw.githubusercontent.com` (longjunyu2/winlator) | Winlator content metadata | None (plain GET) |
| `api.github.com` (logie-github/WoW-Forever) | App update check | None (plain GET) |
| Blizzard login/game servers | The game client itself | Your account login (by the game) |

## Building

Requires JDK 17 and the Android SDK.

```bash
tools/fetch_components.sh              # Proton, DXVK, Turnip bundles (SHA-256 verified)
./gradlew assembleModernRelease
```

Output: `app/build/outputs/apk/modern/release/`.

The GitHub Actions workflow `.github/workflows/build.yml` builds the APK on every push to `main` and on manual dispatch, and publishes a release when a `v*` tag is pushed.

### Signing

Without a keystore, builds are signed with a debug key, which changes per CI runner, so updates will not install over a previous build. For stable signing, add these repository secrets:

| Secret | Value |
| :--- | :--- |
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

## Look

The setup screen uses the modern home layout of [logie-github/Blizzard](https://github.com/logie-github/Blizzard) (`ui/ModernHome.kt`): the World of Warcraft art fills the screen, drifting, with snow in the art's own colours; a panel down the left holds the readiness list and any notices (install, game update, app update, errors); the game's name in Cinzel, its tags, **Play** and **•••** sit at the bottom right, with the box art beside them on tall screens. The gear and ••• open one sheet: region, resolution, touch controls, the Battle.net login and the game folder. Fonts: Cinzel and Jost (SIL OFL, `fonts/`).

## Touch controls

In game, the Blizzard launcher's World of Warcraft gestures (`app/src/main/java/app/gamenative/wowinput/`, from Blizzard's `wowinput/`) replace Winlator's touchpad. Switch back to the touchpad in ••• → Touch controls.

| Gesture | Where | Does |
| :--- | :--- | :--- |
| Swipe and hold | Left half | Walk, like a stick: W/S forward and back, Q/E sideways, diagonals between |
| Drag | Right half | Turn the camera and the character (right button held) |
| Tap | Anywhere | Click; talks, loots, gathers where the game's cursor offers it (a right click follows) |
| Double tap | Enemy / action bars / elsewhere | Attack / press again / jump |
| Hold, lift | Anywhere | Right click |
| Hold, move | Anywhere | Drag with the left button (items, frames) |
| Two fingers | Pinch / tap / swipe up | Zoom / target nearest enemy (Tab) / chat with the keyboard |
| Three-finger tap | | Keyboard |
| Four-finger tap | | The in-game menu |

On-screen controls, when shown, still take touches first; a mouse or trackpad goes to the game as before. The Blizzard version's private-realm signals (what a tap selected, spellbook areas, cutscenes) and its 3.3.5a character-creation detection don't exist on Blizzard's live servers, so taps rely on the game's cursor alone.

## Using the app

1. Install the APK and grant **All files access**.
2. Default game folder is `/storage/emulated/0/WoW Forever/`. **Download & Install** fetches the ARM64 client and archive indexes (~250 MB); remaining game data streams in during play. An existing install copied from a PC/Mac (`.build.info` + `Data/`) is detected automatically.
3. **•••** → **Configure login** stores Battle.net credentials. Subsequent launches boot straight into the game and sign in automatically. Authenticator codes are entered by hand.
4. To return to the setup screen, press Back or hold **Start + Select + L2 + R2** during the loading splash.

Requirements: Snapdragon 8-series (Adreno 7xx/8xx), Android 10+, ~60 GB free for game data.
