param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$DeliveryRoot
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$delivery = [System.IO.Path]::GetFullPath($DeliveryRoot)
$checkpointRoot = Join-Path $delivery 'checkpoint'
$verificationRoot = Join-Path $delivery 'checkpoint-verification'
$unpackedRoot = Join-Path $verificationRoot 'source'
$checkpointPath = Join-Path $checkpointRoot 'SPD_GC_V6_P01_R1_CHECKPOINT.zip'
$sourceManifest = Join-Path $checkpointRoot 'SOURCE_SHA256SUMS.txt'
$reportPath = Join-Path $delivery 'CHECKPOINT_UNPACK_VERIFICATION.md'

if (-not $delivery.StartsWith((Join-Path $repoRoot 'build'), [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'delivery root must stay under the repository build directory'
}
[System.IO.Directory]::CreateDirectory($checkpointRoot) | Out-Null
if (Test-Path -LiteralPath $verificationRoot) {
    Remove-Item -LiteralPath $verificationRoot -Recurse -Force
}
[System.IO.Directory]::CreateDirectory($verificationRoot) | Out-Null

& 'C:\Program Files\PowerShell\7\pwsh.exe' -NoProfile -File (Join-Path $repoRoot 'tools/v6/create-p00-checkpoint.ps1') `
    -Root $repoRoot -Output $checkpointPath -SourceManifest $sourceManifest
if ($LASTEXITCODE -ne 0) { throw 'checkpoint creation failed' }

Expand-Archive -LiteralPath $checkpointPath -DestinationPath $unpackedRoot -Force
& 'C:\Program Files\PowerShell\7\pwsh.exe' -NoProfile -File (Join-Path $unpackedRoot 'tools/v6/verify-p00-source-tree.ps1') `
    -Root $unpackedRoot -Checkpoint
if ($LASTEXITCODE -ne 0) { throw 'checkpoint source-tree verification failed' }

$verifiedFiles = 0
foreach ($line in Get-Content -LiteralPath $sourceManifest) {
    if ($line -notmatch '^([0-9a-f]{64})  (.+)$') { throw "invalid source manifest line: $line" }
    $expected = $Matches[1]
    $relative = $Matches[2]
    $path = Join-Path $unpackedRoot $relative
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "checkpoint file missing: $relative" }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) { throw "checkpoint file hash mismatch: $relative" }
    $verifiedFiles++
}

$gateOutput = Join-Path $verificationRoot 'gates'
& 'C:\Program Files\PowerShell\7\pwsh.exe' -NoProfile -File (Join-Path $unpackedRoot 'tools/v6/run-p01-gates.ps1') `
    -Root $unpackedRoot -Output $gateOutput
$gateExitCode = $LASTEXITCODE
$checkpointHash = (Get-FileHash -LiteralPath $checkpointPath -Algorithm SHA256).Hash.ToLowerInvariant()
$manifestHash = (Get-FileHash -LiteralPath $sourceManifest -Algorithm SHA256).Hash.ToLowerInvariant()
$gateResults = Get-Content -LiteralPath (Join-Path $gateOutput 'P01_GATE_RESULTS.json') -Raw | ConvertFrom-Json
$status = if ($gateExitCode -eq 0 -and $gateResults.status -eq 'PASS') { 'PASS' } else { 'FAIL' }

@(
    '# P01 Checkpoint 解压副本复验报告'
    ''
    "- Status: $status"
    "- Checkpoint: SPD_GC_V6_P01_R1_CHECKPOINT.zip"
    "- Checkpoint SHA-256: $checkpointHash"
    "- Source manifest SHA-256: $manifestHash"
    "- Verified source files: $verifiedFiles"
    '- Forbidden checkpoint content (.git/.gradle/.idea/build/local.properties/signing binaries): absent'
    "- Unpacked gate status: $($gateResults.status)"
    "- Unpacked gate count: $($gateResults.gates.Count)"
    "- Unpacked gate results: checkpoint-verification/gates/P01_GATE_RESULTS.json"
    "- Unpacked logs: checkpoint-verification/gates/logs/"
) | Set-Content -LiteralPath $reportPath -Encoding utf8NoBOM

if ($status -ne 'PASS') { throw 'checkpoint unpack gate verification failed' }
Write-Output "P01_CHECKPOINT_VERIFY_PASS sha256=$checkpointHash files=$verifiedFiles"
