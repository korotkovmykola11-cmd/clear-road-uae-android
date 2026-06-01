param(
    [Parameter(Mandatory = $true)][string]$InputPath,
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [int]$Tolerance = 32
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$exe = Join-Path $scriptDir "WhiteBgRemover.exe"

if (-not (Test-Path $exe)) {
    $csc = Join-Path ${env:WINDIR} "Microsoft.NET\Framework64\v4.0.30319\csc.exe"
    if (-not (Test-Path $csc)) {
        throw "csc.exe not found"
    }
    & $csc /nologo /optimize+ /r:System.Drawing.dll "/out:$exe" (Join-Path $scriptDir "WhiteBgRemover.cs")
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to compile WhiteBgRemover.exe"
    }
}

$outDir = Split-Path $OutputPath -Parent
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

& $exe $InputPath $OutputPath $Tolerance
if ($LASTEXITCODE -ne 0) {
    throw "WhiteBgRemover failed for $InputPath"
}

Write-Output "Saved $OutputPath"
