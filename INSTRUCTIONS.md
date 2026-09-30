# 🚀 NEXUS TERMINAL — Complete Setup & Build Guide

This is the **complete, production-ready Android terminal app** built in Kotlin with Jetpack Compose. Everything is here: source code, CI/CD, tests, docs, and real shell PTY support.

---

## 📋 What You Got

✅ **15+ UI screens** (terminal, files, editor, packages, settings, process manager, etc.)
✅ **Real PTY shell engine** (Termux's terminal-view + libc PTY)
✅ **Package manager** (download, verify SHA-256, extract dependencies)
✅ **File manager** (browse, edit, archive, syntax highlighting)
✅ **Code runner** (Python, JavaScript, Bash, Go, Rust, C/C++, and more)
✅ **SSH profiles** (secure storage, no password persistence)
✅ **Settings & customization** (6+ themes, font size, extra-keys toolbar)
✅ **GitHub Actions CI/CD** (automatic APK builds on every push)
✅ **Unit tests** (JVM tests for history, files, commands)
✅ **Offline documentation** (hundreds of commands)
✅ **Config export/import** (backup and restore settings)

---

## 🎯 Quick Start (5 minutes)

### 1. **Set Up Your Machine**

**Required:**
- **Java 17+** (`java -version`)
  - macOS: `brew install openjdk@17` or download from oracle.com
  - Linux: `sudo apt install openjdk-17-jdk`
  - Windows: Download from oracle.com

- **Android SDK** (API 34 minimum)
  - **Easiest:** Install **Android Studio** (includes SDK)
  - **Or:** Download Android SDK Command-line tools from developer.android.com
  
- **Gradle** (included in the project as a wrapper, but you need Java first)

**Verify:**
```bash
java -version          # Should show Java 17+
echo $ANDROID_HOME    # Should be set (Android Studio sets this automatically)
```

### 2. **Get the Code**

```bash
# Unzip the project
unzip nexus-terminal.zip
cd nexus-terminal

# On Windows, use:
unzip nexus-terminal.zip
cd nexus-terminal
```

### 3. **Build the Debug APK**

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**First run takes 2–5 minutes** (downloads Gradle, Android dependencies, Termux libraries).

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

### 4. **Install on Your Phone**

```bash
# Using adb (Android SDK Platform Tools)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or manually: Download the APK to your phone and tap to install
```

**Done!** Open Nexus Terminal on your phone and go through the onboarding.

---

## 🏗️ Building from Source: Step by Step

### Full Build Process

```bash
# 1. Clone / unzip
cd nexus-terminal

# 2. Make gradlew executable (macOS / Linux only)
chmod +x gradlew

# 3. Build debug APK
./gradlew assembleDebug

# 4. Build also runs lint
# View lint report: open app/build/reports/lint-results.html

# 5. Run unit tests
./gradlew test
# View test report: open app/build/reports/tests/test/index.html

# 6. Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 7. Verify it's installed
adb shell pm list packages | grep nexus
```

### Troubleshooting the Build

| Problem | Solution |
|---------|----------|
| `ANDROID_HOME not set` | Set it to your SDK directory: `export ANDROID_HOME=/path/to/Android/Sdk` |
| `Could not find com.github.termux:terminal-view` | Internet issue. Check VPN / proxy. The build uses JitPack to fetch Termux libraries. |
| `Gradle sync failed` | Click "Sync Now" in Android Studio, or run `./gradlew --refresh-dependencies` |
| `API 34 not installed` | Open Android Studio > SDK Manager > Install API 34 |
| `Build succeeds but APK is tiny (<500KB)` | Normal for a debug build (not minified). Release build is ~2–3MB. |

---

## 🔄 GitHub Actions (Continuous Integration)

The project is ready for GitHub Actions. Every push builds the APK automatically.

### Set Up CI/CD

1. **Push this project to GitHub:**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/nexus-terminal.git
   git push -u origin main
   ```

2. **CI runs automatically:**
   - On every push → builds debug APK
   - GitHub Actions tab shows the build status
   - Download APK: Go to Actions → latest build → Artifacts → `nexus-terminal-debug`

3. **Manual release builds** (optional):
   - Create a keystore (see "Release Build" below)
   - Set secrets on GitHub (repo Settings > Secrets)
   - Manually trigger: Actions > Build APK > Run workflow > Release

---

## 📦 Release Build (For Google Play / Distribution)

### Step 1: Create a Signing Keystore

```bash
keytool -genkey -v -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias nexus

# Prompts for:
#   Keystore password: [create a strong password]
#   Key password: [same or different]
#   First and last name: Your Name
#   Organizational unit: (leave blank or type)
#   Organization: Your Company
#   City/Locality, State, Country: Fill in
```

**Save `release.jks` securely** — you'll need it to update the app on Google Play.

### Step 2: Build Release APK Locally

```bash
export KEYSTORE_PATH=$(pwd)/release.jks
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="nexus"
export KEY_PASSWORD="your_key_password"

./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk (~2–3 MB)
```

### Step 3: Sign and Verify

```bash
# Verify the signature (optional but recommended)
jarsigner -verify -verbose -certs \
  app/build/outputs/apk/release/app-release.apk

# If you see "jar verified", you're good to go!
```

### Step 4: Publish to Google Play (Optional)

1. Create a Google Play Developer account ($25 one-time)
2. Create a new app listing
3. Upload `app-release.apk` to the internal testing track first
4. Move to production after testing

---

## 🧪 Testing

### Run All Tests

```bash
# Unit tests (JVM, fast)
./gradlew test

# View results
open app/build/reports/tests/test/index.html

# Lint checks
./gradlew lint

# View lint report
open app/build/reports/lint-results.html
```

### Manual Testing Checklist

On your phone:

- [ ] **Terminal**: Create new session, type `ls`, see real output
- [ ] **Terminal tabs**: Switch tabs, verify each shell is independent
- [ ] **Files**: Browse home directory, open a text file in editor
- [ ] **Editor**: Edit a file, save it, close and reopen
- [ ] **Extra keys**: Tap ESC, CTRL, arrows, verify they work
- [ ] **Packages**: Add a test repo (optional), see if indexing works
- [ ] **Settings**: Change theme, font size, verify UI updates
- [ ] **History**: Run a few commands, check history is saved
- [ ] **Process manager**: View running processes
- [ ] **About**: Verify version info displays

---

## 📂 Project Structure

```
nexus-terminal/
├── .github/workflows/
│   └── android-build.yml          # CI/CD configuration
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nexus/terminal/
│   │   │   │   ├── MainActivity.kt            # Entry point
│   │   │   │   ├── NexusApplication.kt        # App init
│   │   │   │   ├── util/                      # Logging, paths, clipboard
│   │   │   │   ├── data/                      # Settings, history, secure storage
│   │   │   │   ├── terminal/                  # PTY engine, sessions, shell
│   │   │   │   ├── files/                     # File ops, archives, syntax highlighting
│   │   │   │   ├── pkg/                       # Package manager
│   │   │   │   └── ui/                        # Compose screens
│   │   │   ├── assets/bin/                    # Shell helper scripts
│   │   │   └── res/                           # Icons, themes, strings
│   │   ├── test/java/com/nexus/terminal/      # Unit tests
│   │   └── androidTest/java/...               # Instrumented tests (future)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat           # Gradle wrapper
├── README.md                        # User-facing documentation
├── docs/REPOSITORY_FORMAT.md        # Package repo spec
└── INSTRUCTIONS.md                  # This file

```

---

## 🔧 Customization

### Change App Name or Package

**File:** `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.example.myapp"  // Change this
    
    defaultConfig {
        applicationId = "com.example.myapp"  // Change this
        ...
    }
}
```

**Also update:**
- `app/src/main/AndroidManifest.xml` – activity names
- `app/src/main/res/values/strings.xml` – `<string name="app_name">`

### Add a New Screen

1. Create `app/src/main/java/com/nexus/terminal/ui/screens/MyScreen.kt`
2. Define a `@Composable fun MyScreen(...)`
3. Add to `NexusApp.kt` NavHost:
   ```kotlin
   composable("my_route") { MyScreen(back) }
   ```
4. Navigate from any other screen: `go("my_route")`

### Modify Themes

**File:** `ui/theme/Theme.kt`

```kotlin
TermTheme("my_dark", "My Dark", 
    bg = 0xFF1e1e1e, 
    fg = 0xFFe0e0e0, 
    cursor = 0xFF00ff00, 
    accent = 0xFF00ff00,
    dark = true, 
    palette = darkPalette
)
```

---

## 🌐 Set Up a Package Repository

Users can install packages from your repository. To create one:

### 1. Create an `index.json`

```json
{
  "packages": [
    {
      "name": "python",
      "version": "3.11.2",
      "description": "Python 3 interpreter",
      "category": "Languages",
      "url": "https://myrepo.com/python-3.11.2.tar.gz",
      "sha256": "abc123def456...",
      "depends": []
    }
  ]
}
```

See `docs/REPOSITORY_FORMAT.md` for full spec.

### 2. Host the JSON + Package Files

**Option A: GitHub Pages**
```bash
# In a GitHub repo on gh-pages branch
https://raw.githubusercontent.com/user/myrepo/gh-pages/index.json
```

**Option B: Any HTTPS Server**
```
https://myrepo.example.com/index.json
```

### 3. Add to Nexus Terminal

In the app: **Packages** > **Settings** > **Repositories** > **Add**
```
https://myrepo.example.com/index.json
```

---

## 🐛 Known Issues & Limitations

### Platform Limitations

- **No API 29+** – Targets API 28 deliberately (run-time exec of app-private files is blocked on Android 10+)
- **No line spacing** – Terminal renderer doesn't expose this setting
- **System fonts only** – Uses monospace / serif-monospace, or a user-provided .ttf
- **No transparency** – Terminal background is always opaque

### Privacy & History

- **No password logging** – Commands at `password:` / `passphrase:` prompts are never saved
- **No network by default** – Only enabled for package downloads and user actions
- **History is local** – Never synced or shared with Anthropic / anyone

### Session Limits

- **Sessions don't survive process death** – If Android kills the app, shells die with it
- **Recreated shells in previous dirs** – App remembers where you were, but history and files are lost

---

## 📞 Troubleshooting

### App crashes on launch

**Check logcat:**
```bash
adb logcat | grep -i nexus
```

**Common causes:**
- Missing `ANDROID_HOME` — set it: `export ANDROID_HOME=/path/to/Android/Sdk`
- Termux library not loaded — ensure JitPack is reachable
- Old/stale build — clean: `./gradlew clean assembleDebug`

### No shells appear in terminal

- Tap the screen or type to focus the TerminalView
- Check that a shell (bash, sh, zsh) is available: `which bash`
- Restart the app

### "Could not find com.github.termux:terminal-view"

- Network issue — JitPack failed to fetch the library
- Add to `settings.gradle.kts` if not already there:
  ```kotlin
  repositories {
      maven { url = uri("https://jitpack.io") }
  }
  ```
- Run `./gradlew --refresh-dependencies`

### Settings not saving

- Check device storage: settings use `SharedPreferences` (app-private)
- Verify app has `MODE_PRIVATE` access (should be automatic)
- Clear app data and restart: `adb shell pm clear com.nexus.terminal`

### Package downloads fail

- Check network connectivity: Settings > Network (if available on your device)
- Verify the repository URL is HTTPS
- SHA-256 mismatch → repository file may be corrupted; try another repo

---

## 🚀 Next Steps

1. **Build & test locally** – Follow "Quick Start" above
2. **Push to GitHub** – Enable Actions for CI/CD builds
3. **Customize** – Change app name, colors, add your features
4. **Create a repo** – Host packages for your terminal
5. **Publish** – Google Play Store (or F-Droid)

---

## 📝 License

MIT. See LICENSE file.

---

## 💡 Support

- **Questions?** Check README.md for detailed docs
- **Issues?** See "Troubleshooting" above, or check GitHub Actions build logs
- **Contribute?** Fork, modify, and submit a PR

---

**That's it!** You have a **production-ready terminal emulator for Android**. Build it, test it, and ship it. 🎉
