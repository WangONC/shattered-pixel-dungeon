param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$DeliveryRoot
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$delivery = [System.IO.Path]::GetFullPath($DeliveryRoot)
$base = '49918f76dde77a637d84fc08c8a1a03f99138a3d'
$expectedParent = '8e7cf607f77f34e0a32eecbbdb4ea641a29721cd'
$expectedBranch = 'feature/gameplay-components-v6'
$expectedMessage = '完成 v6 编译执行链与真实运行时接入'
$stage = Join-Path $delivery 'review-content'
$bundle = Join-Path $delivery 'SPD_GC_V6_P03_R1_REVIEW_BUNDLE.zip'
$checkpoint = Join-Path $delivery 'checkpoint/SPD_GC_V6_P03_R1_RUNTIME_CHECKPOINT.zip'
$sourceManifest = Join-Path $delivery 'checkpoint/SOURCE_SHA256SUMS.txt'
$gateResultsPath = Join-Path $delivery 'P03_GATE_RESULTS.json'
$verifyReportPath = Join-Path $delivery 'CHECKPOINT_UNPACK_VERIFICATION.md'
$parentCheckpoint = Join-Path $repoRoot 'build/v6-delivery/P02/checkpoint/SPD_GC_V6_P02_R1_CHECKPOINT.zip'

if (-not $delivery.StartsWith((Join-Path $repoRoot 'build'), [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'delivery root must stay under the repository build directory'
}
$branch = (& git -C $repoRoot branch --show-current).Trim()
$head = (& git -C $repoRoot rev-parse HEAD).Trim()
$parent = (& git -C $repoRoot rev-parse HEAD^).Trim()
$message = (& git -C $repoRoot show -s --format=%s HEAD)
$status = @(& git -C $repoRoot status --porcelain)
$parentTree = (& git -C $repoRoot rev-parse ($base + '^{tree}')).Trim()
$candidateSourceTree = (& git -C $repoRoot rev-parse 'HEAD^{tree}').Trim()
if ($branch -ne $expectedBranch) { throw "unexpected branch: $branch" }
if ($parent -ne $expectedParent) { throw "P03-R1 candidate parent must be $expectedParent, actual $parent" }
if ($message -ne $expectedMessage) { throw "unexpected P03 commit message: $message" }
if ($status.Count -gt 0) { throw 'review bundle requires a clean committed source tree' }
foreach ($required in @($checkpoint, $sourceManifest, $gateResultsPath, $verifyReportPath, $parentCheckpoint)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "required P03 evidence missing: $required" }
}
$gateResults = Get-Content -LiteralPath $gateResultsPath -Raw | ConvertFrom-Json
if ($gateResults.status -ne 'PASS') { throw 'working-tree P03 gates did not pass' }
$checkpointGateResultsPath = Join-Path $delivery 'checkpoint-verification/gates/P03_GATE_RESULTS.json'
$checkpointGateResults = Get-Content -LiteralPath $checkpointGateResultsPath -Raw | ConvertFrom-Json
if ($checkpointGateResults.status -ne 'PASS') { throw 'checkpoint-unpacked P03 gates did not pass' }
$checkpointHash = (Get-FileHash -LiteralPath $checkpoint -Algorithm SHA256).Hash.ToLowerInvariant()
$sourceManifestHash = (Get-FileHash -LiteralPath $sourceManifest -Algorithm SHA256).Hash.ToLowerInvariant()
$parentCheckpointHash = (Get-FileHash -LiteralPath $parentCheckpoint -Algorithm SHA256).Hash.ToLowerInvariant()

if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
[System.IO.Directory]::CreateDirectory((Join-Path $stage 'artifacts')) | Out-Null
[System.IO.Directory]::CreateDirectory((Join-Path $stage 'logs/working-tree')) | Out-Null
[System.IO.Directory]::CreateDirectory((Join-Path $stage 'logs/checkpoint-unpacked')) | Out-Null

function Invoke-GitText {
    param([string[]]$Arguments, [string]$Output)
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = 'git'
    $info.WorkingDirectory = $repoRoot
    $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $info
    $null = $process.Start()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "git command failed: $stderr" }
    [System.IO.File]::WriteAllText($Output, $stdout, [System.Text.UTF8Encoding]::new($false))
}

Invoke-GitText @('diff', '--binary', $base, $head) (Join-Path $stage 'P03_CUMULATIVE.diff')
Invoke-GitText @('diff', '--name-status', $base, $head) (Join-Path $stage 'CHANGED_FILES.txt')

$auditPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md'
$contractPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md'
$planPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md'
$promptPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/P03_typed_skill_compiler_CODEX_PROMPT.md'
$manifest = [ordered]@{
    phase = 'P03-R1'
    title = 'v6 编译执行链与真实运行时接入'
    accepted = $false
    next_phase_allowed = $false
    parent_checkpoint_sha256 = $parentCheckpointHash
    parent_checkpoint_file = 'SPD_GC_V6_P02_R1_CHECKPOINT.zip'
    parent_baseline_commit = $base
    revision_parent_commit = $expectedParent
    cumulative_diff_baseline = $base
    parent_baseline_tree = $parentTree
    branch = $branch
    candidate_commit = $head
    candidate_source_tree = $candidateSourceTree
    checkpoint = [ordered]@{
        file = 'SPD_GC_V6_P03_R1_RUNTIME_CHECKPOINT.zip'
        sha256 = $checkpointHash
        source_manifest_sha256 = $sourceManifestHash
        excluded_from_review_bundle = $true
        unpack_verification = 'PASS'
    }
    versions = [ordered]@{
        schema_version = 6
        skill_schema_version = '0.2'
        contract_version = '0.2-final'
        price_version = 'v6-p03-1'
        runtime_version = 'v6-p03-r1-1'
    }
    frozen_documents = [ordered]@{
        audit_sha256 = (Get-FileHash -LiteralPath $auditPath -Algorithm SHA256).Hash.ToLowerInvariant()
        contract_sha256 = (Get-FileHash -LiteralPath $contractPath -Algorithm SHA256).Hash.ToLowerInvariant()
        plan_sha256 = (Get-FileHash -LiteralPath $planPath -Algorithm SHA256).Hash.ToLowerInvariant()
        p03_prompt_sha256 = (Get-FileHash -LiteralPath $promptPath -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    gate_status = $gateResults.status
    gates = $gateResults.gates
    checkpoint_gate_status = $checkpointGateResults.status
    checkpoint_gates = $checkpointGateResults.gates
    completion_evidence_source = 'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P03ImplementationEvidence.java'
    limitations = @(
        'Only Active/Always, DirectDamage, Direct delivery, SelectedActor/Single/enemy-exclude-self targeting, NoCost, Primary, and optional ImmediateOnPrimarySuccess secondary are player-exposed and IMPLEMENTED.',
        'All other Contract variants remain DECLARED, DEFERRED, or UNSUPPORTED and are not player-exposed.',
        'DelayAfterPrimarySuccess, OnNextActionAfterPrimarySuccess, AnyOf, Not, CapabilityOverride, and BehaviorOverride have no P03 runtime implementation.',
        'Talent, Subclass, Specialization, Armor Ability, and P04+ effect families are intentionally absent.',
        'Both public v6 gameplay and player-builder feature flags remain false pending independent acceptance.'
    )
}
$manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $stage 'V6_CHECKPOINT_MANIFEST.json') -Encoding utf8NoBOM

$changedCount = ((Get-Content -LiteralPath (Join-Path $stage 'CHANGED_FILES.txt')) | Where-Object { $_ }).Count
@(
    '# Gameplay Components v6 — P03-R1 实施报告'
    ''
    '## 结论'
    ''
    '- P03-R1 working-tree Gate：PASS。'
    '- 完整 checkpoint 解压副本 Gate：PASS。'
    '- 未进入 P04；未 push、未 tag。'
    '- accepted=false，next_phase_allowed=false。'
    ''
    '## 基线与范围'
    ''
    "- 分支：$branch"
    "- 唯一 P02 Git 基线 / 累计 Diff 基线：$base"
    "- P03-R1 直接父提交：$expectedParent"
    "- P02 parent checkpoint SHA-256：$parentCheckpointHash"
    "- P03 候选提交：$head"
    "- P03 候选 source tree：$candidateSourceTree"
    "- 修改文件数：$changedCount"
    ''
    '## 实现结果'
    ''
    '- 引入 SkillSpec v0.2 与 Trigger、ConditionExpr/AllOf、Effect、EffectChain、Delivery、Targeting、Modifier、Cost、SkillConstraint 的强类型不可变结构。'
    '- Contract Variant 由独立 descriptor catalog 标注 IMPLEMENTED/DEFERRED/UNSUPPORTED；只有证据完整的 P03 纵切对玩家暴露。'
    '- UI 与 Headless 共同使用专用 BuilderCommand、BuilderReducer 与 SkillDraftEditor；不允许 generic effect field bag。'
    '- Structural、Compatibility、Runtime Capability 三层纯校验器不修改 Spec；Finalize 与 Compiler 均 fail closed。'
    '- CompiledSkill 是独立不可变运行时节点图；ClassCompilePlan 带 build/hash/schema/price/runtime 元数据，并区分 PREVIEW、PARTIAL、EXECUTABLE。'
    '- typed executor preflight 在 cost commit 前完成，明确区分 missing executor、type mismatch、target unavailable、immune 与 unsupported parameters，失败零 mutation。'
    '- 已安装到真实 Hero 的 finalized v6 build 通过 RuleHooks.triggerActive 进入 v6 compile plan、V6RuleRuntime 与 typed executor。'
    '- DirectDamage 复用 RuleHooks 的 damage causality/recursion guard；真实击杀归属 Hero，ON_KILL 仅触发一次且事件、原因与 originating skill 可追踪。'
    '- Completion evidence 已移至测试 QA 层；生产 Compiler/Plan/Runtime 不依赖测试矩阵或测试方法字符串。'
    '- canonical save/load 覆盖全部 P03 typed 字段；v5 skill migration placeholder 明确返回 unsupported typed mapping。'
    ''
    '## Gate 证据'
    ''
    '- 命令、退出码、耗时与日志：P03_GATE_RESULTS.json、logs/working-tree/。'
    '- checkpoint 解压复验：CHECKPOINT_UNPACK_VERIFICATION.md、CHECKPOINT_P03_GATE_RESULTS.json、logs/checkpoint-unpacked/。'
    '- Builder command trace、canonical build、真实 runtime trace：artifacts/。'
    '- Test-only completion evidence 会解析并实际运行每个证据 JUnit 方法；删除或拼错 ID 会使 Gate 失败。'
    ''
    '## Deferred / Unsupported'
    ''
    '- 除 P03 明确纵切外的 Effect/Delivery/Selector/Coverage/Filter/Cost/Constraint Variant 均未暴露。'
    '- Delay/NextAction、AnyOf/Not、Capability/Behavior Override 未实现。'
    '- P04+ Effect Family、Talent、Subclass、Specialization、Armor Ability 未实现。'
    '- public feature flag 与 player-builder feature flag 均保持关闭。'
) | Set-Content -LiteralPath (Join-Path $stage 'PHASE_P03_IMPLEMENTATION_REPORT.md') -Encoding utf8NoBOM

Copy-Item -LiteralPath $gateResultsPath -Destination (Join-Path $stage 'P03_GATE_RESULTS.json')
Copy-Item -LiteralPath $verifyReportPath -Destination (Join-Path $stage 'CHECKPOINT_UNPACK_VERIFICATION.md')
Copy-Item -LiteralPath $sourceManifest -Destination (Join-Path $stage 'CHECKPOINT_SOURCE_SHA256SUMS.txt')
Copy-Item -Path (Join-Path $delivery 'artifacts/*') -Destination (Join-Path $stage 'artifacts') -Recurse -Force
Copy-Item -Path (Join-Path $delivery 'logs/*') -Destination (Join-Path $stage 'logs/working-tree') -Force
Copy-Item -LiteralPath $checkpointGateResultsPath -Destination (Join-Path $stage 'CHECKPOINT_P03_GATE_RESULTS.json')
Copy-Item -Path (Join-Path $delivery 'checkpoint-verification/gates/logs/*') -Destination (Join-Path $stage 'logs/checkpoint-unpacked') -Force

$checksumLines = [System.Collections.Generic.List[string]]::new()
Get-ChildItem -LiteralPath $stage -Recurse -File | Sort-Object FullName | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($stage, $_.FullName).Replace('\', '/')
    $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumLines.Add("$hash  $relative")
}
$checksumLines | Set-Content -LiteralPath (Join-Path $stage 'SHA256SUMS.txt') -Encoding utf8NoBOM

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $bundle) { Remove-Item -LiteralPath $bundle -Force }
$stream = [System.IO.File]::Open($bundle, [System.IO.FileMode]::CreateNew)
try {
    $archive = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        Get-ChildItem -LiteralPath $stage -Recurse -File | Sort-Object FullName | ForEach-Object {
            $relative = [System.IO.Path]::GetRelativePath($stage, $_.FullName).Replace('\', '/')
            $entry = $archive.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime = [DateTimeOffset]::new(2000, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
            $input = [System.IO.File]::OpenRead($_.FullName)
            $output = $entry.Open()
            try { $input.CopyTo($output) } finally { $output.Dispose(); $input.Dispose() }
        }
    } finally { $archive.Dispose() }
} finally { $stream.Dispose() }

$bundleHash = (Get-FileHash -LiteralPath $bundle -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Output "review_bundle=$bundle"
Write-Output "review_bundle_sha256=$bundleHash"
Write-Output "checkpoint_sha256=$checkpointHash"
Write-Output "parent_checkpoint_sha256=$parentCheckpointHash"
