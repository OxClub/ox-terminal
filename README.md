# Nexus Terminal

A real terminal emulator for Android. Every tab runs a full PTY shell directly on your device—no simulation, no fake output.

## Features

- **Real shells** – Bash, mksh, zsh or any installed shell via PTY (not simulated)
- **Multi-session tabs** – Switch between independent shell processes
- **File manager** – Browse, edit, archive, and share your files
- **Text editor** – Syntax highlighting, line numbers, large-file handling
- **Package manager** – Download and install packages from custom HTTPS repositories
- **Code runner** – Execute .py, .js, .sh, .rb and other scripts directly
- **Command reference** – Offline documentation for hundreds of commands
- **SSH profiles** – Saved connection configurations (passwords never stored)
- **Customization** – 6+ themes, font size, extra-keys toolbar, aliases, env vars
- **Process manager** – View running processes and resource usage
- **History & bookmarks** – Persist your commands and important locations
- **Settings export/import** – Back up and restore your configuration

## Architecture

```
app/src/main/
├── java/com/nexus/terminal/
│   ├── util/              # Logging, paths, clipboard, TSV codec
│   ├── data/              # Settings, history, secure storage
│   ├── terminal/          # PTY engine, sessions, shell clients
│   ├── files/             # File ops, archive handling, syntax highlighting
│   ├── pkg/               # Package manager (download, verify, extract)
│   └── ui/                # Compose screens and components
├── assets/
│   └── bin/               # Shell helper scripts (nxpkg, nexus-help)
└── res/                   # Icons, themes, strings
```

### Data Flow

1. **Shells** – Termux's `terminal-view` and PTY library handle the real shell process
2. **Sessions** – Registered in a global registry (survive activity rotation)
3. **Settings** – SharedPreferences (public) + EncryptedSharedPreferences (SSH profiles)
4. **Terminal storage** – `$PREFIX` = app-private; `/sdcard` = shared storage (on demand)

### Terminal Backend

The app uses **Termux's terminal-view** library, which provides:
- A real Unix PTY (`libutil.so` from libc)
- Full terminal emulation (xterm-256color)
- Text selection, scrollback, and rendering

No simulated shell output; all commands run in a real `fork()`'d process.

## Package Manager

### Repository Format

Repositories are HTTPS-hosted JSON files (`docs/REPOSITORY_FORMAT.md`):

```json
{
  "packages": [
    {
      "name": "python",
      "version": "3.11.2",
      "description": "Python 3 interpreter",
      "category": "Languages",
      "url": "https://repo.example.com/python-3.11.2.tar.gz",
      "sha256": "abc123...",
      "depends": []
    }
  ]
}
```

### Install Flow

1. Fetch index from configured repos
2. Resolve dependencies
3. Download with SHA-256 verification
4. Extract .zip or .tar.gz to `$PREFIX`
5. Record installed files (for removal)

**Local install** – Users can import .zip/.tar.gz files from device storage (fully offline).

## Permissions

| Permission | Why | When requested |
|---|---|---|
| INTERNET | For package repositories | Used only when you start a download |
| ACCESS_NETWORK_STATE | Check connectivity | Automatically checked when needed |
| FOREGROUND_SERVICE | Keep shells alive | Always active while sessions exist |
| READ/WRITE_EXTERNAL_STORAGE | /sdcard access | Only when you tap "Enable shared storage" |

**No analytics, no third-party trackers.** Network is used only for features you explicitly start.

## Known Limitations

### Android API Level

The app targets **API 28** (Android 9) deliberately:
- Android 10+ (API 29+) forbids executing files from app data directories
- Running installed packages requires this permission
- Workaround: Always use `$PREFIX` for installed files; don't try to exec from elsewhere

### UI & Rendering

- **No line-spacing setting** – renderer doesn't support it
- **No transparency** – terminal always opaque
- **System monospace only** – or a user-provided custom font file
- **No bundled fonts** – choose "monospace", "serif-monospace", or upload a .ttf

### History & Privacy

- **Password prompts ignored** – Commands typed at `password:`/`passphrase:` prompts are never saved
- **Sensitive patterns skipped** – `PASSWORD=`, `API_KEY=` etc. are never recorded
- **History is local** – Never synced or shared

### Sessions

- **Cannot survive process death** – When your app is killed by the OS, shell processes die with it
- **Recreated shells start in previous directories** – The shell history and open files are lost, but we remember where you were

## Build & Install

### From Source

**Prerequisites:**
- JDK 17+
- Android SDK (API 34, also API 28 sources for `compileSdk 34, targetSdk 28`)
- Gradle 8.7+ (wrapper is built from `gradle.properties`)

```bash
# Debug build (unsigned APK)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release build (requires signing config)
# Set env vars: KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
./gradlew assembleRelease
```

### GitHub Actions

The CI workflow (`.github/workflows/android-build.yml`) builds APKs automatically:

1. **On every push to `main`** – Builds debug APK
2. **Manual trigger** – Builds release APK (if keystore secrets configured)

**Download APK:**
- Go to **Actions** → latest build run
- Find **Artifacts** section
- Download `nexus-terminal-debug` (or `-release`)

### Install via `adb`

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
# or
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Development Setup

### First Run

1. **Onboarding** – New users see a 5-page introduction
2. **Session creation** – First session auto-created (or set `startup_behavior: dashboard`)
3. **Bootstrap** – App initializes `$PREFIX`, copies helper scripts, generates offline help

### Directory Structure

```
$HOME (app-private)
  ~/scripts/              # Your scripts (Runner screen)
  ~/.nexus_aliases        # Generated from Aliases settings
  
$PREFIX (app-private)
  bin/                    # Your executables
  lib/                    # Libraries (LD_LIBRARY_PATH)
  var/nx/                 # Package DB
    available.txt         # Cached package index
    installed.txt         # Installed packages
    installed.json        # Full manifest (for removal)
  share/nexus/help/       # Offline command help
  etc/                    # Config files
```

### Settings (Compose Observable)

All settings read from SharedPreferences; each read subscribes to changes:

```kotlin
AppSettings.fontSize = 14  // Triggers recomposition
AppSettings.themeId        // Reads + subscribes
AppSettings.exportJson()   // JSON export (sanitized)
AppSettings.importJson(json) // Import from backup
```

### Adding a New Screen

1. Create `ui/screens/MyScreen.kt`
2. Add route to `NexusApp.kt` NavHost
3. Navigation: `go("my_route")`
4. Back button: `back()` function parameter

## Testing

### Run Tests

```bash
# Unit tests (JVM)
./gradlew test

# Instrumented tests (device/emulator)
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

### Test Reports

- JVM: `app/build/reports/tests/test/index.html`
- Lint: `app/build/reports/lint-results.html`

## Release Build

### Sign Your APK

1. Create a keystore:
   ```bash
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nexus
   ```

2. Set GitHub Actions secrets:
   - `KEYSTORE_BASE64` – `base64 release.jks`
   - `KEYSTORE_PASSWORD` – your password
   - `KEY_ALIAS` – `nexus`
   - `KEY_PASSWORD` – your password

3. Go to **Actions** → **Build APK** → **Run workflow** → **Release** (manual dispatch)

## Troubleshooting

**Q: "No packages found" in Packages screen**
- A: No repository configured. Add one: Settings > Packages > Repositories. Use `https://raw.githubusercontent.com/...`.

**Q: Terminal is black, nothing appears**
- A: Session hasn't started yet. It starts when you first focus the TerminalView. Try typing or tapping the screen.

**Q: Can't execute a file from ~/something**
- A: Android 10+ blocks executing from app-private storage. Use `$PREFIX` for installed executables.

**Q: "Blocked unsafe path in archive"**
- A: Archive contained a path like `../etc/passwd` (zip slip). This is blocked for safety.

**Q: Build fails: "Could not find google()"**
- A: Gradle can't reach Maven Central / Google repos. Check internet; if behind proxy, configure `gradle.properties`.

## License

MIT (open source). See LICENSE file.

## Contributing

Bug reports and PRs welcome. Please test on real hardware (the emulator may behave differently).

---

**Made with Kotlin, Compose, and Termux's terminal-view.**
