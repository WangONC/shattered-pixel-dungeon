param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$DeliveryRoot,
    [string]$JavaHome = 'C:\User\Environment\Java\jdk-24',
    [string]$AndroidSdk = 'C:\User\Application\Android\SDK'
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$delivery = [System.IO.Path]::GetFullPath($DeliveryRoot)
$checkpointRoot = Join-Path $delivery 'checkpoint'
$verificationRoot = Join-Path $delivery 'checkpoint-verification'
$unpackedRoot = Join-Path $verificationRoot 'source'
$checkpointPath = Join-Path $checkpointRoot 'SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip'
$sourceManifest = Join-Path $checkpointRoot 'SOURCE_SHA256SUMS.txt'
$reportPath = Join-Path $delivery 'CHECKPOINT_UNPACK_VERIFICATION.md'

if (-not $delivery.StartsWith((Join-Path $repoRoot 'build'), [System.StringComparison]::OrdinalIgnoreCase)) { throw 'delivery root must stay under repository build' }
[System.IO.Directory]::CreateDirectory($checkpointRoot) | Out-Null
if (Test-Path -LiteralPath $verificationRoot) { Remove-Item -LiteralPath $verificationRoot -Recurse -Force }
[System.IO.Directory]::CreateDirectory($verificationRoot) | Out-Null

& (Join-Path $repoRoot 'tools/v6/create-p00-checkpoint.ps1') -Root $repoRoot -Output $checkpointPath -SourceManifest $sourceManifest
Expand-Archive -LiteralPath $checkpointPath -DestinationPath $unpackedRoot -Force
& (Join-Path $unpackedRoot 'tools/v6/verify-p04-source-tree.ps1') -Root $unpackedRoot -Checkpoint

$verifiedFiles = 0
foreach ($line in Get-Content -LiteralPath $sourceManifest) {
    if ($line -notmatch '^([0-9a-f]{64})  (.+)$') { throw "invalid source manifest line: $line" }
    $path = Join-Path $unpackedRoot $Matches[2]
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "checkpoint file missing: $($Matches[2])" }
    if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Matches[1]) { throw "checkpoint file hash mismatch: $($Matches[2])" }
    $verifiedFiles++
}

$gateOutput = Join-Path $verificationRoot 'gates'
& (Join-Path $unpackedRoot 'tools/v6/run-p04-gates.ps1') -Root $unpackedRoot -Output $gateOutput -JavaHome $JavaHome -AndroidSdk $AndroidSdk
$gateExitCode = $LASTEXITCODE
$gateResults = Get-Content -LiteralPath (Join-Path $gateOutput 'P04_GATE_RESULTS.json') -Raw | ConvertFrom-Json
$status = if ($gateExitCode -eq 0 -and $gateResults.status -eq 'PASS') { 'PASS' } else { 'FAIL' }
$checkpointHash = (Get-FileHash -LiteralPath $checkpointPath -Algorithm SHA256).Hash.ToLowerInvariant()
$manifestHash = (Get-FileHash -LiteralPath $sourceManifest -Algorithm SHA256).Hash.ToLowerInvariant()
@('# P04 Checkpoint 解压副本复验报告', '', "- Status: $status", '- Checkpoint: SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip', "- Checkpoint SHA-256: $checkpointHash", "- Source manifest SHA-256: $manifestHash", "- Verified source files: $verifiedFiles", '- Forbidden checkpoint content (.git/.gradle/.idea/build/local.properties/signing binaries): absent', "- Unpacked gate status: $($gateResults.status)", "- Unpacked gate count: $($gateResults.gates.Count)", '- Unpacked gate results: checkpoint-verification/gates/P04_GATE_RESULTS.json', '- Unpacked logs: checkpoint-verification/gates/logs/') |
    Set-Content -LiteralPath $reportPath -Encoding utf8NoBOM
if ($status -ne 'PASS') { throw 'checkpoint unpack gate verification failed' }
Write-Output "P04_CHECKPOINT_VERIFY_PASS sha256=$checkpointHash files=$verifiedFiles"
