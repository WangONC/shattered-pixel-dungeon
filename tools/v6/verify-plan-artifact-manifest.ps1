param(
    [Parameter(Mandatory = $true)][string]$Root
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$planRoot = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN'
$manifestPath = Join-Path $planRoot 'PLAN_ARTIFACT_MANIFEST.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($entry in $manifest.files) {
    $path = Join-Path $planRoot $entry.path
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $failures.Add("MISSING:$($entry.path)")
        continue
    }
    $item = Get-Item -LiteralPath $path
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($item.Length -ne [long]$entry.bytes) { $failures.Add("SIZE:$($entry.path):$($item.Length):$($entry.bytes)") }
    if ($hash -ne $entry.sha256) { $failures.Add("SHA256:$($entry.path):${hash}:$($entry.sha256)") }
    Write-Output "OK $($entry.path) bytes=$($item.Length) sha256=$hash"
}

Write-Output "manifest=$manifestPath"
Write-Output "verified=$($manifest.files.Count)"
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Output 'PLAN_ARTIFACT_MANIFEST_VERIFY_PASS'
