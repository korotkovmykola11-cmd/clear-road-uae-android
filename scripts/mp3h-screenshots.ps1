param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("A", "C")]
    [string]$Variant
)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$device = "emulator-5554"
$shots = "C:\Users\korot\Pictures\mp3h_$Variant"
New-Item -ItemType Directory -Force -Path $shots | Out-Null
Add-Type -AssemblyName System.Drawing

function Tap([int]$x, [int]$y) { & $adb -s $device shell input tap $x $y | Out-Null }
function Back { & $adb -s $device shell input keyevent KEYCODE_BACK | Out-Null }
function ClearField { for ($i = 0; $i -lt 90; $i++) { & $adb -s $device shell input keyevent 67 | Out-Null } }
function GetUiXml {
    & $adb -s $device shell uiautomator dump /sdcard/ui.xml 2>$null | Out-Null
    & $adb -s $device shell cat /sdcard/ui.xml
}
function TapDropdown([string]$needle, [int]$minY = 550) {
    $xml = GetUiXml
    $pattern = 'text="[^"]*' + [regex]::Escape($needle) + '[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    foreach ($m in [regex]::Matches($xml, $pattern)) {
        $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
        if ($y -ge $minY) {
            $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
            Tap $x $y
            return "Tapped dropdown '$needle' at $x,$y"
        }
    }
    return "NO dropdown '$needle'"
}
function WaitTapDropdown([string]$needle, [int]$minY = 550, [int]$timeoutSec = 60) {
    for ($i = 0; $i -lt $timeoutSec; $i++) {
        $r = TapDropdown $needle $minY
        if ($r -notlike "NO*") { return "$r (after ${i}s)" }
        Start-Sleep -Seconds 1
    }
    return "NO dropdown '$needle' after ${timeoutSec}s"
}
function TapText([string]$needle) {
    $xml = GetUiXml
    $pattern = 'text="[^"]*' + [regex]::Escape($needle) + '[^"]*"[^>]*class="android.widget.TextView"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $m = [regex]::Match($xml, $pattern)
    if ($m.Success) {
        $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
        $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
        Tap $x $y
        return "Tapped '$needle' at $x,$y"
    }
    return "NO '$needle'"
}
function TapDestinationField {
    $xml = GetUiXml
    $edits = [regex]::Matches($xml, 'class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if ($edits.Count -ge 2) {
        $m = $edits[1]
        $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
        $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
        Tap $x $y
        return "Tapped destination EditText at $x,$y"
    }
    return TapByPattern 'text="Enter destination"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' "destination"
}
function TapByPattern([string]$pattern, [string]$label) {
    $xml = GetUiXml
    $m = [regex]::Match($xml, $pattern)
    if ($m.Success) {
        $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
        $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
        Tap $x $y
        return "Tapped $label at $x,$y"
    }
    return "NO $label"
}
function SaveShot([string]$name) {
    Start-Sleep -Seconds 5
    $rawRemote = "/sdcard/mp3h_raw.cap"
    $rawLocal = Join-Path $shots "tmp.cap"
    $path = Join-Path $shots $name
    & $adb -s $device shell screencap $rawRemote | Out-Null
    & $adb -s $device pull $rawRemote $rawLocal 2>$null | Out-Null
    if (-not (Test-Path $rawLocal)) {
        Write-Output "FAILED $name (no raw cap)"
        return
    }
    $bytes = [IO.File]::ReadAllBytes($rawLocal)
    $w = [BitConverter]::ToInt32($bytes, 0)
    $h = [BitConverter]::ToInt32($bytes, 4)
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
    $bd = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::WriteOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    [Runtime.InteropServices.Marshal]::Copy($bytes, 16, $bd.Scan0, ($w * $h * 4))
    $bmp.UnlockBits($bd)
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Remove-Item $rawLocal -Force -ErrorAction SilentlyContinue
    Write-Output "Saved $name ($((Get-Item $path).Length) bytes)"
}
function WaitForText([string]$needle, [int]$timeoutSec = 120) {
    for ($i = 0; $i -lt $timeoutSec; $i++) {
        $xml = GetUiXml
        if ($xml -match [regex]::Escape($needle)) { return "FOUND $needle after ${i}s" }
        Start-Sleep -Seconds 1
    }
    return "TIMEOUT waiting for $needle"
}

function EnsureRouteLoaded {
    for ($attempt = 1; $attempt -le 2; $attempt++) {
        Tap 540 496
        Start-Sleep -Seconds 1
        ClearField
        & $adb -s $device shell input text "Al%sNuaimiya"
        Start-Sleep -Seconds 25
        $origin = WaitTapDropdown "Al Nuaimiya Towers" 550 30
        if ($origin -like "NO*") { $origin = WaitTapDropdown "Nuaimiya Towers" 550 15 }
        Write-Output "Origin attempt $attempt : $origin"
        if ($origin -notlike "NO*") { break }
        Back
        Start-Sleep -Seconds 1
    }

    Tap 540 850
    Start-Sleep -Seconds 2

    Write-Output (TapDestinationField)
    Start-Sleep -Seconds 2
    ClearField
    Start-Sleep -Seconds 1
    & $adb -s $device shell input text "AAL%sGroup"
    Start-Sleep -Seconds 25
    $dest = WaitTapDropdown "AAL Group Ltd - Sharjah" 650 45
    Write-Output "Destination: $dest"
    Tap 540 400
    Start-Sleep -Seconds 2
}

Write-Output "=== MP-3H Variant $Variant ==="

& $adb -s $device shell am force-stop com.clearroad.app
Start-Sleep -Seconds 2
& $adb -s $device shell am start -n com.clearroad.app/.MainActivity
Start-Sleep -Seconds 8

EnsureRouteLoaded
Start-Sleep -Seconds 45
Write-Output (WaitForText "Why This Route" 30)
Tap 540 400
Start-Sleep -Seconds 1
& $adb -s $device shell input swipe 540 1600 540 700 350
Start-Sleep -Seconds 2

Write-Output (TapText "Why This Route")
Start-Sleep -Seconds 8
Write-Output (WaitForText "Route Details" 30)
& $adb -s $device shell input swipe 540 1800 540 900 350
Start-Sleep -Seconds 2
& $adb -s $device shell input swipe 540 1800 540 900 350
Start-Sleep -Seconds 2
Write-Output (WaitForText "Route preview" 30)
if ($Variant -eq "A") { SaveShot "A1_compact_normal_no_style.png" } else { SaveShot "C1_compact_hybrid_no_style.png" }

Write-Output (TapText "Explore route")
Start-Sleep -Seconds 10
Write-Output (WaitForText "Fit route" 30)
if ($Variant -eq "A") { SaveShot "A2_explore_normal_no_alternatives.png" } else { SaveShot "C2_explore_hybrid_no_alternatives.png" }

$r = TapText "Show Google alternatives"
if ($r -like "NO*") { Write-Output (TapText "Other Google alternatives") } else { Write-Output $r }
Start-Sleep -Seconds 8
if ($Variant -eq "A") { SaveShot "A3_explore_normal_with_alternatives.png" } else { SaveShot "C3_explore_hybrid_with_alternatives.png" }

Back
Start-Sleep -Seconds 3
Write-Output (WaitForText "Route Details" 20)
& $adb -s $device shell input swipe 540 2000 540 800 350
Start-Sleep -Seconds 2
Write-Output (TapText "Follow with MARSHIO")
Start-Sleep -Seconds 12
SaveShot "F1_follow_isolation_check.png"

Write-Output "Done variant $Variant -> $shots"
