param(
    [Parameter(Mandatory = $true)][string]$Root,
    [switch]$Checkpoint
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$p02 = Join-Path $repoRoot 'tools/v6/verify-p02-source-tree.ps1'
& 'D:\Environment\PowerShell\7\pwsh.exe' -NoProfile -File $p02 -Root $repoRoot -Checkpoint:$Checkpoint
if ($LASTEXITCODE -ne 0) { throw 'P02 source-tree verification failed' }

$required = @(
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/SkillSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/TriggerSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/ConditionExpr.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/EffectSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/EffectChainSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/DeliverySpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/TargetingSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/ModifierSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/CostSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/spec/skill/SkillConstraintSpec.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/validation/SkillStructuralValidator.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/validation/SkillCompatibilityValidator.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/validation/RuntimeCapabilityValidator.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/ClassCompilePlan.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/CompiledSkill.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/ExecutableBuildAdmissionPolicy.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/compile/SkillCompiler.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/EffectExecutorRegistry.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/EffectPreflightResult.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/DirectDamageExecutor.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/GameplayEventContext.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/RuntimeExecutionContext.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/V6RuleRuntime.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/V6RuleRuntimeBridge.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/runtime/RuntimeTrace.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/catalog/GameplayVariantCatalog.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/builder/SkillDraftEditor.java',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/migration/v5/P03SkillMigrationPlaceholder.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03PlayerBuilderVerticalSliceTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03DirectDamageRuntimeBehaviorTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03TypedSkillCanonicalRoundTripTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03R1CompilePlanTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03R2CompileAdmissionTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03R1ExecutorPreflightTest.java',
    'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03ImplementationEvidenceTest.java',
    'headless/src/test/java/com/shatteredpixel/shatteredpixeldungeon/headless/P03HeadlessTypedSkillParityTest.java',
    'tools/v6/run-p03-gates.ps1',
    'tools/v6/verify-p03-checkpoint.ps1',
    'tools/v6/create-p03-review-bundle.ps1'
)
$failures = [System.Collections.Generic.List[string]]::new()
foreach ($relative in $required) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relative) -PathType Leaf)) {
        $failures.Add("MISSING:$relative")
    }
}

$boundaryPath = Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/V6GameplayBoundary.java'
$boundary = [System.IO.File]::ReadAllText($boundaryPath)
foreach ($flag in @('PUBLIC_GAMEPLAY_ENABLED', 'PLAYER_BUILDER_ENABLED')) {
    if ($boundary -notmatch ($flag + '\s*=\s*false')) { $failures.Add("PUBLIC_FLAG_NOT_FALSE:$flag") }
}

$v6Root = Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6'
$registryPath = Join-Path $v6Root 'runtime/EffectExecutorRegistry.java'
if (Test-Path -LiteralPath $registryPath) {
    $registry = [System.IO.File]::ReadAllText($registryPath)
    if ($registry -match 'V6FormSchemas') { $failures.Add('EXECUTOR_REGISTRY_DEPENDS_ON_FORM_SCHEMA') }
    if ($registry -match 'DAMAGE_STANDARD|defaultDamage|fallback') { $failures.Add('EXECUTOR_REGISTRY_CONTAINS_DAMAGE_FALLBACK') }
}

foreach ($removed in @(
    'catalog/P03CompletionMatrix.java',
    'catalog/ComponentCompletionRow.java'
)) {
    if (Test-Path -LiteralPath (Join-Path $v6Root $removed) -PathType Leaf) { $failures.Add("PRODUCTION_QA_EVIDENCE_PRESENT:$removed") }
}

foreach ($relative in @(
    'compile/CompiledSkill.java',
    'compile/ClassCompilePlan.java',
    'runtime/V6RuleRuntime.java',
    'runtime/EffectExecutor.java',
    'runtime/EffectExecutorRegistry.java',
    'runtime/DirectDamageExecutor.java'
)) {
    $text = [System.IO.File]::ReadAllText((Join-Path $v6Root $relative))
    if ($text -match 'spec\.skill|\bSkillSpec\b') { $failures.Add("COMPILED_RUNTIME_DEPENDS_ON_AUTHORING:$relative") }
    if ($text -match 'P03CompletionMatrix|ComponentCompletionRow|P03ImplementationEvidence|P03[A-Za-z0-9_]+Test#') { $failures.Add("PRODUCTION_DEPENDS_ON_QA_EVIDENCE:$relative") }
}

$evidencePath = Join-Path $repoRoot 'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03ImplementationEvidence.java'
$evidence = [System.IO.File]::ReadAllText($evidencePath)
if ($evidence -match 'GameplayVariantCatalog|Iterable<String>') { $failures.Add('COMPLETION_EVIDENCE_IS_CATALOG_GENERATED') }
if ($evidence -notmatch 'register\(out, row\("EFFECT\.DIRECT_DAMAGE"\)\)') { $failures.Add('DIRECT_DAMAGE_EXPLICIT_EVIDENCE_ROW_MISSING') }

$eventContext = [System.IO.File]::ReadAllText((Join-Path $v6Root 'runtime/GameplayEventContext.java'))
if ($eventContext -match 'actors\.Char|\bChar\b') { $failures.Add('GAMEPLAY_EVENT_CONTEXT_RETAINS_MUTABLE_CHAR') }
$executorContract = [System.IO.File]::ReadAllText((Join-Path $v6Root 'runtime/EffectExecutor.java'))
if ($executorContract -notmatch 'EffectPreflightResult\s+preflight') { $failures.Add('TYPED_EXECUTOR_PREFLIGHT_MISSING') }
$ruleHooks = [System.IO.File]::ReadAllText((Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/RuleHooks.java'))
if ($ruleHooks -notmatch 'gameplayComponentsV6RuntimeBridge\(\)' -or $ruleHooks -notmatch 'v6DamageGateway') { $failures.Add('RULE_HOOKS_V6_RUNTIME_BRIDGE_MISSING') }

Get-ChildItem -LiteralPath $v6Root -Recurse -Filter '*.java' | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($repoRoot, $_.FullName).Replace('\', '/')
    $text = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($forbidden in @(
        'rules.EffectVocabularyRegistry',
        'rules.PlayerBuildAssembler',
        'rules.EffectSpec'
    )) {
        if ($text.Contains($forbidden)) { $failures.Add("LEGACY_SKILL_DEPENDENCY:$($relative):$forbidden") }
    }
}

$skillRoot = Join-Path $v6Root 'spec/skill'
if (Test-Path -LiteralPath $skillRoot) {
    Get-ChildItem -LiteralPath $skillRoot -Recurse -Filter '*.java' | ForEach-Object {
        $relative = [System.IO.Path]::GetRelativePath($repoRoot, $_.FullName).Replace('\', '/')
        $text = [System.IO.File]::ReadAllText($_.FullName)
        if ($text -match '\b(?:int|Integer|long|Long|String|Object)\s+(?:power|duration|stateId|templateId)\b') {
            $failures.Add("GENERIC_EFFECT_FIELD_BAG:$relative")
        }
    }
}

$viewPath = Join-Path $repoRoot 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndCreateClassV6ControllerView.java'
$view = [System.IO.File]::ReadAllText($viewPath)
if ($view -notmatch 'new BuilderCommand\.CreateSkill\(name\)') { $failures.Add('PLAYER_ROOT_MISSING_TYPED_SKILL_ENTRY') }
if ($view -match 'Talent|Subclass|Specialization|Armor Ability') { $failures.Add('POST_P03_PLAYER_FEATURE_EXPOSED') }

Write-Output "source_root=$repoRoot"
Write-Output "checkpoint_mode=$Checkpoint"
Write-Output "p03_required_files=$($required.Count)"
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Output 'P03_SOURCE_TREE_VERIFY_PASS'
