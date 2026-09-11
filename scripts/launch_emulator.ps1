<#
.SYNOPSIS
    Reliably launches the Android Emulator on the user's primary interactive desktop (WinSta0\Default)
    in one try, avoiding headless or isolated agent desktops, and optionally builds, installs, and launches the app.

.PARAMETER Avd
    The name of the AVD to launch (defaults to countUp_api36, or auto-detected).

.PARAMETER Deploy
    If set, builds assembleDebug, installs the APK, and starts MainActivity.

.PARAMETER OpenDialog
    If set, sends input taps to open and expand the "New Item" dialog for immediate testing.
#>
param(
    [string]$Avd = "",
    [switch]$Deploy,
    [switch]$OpenDialog
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

# 1. Resolve Android SDK Path
$sdkDir = $null
if (Test-Path "local.properties") {
    $line = Get-Content "local.properties" | Where-Object { $_ -match "^sdk\.dir\s*=" } | Select-Object -First 1
    if ($line) {
        $sdkDir = ($line -replace "^sdk\.dir\s*=\s*", "").Trim().Replace("\\", "\")
    }
}
if (-not $sdkDir -or -not (Test-Path $sdkDir)) {
    if (Test-Path "D:\Android\Sdk") { $sdkDir = "D:\Android\Sdk" }
    elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") { $sdkDir = "$env:LOCALAPPDATA\Android\Sdk" }
    elseif ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { $sdkDir = $env:ANDROID_HOME }
}

if (-not $sdkDir -or -not (Test-Path $sdkDir)) {
    Write-Error "Android SDK not found. Please specify sdk.dir in local.properties."
}

$adb = Join-Path $sdkDir "platform-tools\adb.exe"
$emulatorExe = Join-Path $sdkDir "emulator\emulator.exe"
$emulatorDir = Join-Path $sdkDir "emulator"

if (-not (Test-Path $adb) -or -not (Test-Path $emulatorExe)) {
    Write-Error "adb.exe or emulator.exe missing in SDK directory: $sdkDir"
}

# 2. Resolve AVD Name
if (-not $Avd) {
    $avdList = & $emulatorExe -list-avds
    if ($avdList -contains "countUp_api36") {
        $Avd = "countUp_api36"
    } elseif ($avdList.Count -gt 0) {
        $Avd = $avdList[0]
    } else {
        Write-Error "No Android Virtual Devices (AVDs) found. Run 'emulator -list-avds'."
    }
}

Write-Host "Target AVD: $Avd"

# 3. Check if emulator is already running
$runningDevices = & $adb devices
$emulatorOnline = $false
if ($runningDevices -match "emulator-\d+\s+device") {
    Write-Host "Emulator is already running and connected via ADB."
    $emulatorOnline = $true
} elseif ($runningDevices -match "emulator-\d+\s+offline") {
    Write-Host "Emulator is running but currently booting/offline. Waiting for online..."
} else {
    Write-Host "Launching emulator on user's primary interactive desktop (WinSta0\Default)..."
    
    # C# Win32 Process Launcher with lpDesktop = WinSta0\Default
    $launcherSource = @"
using System;
using System.Runtime.InteropServices;

public class DesktopLauncher {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct STARTUPINFO {
        public int cb;
        public string lpReserved;
        public string lpDesktop;
        public string lpTitle;
        public int dwX;
        public int dwY;
        public int dwXSize;
        public int dwYSize;
        public int dwXCountChars;
        public int dwYCountChars;
        public int dwFillAttribute;
        public int dwFlags;
        public short wShowWindow;
        public short cbReserved2;
        public IntPtr lpReserved2;
        public IntPtr hStdInput;
        public IntPtr hStdOutput;
        public IntPtr hStdError;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct PROCESS_INFORMATION {
        public IntPtr hProcess;
        public IntPtr hThread;
        public int dwProcessId;
        public int dwThreadId;
    }

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    public static extern bool CreateProcess(
        string lpApplicationName,
        string lpCommandLine,
        IntPtr lpProcessAttributes,
        IntPtr lpThreadAttributes,
        bool bInheritHandles,
        uint dwCreationFlags,
        IntPtr lpEnvironment,
        string lpCurrentDirectory,
        ref STARTUPINFO lpStartupInfo,
        out PROCESS_INFORMATION lpProcessInformation
    );

    [DllImport("kernel32.dll")]
    public static extern bool CloseHandle(IntPtr hObject);

    public static int StartOnInteractiveDesktop(string exePath, string arguments, string workingDirectory) {
        STARTUPINFO si = new STARTUPINFO();
        si.cb = Marshal.SizeOf(si);
        si.lpDesktop = @"WinSta0\Default";

        PROCESS_INFORMATION pi = new PROCESS_INFORMATION();
        string cmd = "\"" + exePath + "\" " + arguments;
        bool success = CreateProcess(null, cmd, IntPtr.Zero, IntPtr.Zero, false, 0, IntPtr.Zero, workingDirectory, ref si, out pi);
        if (!success) {
            int err = Marshal.GetLastWin32Error();
            throw new InvalidOperationException("Failed to launch process on interactive desktop. Error code: " + err);
        }
        CloseHandle(pi.hThread);
        CloseHandle(pi.hProcess);
        return pi.dwProcessId;
    }
}
"@
    if (-not ([System.Management.Automation.PSTypeName]'DesktopLauncher').Type) {
        Add-Type -TypeDefinition $launcherSource
    }

    $pid = [DesktopLauncher]::StartOnInteractiveDesktop($emulatorExe, "-avd $Avd", $emulatorDir)
    Write-Host "Spawned emulator process PID: $pid on WinSta0\Default"
}

# 4. Wait for boot completion
Write-Host "Waiting for device boot completion..."
& $adb wait-for-device
$bootDone = $false
$retries = 30
while (-not $bootDone -and $retries -gt 0) {
    $res = & $adb shell "getprop sys.boot_completed" 2>$null
    if ($res -and $res.Trim() -eq "1") {
        $bootDone = $true
        break
    }
    Start-Sleep -Seconds 1
    $retries--
}

if (-not $bootDone) {
    Write-Warning "Emulator is connected, but boot_completed flag not 1 yet. Continuing..."
} else {
    Write-Host "Emulator boot completed successfully!"
}

# 5. Build, Install, and Launch if -Deploy requested
if ($Deploy) {
    Write-Host "Building and deploying debug APK..."
    $gradlew = if ($IsWindows -or $env:OS -match "Windows") { ".\gradlew.bat" } else { "./gradlew" }
    & $gradlew assembleDebug
    
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        Write-Host "Installing $apk..."
        & $adb install -r $apk
        Write-Host "Launching com.countup.app/.MainActivity..."
        & $adb shell am start -n com.countup.app/.MainActivity
    } else {
        Write-Error "APK not found at $apk"
    }
}

# 6. Open and Expand Dialog if -OpenDialog requested
if ($OpenDialog) {
    Write-Host "Expanding New Item dialog for manual testing..."
    Start-Sleep -Milliseconds 1200
    & $adb shell input tap 964 260
    Start-Sleep -Milliseconds 800
    & $adb shell input tap 834 895
}

Write-Host "Emulator is ready and visible on your desktop!" -ForegroundColor Green
