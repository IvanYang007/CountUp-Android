# CountUp — Agent Guidelines & Instructions

## Emulator & Manual Testing Rule (CRITICAL)

When the user asks to **"show emulator"**, **"run in emulator"**, **"test manually"**, or **"show me in emulator"**:

1. **DO NOT** attempt blind `Start-Process emulator.exe` or `cmd.exe /c start ...` without the desktop specifier.
   - On Windows, agent shell processes execute in an isolated desktop (`exebox-...`), causing the emulator GUI window to spawn invisibly away from the user's screen.
2. **ALWAYS** run the pre-configured 1-try launcher script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\launch_emulator.ps1 -Deploy -OpenDialog
   ```
   This script:
   - Resolves the Android SDK and targets `countUp_api36`.
   - Checks if an emulator is already online (preventing redundant boot cycles).
   - If not running, spawns `emulator.exe` directly onto the user's primary interactive desktop (`WinSta0\Default`) via Win32 `CreateProcess`.
   - Waits for boot completion.
   - Builds `assembleDebug`, installs the APK, and launches `MainActivity`.
   - Taps to open and expand the dialog so the feature under review is instantly on screen.
