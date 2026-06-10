function Strip-Html([string]$s) { return ($s -replace '<[^>]+>', ' ' -replace '\s+', ' ').Trim() }

function Salik-Estimate([string]$scan) {
    $lc = $scan.ToLower()
    $tr = ([regex]::Matches($lc, 'toll road')).Count
    if ($tr -gt 0) { return $tr * 4 }
    $medium = @('sheikh zayed', 'szr', 'e11', 'al ittihad', 'al khail', 'd68', 'garhoud', 'airport tunnel', 'al safa', 'barsha') |
        Where-Object { $lc.Contains($_) } | Measure-Object | Select-Object -ExpandProperty Count
    $lower = @('e311', 'e611', 'emirates road', 'emirates rd', 'mohammed bin zayed', 'sheikh mohammed bin zayed', 'mbz road') |
        Where-Object { $lc.Contains($_) } | Measure-Object | Select-Object -ExpandProperty Count
    if ($lower -gt 0 -and $medium -eq 0) { return 0 }
    if ($medium -gt $lower) { return 4 }
    return 0
}

function Fuel([double]$km) { return [int][math]::Round(($km / 12.0) * 2.8) }

function Score-Prod($mode, $durMin, $distKm, $toll) {
    $fuel = Fuel $distKm
    $tot = $toll + $fuel
    $apm = if ($durMin -gt 0) { $tot / $durMin } else { $tot }
    switch ($mode) {
        'FASTEST' { return [math]::Round($durMin + $tot * 0.15 + $distKm * 0.05 + $apm * 0.2, 2) }
        'SAVE' { return [math]::Round($tot * 4 + $toll * 6 + $durMin * 0.35 + $apm * 0.4, 2) }
        'CALM' { return [math]::Round($durMin * 0.6 + $tot * 1.2 + $distKm * 0.15 + $apm * 0.3, 2) }
    }
}

function Corr-P([string]$scan) {
    $lc = $scan.ToLower()
    $hw = @('e11', 'sheikh zayed', 'szr', 'al khail', 'e44', 'e311', 'e611', 'emirates road', 'mbz', 'mohammed bin zayed') |
        Where-Object { $lc.Contains($_) } | Measure-Object | Select-Object -ExpandProperty Count
    $ur = @('business bay', 'marina', 'jumeirah', 'al wasl', 'deira', 'bur dubai', 'satwa', 'karama', 'local', 'street') |
        Where-Object { $lc.Contains($_) } | Measure-Object | Select-Object -ExpandProperty Count
    return [math]::Max(-6, [math]::Min(12, $ur * 3 - $hw * 2))
}

function Score-NewSmooth($routes) {
    $ts = $routes | ForEach-Object { [int]$_.selectedSec }
    $tf = ($ts | Measure-Object -Minimum).Minimum
    $tmax = ($ts | Measure-Object -Maximum).Maximum
    $spreadMin = ($tmax - $tf) / 60.0
    $mind = ($routes | Measure-Object -Property distKm -Minimum).Minimum
    $budget = [math]::Max(5, 0.12 * ($tf / 60.0))
    foreach ($r in $routes) {
        $bm = $r.baseSec / 60.0
        $tm = $r.selectedSec / 60.0
        $delay = [math]::Max(0, $tm - $bm)
        $dr = if ($bm -gt 0) { $delay / $bm } else { 0 }
        $dtf = $tm - ($tf / 60.0)
        $pred = $dr * 100
        $time = [math]::Max(0, $dtf - $budget) * 8
        $chaos = 0.0
        if ($dtf -le 1 -and $dr -gt 0.15 -and $spreadMin -ge 3) { $chaos = ($dr - 0.10) * 60 }
        $corr = Corr-P $r.scan
        $dist = ($r.distKm - $mind) * 0.05
        $r.newSmoothScore = [math]::Round($pred + $time + $chaos + $corr + $dist, 2)
        $r.delayMin = [math]::Round($delay, 1)
        $r.delayPct = [math]::Round($dr * 100, 1)
    }
}

function Parse-Routes($jsonPath) {
    $data = Get-Content $jsonPath -Raw | ConvertFrom-Json
    $routes = @()
    $idx = 0
    foreach ($route in $data.routes) {
        $leg = $route.legs[0]
        $baseSec = [int]$leg.duration.value
        $baseText = $leg.duration.text
        $trafSec = if ($leg.duration_in_traffic) { [int]$leg.duration_in_traffic.value } else { $baseSec }
        $trafText = if ($leg.duration_in_traffic) { $leg.duration_in_traffic.text } else { $baseText }
        $dist = [int]$leg.distance.value
        $sum = if ($route.summary) { Strip-Html $route.summary } else { '' }
        $instr = ($leg.steps | ForEach-Object { Strip-Html $_.html_instructions }) -join ' '
        $scan = ($sum + ' ' + $instr).Trim()
        $fare = 0
        if ($route.fare -and $route.fare.value) { $fare = [int][math]::Round([double]$route.fare.value) }
        $toll = if ($fare -gt 0) { $fare } else { Salik-Estimate $scan }
        $durMin = [int][math]::Round($trafSec / 60.0)
        $routes += [pscustomobject]@{
            index       = $idx
            summary     = $sum
            baseText    = $baseText
            baseSec     = $baseSec
            trafficText = $trafText
            trafficSec  = $trafSec
            selectedMin = $durMin
            distKm      = [math]::Round($dist / 1000.0, 1)
            toll        = $toll
            scan        = $scan
            scoreFastest  = Score-Prod 'FASTEST' $durMin $([math]::Round($dist / 1000.0, 1)) $toll
            scoreSave     = Score-Prod 'SAVE' $durMin $([math]::Round($dist / 1000.0, 1)) $toll
            scoreCalm     = Score-Prod 'CALM' $durMin $([math]::Round($dist / 1000.0, 1)) $toll
        }
        $idx++
    }
    return $routes
}

function Pick-Idx($routes, $field) {
    $best = 0
    $bs = [double]::MaxValue
    for ($j = 0; $j -lt $routes.Count; $j++) {
        $s = $routes[$j].$field
        if ($s -lt $bs -or ($s -eq $bs -and $j -lt $best)) { $bs = $s; $best = $j }
    }
    return $best
}

$key = (Get-Content 'c:\Users\korot\AndroidStudioProjects\ClearRoad2\local.properties' | Where-Object { $_ -match '^PLACES_API_KEY=' }) -replace 'PLACES_API_KEY=', ''
$pairs = @(
    @{ file = 'difc-marina.json'; name = 'DIFC -> Marina'; o = '25.213815,55.282032'; d = '25.077964,55.137789' },
    @{ file = 'ajman-difc.json'; name = 'Ajman -> DIFC'; o = '25.405216,55.513643'; d = '25.213815,55.282032' },
    @{ file = 'jvc-abudhabi.json'; name = 'JVC -> Abu Dhabi'; o = '25.060000,55.210000'; d = '24.487345,54.606678' },
    @{ file = 'sharjah-downtown.json'; name = 'Sharjah -> Downtown'; o = '25.338715,55.420114'; d = '25.197197,55.274376' }
)
$outDir = 'c:\Users\korot\AndroidStudioProjects\ClearRoad2\docs\stage-35-0-audit'
foreach ($p in $pairs) {
    $url = "https://maps.googleapis.com/maps/api/directions/json?origin=$($p.o)&destination=$($p.d)&mode=driving&alternatives=true&departure_time=now&traffic_model=best_guess&key=$key"
    $resp = Invoke-RestMethod -Uri $url -Method Get
    $resp | ConvertTo-Json -Depth 100 | Set-Content -Encoding utf8 "$outDir\$($p.file)"
    Write-Output "FETCH $($p.name) status=$($resp.status) routes=$($resp.routes.Count)"
}

$report = @()
foreach ($p in $pairs) {
    $routes = Parse-Routes "$outDir\$($p.file)"
    Score-NewSmooth $routes
    $wf = Pick-Idx $routes 'scoreFastest'
    $ws = Pick-Idx $routes 'scoreSave'
    $wc = Pick-Idx $routes 'scoreCalm'
    $wn = Pick-Idx $routes 'newSmoothScore'
    $report += [pscustomobject]@{ scenario = $p.name; routes = $routes; wf = $wf; ws = $ws; wc = $wc; wn = $wn }
}

$report | ConvertTo-Json -Depth 8 | Set-Content "$outDir\analysis.json" -Encoding utf8
foreach ($r in $report) {
    Write-Output "`n=== $($r.scenario) ==="
    $bases = $r.routes | ForEach-Object { $_.baseSec }
    $trafs = $r.routes | ForEach-Object { $_.trafficSec }
    $delays = $r.routes | ForEach-Object { [math]::Max(0, $_.trafficSec - $_.baseSec) }
    Write-Output ("traffic differs across alts: {0} | base differs: {1} | positive delay on any alt: {2}" -f (($trafs | Select-Object -Unique).Count -gt 1), (($bases | Select-Object -Unique).Count -gt 1), (($delays | Where-Object { $_ -gt 0 }).Count -gt 0))
    foreach ($alt in $r.routes) {
        Write-Output ("idx{0} | {1} | base={2} ({3}s) | traffic={4} ({5}s) | delay=+{6}min ({7}%) | {8}km | toll={9}AED | F={10} S={11} C={12} NS={13}" -f $alt.index, $alt.summary, $alt.baseText, $alt.baseSec, $alt.trafficText, $alt.trafficSec, $alt.delayMin, $alt.delayPct, $alt.distKm, $alt.toll, $alt.scoreFastest, $alt.scoreSave, $alt.scoreCalm, $alt.newSmoothScore)
    }
    Write-Output ("WINNERS prod: FASTEST=idx{0}({1}min) SAVE=idx{2}({3}min) CALM=idx{4}({5}min) | NEW_SMOOTH=idx{6}({7}min)" -f $r.wf, $r.routes[$r.wf].selectedMin, $r.ws, $r.routes[$r.ws].selectedMin, $r.wc, $r.routes[$r.wc].selectedMin, $r.wn, $r.routes[$r.wn].selectedMin)
}
