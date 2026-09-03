param(
    [Parameter(Mandatory = $true)][string]$Root,
    [switch]$Checkpoint
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$p00 = Join-Path $repoRoot 'tools/v6/verify-p00-source-tree.ps1'
& 'D:\Environment\PowerShell\7\pwsh.exe' -NoProfile -File $p00 -Root $repoRoot -Checkpoint:$Checkpoint
if ($LASTEXITCODE -ne 0) { throw 'P00 source-tree verification failed' }

$required = @(
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/builder/BuilderCommand.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/builder/BuilderReducer.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/builder/BuilderState.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/builder/PlayerBuildSession.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/form/FormSchema.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/form/BuilderFormController.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/form/EnumListFieldSchema.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/form/MultiEnumSelector.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndCreateClassV6ControllerView.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/form/NumberStepper.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/form/ReferencePicker.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndCreateClassV6.java',
    'headless/src/main/java/com/shatteredpixel/shatteredpixeldungeon/headless/HeadlessPlayerBuildAdapter.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/BuilderReducerPlayerPathTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/BuilderFormControllerTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/RuntimeNodeCounterValidationTest.java',
    'headless/src/test/java/com/shatteredpixel/shatteredpixeldungeon/headless/HeadlessPlayerBuildAdapterTest.java',
    'headless/src/test/java/com/shatteredpixel/shatteredpixeldungeon/headless/HeadlessBuilderFormParityTest.java'
)
$failures = [System.Collections.Generic.List[string]]::new()
foreach ($relative in $required) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relative) -PathType Leaf)) {
        $failures.Add("MISSING:$relative")
    }
}

$boundary = Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/V6GameplayBoundary.java'
if (Test-Path -LiteralPath $boundary -PathType Leaf) {
    $text = [System.IO.File]::ReadAllText($boundary)
    if ($text -notmatch 'PLAYER_BUILDER_ENABLED\s*=\s*false') {
        $failures.Add('PLAYER_BUILDER_FLAG_NOT_FALSE')
    }
}

$entry = [System.IO.File]::ReadAllText((Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndCreateClassV6.java'))
$view = [System.IO.File]::ReadAllText((Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndCreateClassV6ControllerView.java'))
if ($entry -notmatch 'WndCreateClassV6ControllerView\.show\(new BuilderFormController\(session\)\)') {
    $failures.Add('V6_PLAYER_WINDOW_NOT_CONTROLLER_DRIVEN')
}
if ($view -match 'P02_DEFERRED_(?:SKILL|COMPONENT)') {
    $failures.Add('V6_PLAYER_ROOT_EXPOSES_DEFERRED_SKILL_OR_COMPONENT')
}
if ($view -notmatch 'controller\.dispatchValue\(') {
    $failures.Add('V6_PLAYER_FIELDS_DO_NOT_DISPATCH_CONTROLLER')
}

Write-Output "source_root=$repoRoot"
Write-Output "checkpoint_mode=$Checkpoint"
Write-Output "p02_required_files=$($required.Count)"
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Output 'P02_SOURCE_TREE_VERIFY_PASS'
