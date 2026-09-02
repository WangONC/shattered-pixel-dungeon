param(
    [Parameter(Mandatory = $true)][string]$Root,
    [switch]$Checkpoint
)

$ErrorActionPreference = 'Stop'
$sourceRoot = [System.IO.Path]::GetFullPath($Root)
$failures = [System.Collections.Generic.List[string]]::new()

$required = @(
    'SPD-classes', 'android', 'core', 'desktop', 'docs', 'headless', 'ios', 'services', 'gradle',
    'gradlew', 'gradlew.bat', 'build.gradle', 'settings.gradle', 'gradle.properties',
    'core/src/main/assets', 'core/src/main/java', 'core/src/test/java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/architecture/GameplayComponentsV6ArchitectureTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/qa/LegacyGameplaySmokeTest.java'
)
foreach ($relative in $required) {
    if (-not (Test-Path -LiteralPath (Join-Path $sourceRoot $relative))) { $failures.Add("MISSING:$relative") }
}

$forbiddenNames = @('.git', '.gradle', '.idea', 'local.properties')
if ($Checkpoint) {
    foreach ($name in $forbiddenNames) {
        if (Get-ChildItem -LiteralPath $sourceRoot -Recurse -Force -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -eq $name } | Select-Object -First 1) {
            $failures.Add("FORBIDDEN_NAME:$name")
        }
    }
    if (Get-ChildItem -LiteralPath $sourceRoot -Recurse -Directory -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -eq 'build' } | Select-Object -First 1) {
        $failures.Add('FORBIDDEN_BUILD_DIRECTORY')
    }
    if (Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.Extension -in @('.apk', '.aab', '.jks', '.keystore') } | Select-Object -First 1) {
        $failures.Add('FORBIDDEN_BINARY_OR_SIGNING_FILE')
    }
}

$expectedHashes = [ordered]@{
    'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md' = '6da79a5fd3767070911a173a5db3340c0c43c9005e503383cd6858b98e26d65a'
    'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md' = '61f2e37f6cb527f788dbd87ceed3bab227c31b257ab7b731dfbba2c1d1021bd8'
    'docs/archive/SPD_CLASS_GAMEPLAY_COMPONENTS_SPEC_v0.1.md' = 'fc53a922d29ce0cd1de59f8f68a4647b94b622626d224f26c1d441161a0ebc6c'
    'docs/SPD_GAMEPLAY_V6_DEV_PLAN/README.md' = '1dbd1785c2dd19e615abc8669559005b417d56576dd1e9ac324bc31644802718'
    'docs/SPD_GAMEPLAY_V6_DEV_PLAN/SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md' = '975ee57b8c920a4f2e5d2b19cd4f70c7bd4fbc4509a23609fe7d46dd7755342f'
    'docs/SPD_GAMEPLAY_V6_DEV_PLAN/P00_baseline_legacy_freeze_CODEX_PROMPT.md' = '03ed2b7507d788469a86afc9f3a8dca92ce29cc15c5f8ad93b04bf800b0c7b25'
}
foreach ($relative in $expectedHashes.Keys) {
    $path = Join-Path $sourceRoot $relative
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { $failures.Add("FROZEN_MISSING:$relative"); continue }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expectedHashes[$relative]) { $failures.Add("FROZEN_HASH:${relative}:$actual") }
}

Write-Output "source_root=$sourceRoot"
Write-Output "checkpoint_mode=$Checkpoint"
Write-Output "required_items=$($required.Count)"
Write-Output "frozen_hashes=$($expectedHashes.Count)"
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Output 'P00_SOURCE_TREE_VERIFY_PASS'
