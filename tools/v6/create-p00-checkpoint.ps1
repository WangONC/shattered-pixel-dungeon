param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$Output,
    [Parameter(Mandatory = $true)][string]$SourceManifest
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$zipPath = [System.IO.Path]::GetFullPath($Output)
$manifestPath = [System.IO.Path]::GetFullPath($SourceManifest)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($zipPath)) | Out-Null

$rawFiles = & git -C $repoRoot ls-files --cached --others --exclude-standard
if ($LASTEXITCODE -ne 0) { throw 'git ls-files failed' }
$files = @($rawFiles | ForEach-Object { $_.Replace('\', '/') } | Where-Object {
    $_ -and
    $_ -notmatch '(^|/)(\.git|\.gradle|\.idea|build)(/|$)' -and
    $_ -notmatch '(^|/)local\.properties$' -and
    $_ -notmatch '\.(apk|aab|jks|keystore)$' -and
    $_ -notmatch '^build/v6-delivery/'
} | Sort-Object -Unique)
if ($files.Count -eq 0) { throw 'no source files selected' }

$manifestLines = [System.Collections.Generic.List[string]]::new()
foreach ($relative in $files) {
    $path = Join-Path $repoRoot $relative
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "selected file missing: $relative" }
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    $manifestLines.Add("$hash  $relative")
}
$manifestLines | Set-Content -LiteralPath $manifestPath -Encoding utf8NoBOM

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
$stream = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::CreateNew)
try {
    $archive = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        foreach ($relative in $files) {
            $path = Join-Path $repoRoot $relative
            $entry = $archive.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime = [DateTimeOffset]::new(2000, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
            $input = [System.IO.File]::OpenRead($path)
            $outputStream = $entry.Open()
            try { $input.CopyTo($outputStream) }
            finally { $outputStream.Dispose(); $input.Dispose() }
        }
    } finally { $archive.Dispose() }
} finally { $stream.Dispose() }

$zipHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
$manifestHash = (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Output "checkpoint=$zipPath"
Write-Output "checkpoint_sha256=$zipHash"
Write-Output "source_manifest=$manifestPath"
Write-Output "source_manifest_sha256=$manifestHash"
Write-Output "source_files=$($files.Count)"
