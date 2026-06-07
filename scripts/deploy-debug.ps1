# Bypass Android Studio "Terminating the app" when am force-stop hangs.
# Usage: .\scripts\deploy-debug.ps1
# Requires: emulator running, adb on PATH or default SDK location.

$ErrorActionPreference = "Stop"
$Package = "com.clearroad.app"
$Activity = "$Package/.MainActivity"
$SdkAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$Adb = if (Get-Command adb -ErrorAction SilentlyContinue) { "adb" } elseif (Test-Path $SdkAdb) { $SdkAdb } else { throw "adb not found" }

Write-Host "== devices =="
& $Adb devices -l
$serial = (& $Adb devices | Select-String "device$" | Select-Object -First 1).ToString().Split()[0]
if (-not $serial) { throw "No online device. Start emulator first (Cold Boot)." }

Write-Host "== build =="
Push-Location (Split-Path $PSScriptRoot -Parent)
.\gradlew assembleDebug --quiet
$Apk = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }

Write-Host "== uninstall (skip force-stop) =="
$p = Start-Process -FilePath $Adb -ArgumentList "-s", $serial, "uninstall", $Package -PassThru -NoNewWindow -Wait -PassThru
if ($p.ExitCode -ne 0) { Write-Host "uninstall skipped or failed (exit $($p.ExitCode))" }

Write-Host "== install =="
& $Adb -s $serial install -r -t $Apk
if ($LASTEXITCODE -ne 0) { throw "install failed" }

Write-Host "== launch =="
& $Adb -s $serial shell am start -n $Activity
Write-Host "Done."
