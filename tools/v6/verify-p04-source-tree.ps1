param(
    [Parameter(Mandatory = $true)][string]$Root,
    [switch]$Checkpoint
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$failures = [System.Collections.Generic.List[string]]::new()

& (Join-Path $repoRoot 'tools/v6/verify-p00-source-tree.ps1') -Root $repoRoot -Checkpoint:$Checkpoint

$required = @(
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/builder/ClassNodeDraftEditor.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/CompiledClassComponent.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/CompiledClassOperation.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/CompiledResource.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/dependency/GlobalNodeIdentityValidator.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/dependency/RuntimeStateValidator.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/P04EffectExecutors.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/ResourceRuntimeStateFactory.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/ResourceTransaction.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/SkillTargetPreflight.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/V6RuleRuntime.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/V6RuleRuntimeBridge.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/save/CanonicalBuildCodec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/component/BasicAttackComponentSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/component/ResourceFlowComponentSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/component/ActiveResourceOperationComponentSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/component/ResourceClassOperationSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/resource/ConvertResourceSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/BuiltinStatRef.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/HpCostSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/ClassResourceHUD.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04ComponentsAndRecipesTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04CoreEffectRuntimeTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04DeliveryArtifactTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04DeliveryModifierCostTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04ImplementationEvidence.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04ImplementationEvidenceTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04LayerEvidenceTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04ResourceTransactionTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04RuntimeRestoreAndIdentityTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04TargetingAndTransactionBoundaryTest.java',
    'tools/v6/run-p04-gates.ps1',
    'tools/v6/verify-p04-checkpoint.ps1',
    'tools/v6/create-p04-review-bundle.ps1'
)
foreach ($relative in $required) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relative) -PathType Leaf)) {
        $failures.Add("MISSING:$relative")
    }
}

$boundary = [System.IO.File]::ReadAllText((Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/V6GameplayBoundary.java'))
foreach ($flag in @('PUBLIC_GAMEPLAY_ENABLED', 'PLAYER_BUILDER_ENABLED')) {
    if ($boundary -notmatch ($flag + '\s*=\s*false')) { $failures.Add("PUBLIC_FLAG_NOT_FALSE:$flag") }
}

$v6Root = Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6'
Get-ChildItem -LiteralPath $v6Root -Recurse -Filter '*.java' | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($repoRoot, $_.FullName).Replace('\', '/')
    $text = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($forbidden in @('rules.EffectVocabularyRegistry', 'rules.PlayerBuildAssembler', 'rules.EffectSpec')) {
        if ($text.Contains($forbidden)) { $failures.Add("LEGACY_DEPENDENCY:$relative`:$forbidden") }
    }
}

$forms = [System.IO.File]::ReadAllText((Join-Path $v6Root 'form/V6FormSchemas.java'))
if ($forms -match 'ALLOW_LETHAL') { $failures.Add('ALLOW_LETHAL_EXPOSED_BY_FORM') }
$catalog = [System.IO.File]::ReadAllText((Join-Path $v6Root 'catalog/GameplayVariantCatalog.java'))
foreach ($future in @('EFFECT.*ADD_MARK', 'EFFECT.*SET_MODE', 'EFFECT.*CREATE_ENTITY', 'RESOURCE_OPERATION.*TRANSFER')) {
    if ($catalog -match ('implemented\([^\r\n]*' + $future)) { $failures.Add("POST_P04_VARIANT_IMPLEMENTED:$future") }
}
$production = (Get-ChildItem -LiteralPath $v6Root -Recurse -Filter '*.java' | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"
foreach ($label in @('Gunner Core', 'Blood Class', 'Mage Domain')) {
    if ($production.Contains($label)) { $failures.Add("FIXED_CLASS_LABEL:$label") }
}
$hpCost = [System.IO.File]::ReadAllText((Join-Path $v6Root 'spec/skill/HpCostSpec.java'))
if ($hpCost -notmatch 'REJECT_IF_WOULD_KILL' -or $hpCost -notmatch 'minimumRemainingHp' -or $hpCost -notmatch 'ALLOW_LETHAL is not exposed in P04') {
    $failures.Add('P04_HP_COST_BOUNDARY_MISSING')
}
$runtime = [System.IO.File]::ReadAllText((Join-Path $v6Root 'runtime/V6RuleRuntime.java')) + "`n" + [System.IO.File]::ReadAllText((Join-Path $v6Root 'runtime/SkillTargetPreflight.java'))
foreach ($stage in @('dependency_resolve', 'target_preflight', 'effect_capability_preflight', 'cost_preflight', 'cost_commit', 'chain_activation')) {
    if ($runtime -notmatch $stage) { $failures.Add("TRANSACTION_STAGE_MISSING:$stage") }
}

Write-Output "source_root=$repoRoot"
Write-Output "checkpoint_mode=$Checkpoint"
Write-Output "p04_required_files=$($required.Count)"
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Output 'P04_SOURCE_TREE_VERIFY_PASS'
