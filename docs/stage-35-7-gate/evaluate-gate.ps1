param(
    [Parameter(Mandatory = $true)]
    [string]$CapturePath
)

function Evaluate-Capture {
    param([string]$JsonPath)

    $name = Split-Path $JsonPath -Leaf
    $data = Get-Content $JsonPath -Raw | ConvertFrom-Json
    $rows = @()

    for ($i = 0; $i -lt $data.routes.Count; $i++) {
        $leg = $data.routes[$i].legs[0]
        $base = [int]$leg.duration.value
        $hasField = $null -ne $leg.duration_in_traffic
        $traffic = if ($hasField) { [int]$leg.duration_in_traffic.value } else { $base }
        $delay = [Math]::Max(0, $traffic - $base)
        $ratio = if ($base -gt 0) { [Math]::Round($delay / $base, 4) } else { 0.0 }
        $rows += [PSCustomObject]@{
            RouteIndex       = $i
            BaseSec          = $base
            TrafficSec       = $traffic
            HasTrafficField  = $hasField
            DelaySec         = $delay
            DelayRatio       = $ratio
        }
    }

    $delayActive = @($rows | Where-Object { $_.DelaySec -gt 0 }).Count
    $total = $rows.Count
    $pct = if ($total -gt 0) { [Math]::Round(100.0 * $delayActive / $total, 1) } else { 0.0 }
    $maxRatio = ($rows | Measure-Object -Property DelayRatio -Maximum).Maximum
    $minRatio = ($rows | Measure-Object -Property DelayRatio -Minimum).Minimum
    $spread = [Math]::Round($maxRatio - $minRatio, 4)
    $trafficSource =
        if (@($rows | Where-Object { -not $_.HasTrafficField }).Count -gt 0) { "FALLBACK" }
        elseif (@($rows | Where-Object { $_.HasTrafficField }).Count -eq $total) { "GOOGLE" }
        else { "MIXED" }

    $gateRoutePass = $pct -ge 50.0
    $gateContribution = if ($gateRoutePass) { "counts toward PASS" } else { "counts toward FAIL/PARTIAL" }

    Write-Output ""
    Write-Output "=== Stage 35.7 Gate Evaluate: $name ==="
    $rows | Format-Table -AutoSize
    Write-Output "delay_gt_zero: $delayActive / $total ($pct%)"
    Write-Output "max_delay_ratio: $maxRatio  spread: $spread"
    Write-Output "traffic_source: $trafficSource"
    Write-Output "single-capture gate hint: $gateContribution (need >= 50% alternatives with delay > 0)"
    Write-Output ""
}

if (-not (Test-Path $CapturePath)) {
    Write-Error "File not found: $CapturePath"
    exit 1
}

Evaluate-Capture -JsonPath $CapturePath
