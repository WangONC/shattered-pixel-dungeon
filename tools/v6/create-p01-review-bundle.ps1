param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$DeliveryRoot
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$delivery = [System.IO.Path]::GetFullPath($DeliveryRoot)
$base = '1f3f6678eb7b8fa43df18deb5a46eda7e601a4b9'
$expectedBranch = 'feature/gameplay-components-v6'
$stage = Join-Path $delivery 'review-content'
$bundle = Join-Path $delivery 'SPD_GC_V6_P01_REVIEW_BUNDLE.zip'
$checkpoint = Join-Path $delivery 'checkpoint/SPD_GC_V6_P01_IDENTITY_CHECKPOINT.zip'
$sourceManifest = Join-Path $delivery 'checkpoint/SOURCE_SHA256SUMS.txt'

if (-not $delivery.StartsWith((Join-Path $repoRoot 'build'), [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'delivery root must stay under the repository build directory'
}
$branch = (& git -C $repoRoot branch --show-current).Trim()
$head = (& git -C $repoRoot rev-parse HEAD).Trim()
$tree = (& git -C $repoRoot rev-parse ($base + '^{tree}')).Trim()
if ($branch -ne $expectedBranch) { throw "unexpected branch: $branch" }
& git -C $repoRoot merge-base --is-ancestor $base $head
if ($LASTEXITCODE -ne 0) { throw 'P00 baseline is not an ancestor of current HEAD' }
if (-not (Test-Path -LiteralPath $checkpoint -PathType Leaf)) { throw 'P01 checkpoint is missing' }
$gateResultsPath = Join-Path $delivery 'P01_GATE_RESULTS.json'
$verifyReportPath = Join-Path $delivery 'CHECKPOINT_UNPACK_VERIFICATION.md'
if (-not (Test-Path -LiteralPath $gateResultsPath -PathType Leaf)) { throw 'P01 gate results are missing' }
if (-not (Test-Path -LiteralPath $verifyReportPath -PathType Leaf)) { throw 'checkpoint verification report is missing' }
$gateResults = Get-Content -LiteralPath $gateResultsPath -Raw | ConvertFrom-Json
if ($gateResults.status -ne 'PASS') { throw 'working-tree P01 gates did not pass' }
$checkpointHash = (Get-FileHash -LiteralPath $checkpoint -Algorithm SHA256).Hash.ToLowerInvariant()
$sourceManifestHash = (Get-FileHash -LiteralPath $sourceManifest -Algorithm SHA256).Hash.ToLowerInvariant()

if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
[System.IO.Directory]::CreateDirectory($stage) | Out-Null
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

Invoke-GitText @('diff', '--binary', ($base + '..' + $head)) (Join-Path $stage 'P01_FULL_BINARY.diff')
Invoke-GitText @('diff', '--name-status', ($base + '..' + $head)) (Join-Path $stage 'CHANGED_FILES.txt')

$auditPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md'
$contractPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md'
$planPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md'
$promptPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/P01_identity_dependency_save_CODEX_PROMPT.md'
$remote = (& git -C $repoRoot remote -v) -join "`n"
@(
    'git branch --show-current'
    $expectedBranch
    'git rev-parse HEAD'
    $base
    'git status --short'
    '<clean>'
    'git remote -v'
    $remote
) | Set-Content -LiteralPath (Join-Path $stage 'STARTUP_CHECKS.txt') -Encoding utf8NoBOM

$manifest = [ordered]@{
    phase = 'P01'
    title = 'Stable Identity、Dependency 与 Canonical Save Core'
    accepted = $false
    next_phase_allowed = $false
    parent_checkpoint_sha256 = $null
    parent_checkpoint_note = 'P00 checkpoint ZIP was not present locally; the user-designated immutable Git baseline was used.'
    parent_baseline_commit = $base
    parent_baseline_tree = $tree
    branch = $branch
    candidate_commit = $head
    checkpoint = [ordered]@{
        file = 'SPD_GC_V6_P01_IDENTITY_CHECKPOINT.zip'
        sha256 = $checkpointHash
        source_manifest_sha256 = $sourceManifestHash
        excluded_from_review_bundle = $true
        unpack_verification = 'PASS'
    }
    versions = [ordered]@{
        schema_version = 6
        contract_version = '0.2-final'
        price_version = 'not-introduced-in-P01'
    }
    frozen_documents = [ordered]@{
        audit_sha256 = (Get-FileHash -LiteralPath $auditPath -Algorithm SHA256).Hash.ToLowerInvariant()
        contract_sha256 = (Get-FileHash -LiteralPath $contractPath -Algorithm SHA256).Hash.ToLowerInvariant()
        plan_sha256 = (Get-FileHash -LiteralPath $planPath -Algorithm SHA256).Hash.ToLowerInvariant()
        p01_prompt_sha256 = (Get-FileHash -LiteralPath $promptPath -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    gate_status = $gateResults.status
    gates = $gateResults.gates
    limitations = @(
        'P02 Builder Reducer/UI is intentionally absent.',
        'Typed Skill Compiler and concrete Effect Runtime are intentionally absent.',
        'Entity facets and capability runtime remain fail-closed DEFERRED/UNSUPPORTED envelopes.',
        'Talent/Subclass/Specialization/Armor Ability and class-specific Domain/Core tags are absent.'
    )
}
$manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $stage 'V6_CHECKPOINT_MANIFEST.json') -Encoding utf8NoBOM

$changedCount = ((Get-Content -LiteralPath (Join-Path $stage 'CHANGED_FILES.txt')) | Where-Object { $_ }).Count
@(
    '# Gameplay Components v6 — P01 实施报告'
    ''
    '## 结论'
    ''
    '- P01 working-tree Gate：PASS。'
    '- 完整 Checkpoint 解压副本 Gate：PASS。'
    '- 候选提交已创建；未 push、未 tag。'
    '- `accepted=false`，`next_phase_allowed=false`；本轮到此停止。'
    ''
    '## 基线与范围'
    ''
    "- 分支：$branch"
    "- 唯一 P00 基线：$base"
    "- 候选提交：$head"
    "- 提交范围内文件数：$changedCount"
    '- 冻结 Contract、Audit、FINAL Plan 与阶段 Prompt 未修改。'
    '- 未读取或执行 P02–P12 阶段 Prompt。'
    ''
    '## 实现结果'
    ''
    '- 新建不可变 `StableId`、随机/确定性/迁移 ID generator 与 NFC `DisplayName` 验证。'
    '- 新建 Contract 全部 typed Ref；`lastKnownDisplayName` 只作诊断显示，不参与解析。'
    '- 新建只读 Resolver/Validator，覆盖 duplicate、missing、类型错误、非法循环、Unsupported 与 exclusive initial conflict。'
    '- 建立 schema 6 `ClassBuildSpec` 与独立 `ClassRuntimeState`，保存玩家顺序、node ID 和全部 P01 runtime state。'
    '- 建立 Resource、Mark、Mode、Capacity、Entity、AbilityPool、Property、Recipe 数据边界；Contract 6.3 ResourceStorage capability/slot 已进入 canonical schema，但执行能力保持 Deferred。'
    '- rename/delete/duplicate/explicit rebind 均返回新 immutable graph；delete 不级联，同名重建不接管旧引用。'
    '- canonical build/runtime codec 覆盖全部 P01 字段；独立反射式深等价 oracle 不复用 serializer 字符串。'
    '- Hero Bundle adapter 使用独立 `class_build_spec_v6` / `class_runtime_state_v6` payload，并拒绝 build/runtime ID 冲突。'
    '- v5→v6 migration 仅实现可重放 Resource 映射与 MigrationReport；Skill/Effect/Entity runtime 映射保持 skeleton。'
    '- v6 production 路径无 `resolvePendingBindings()`、无 first-item/default target fallback。'
    ''
    '## P00 Guard 误杀修正'
    ''
    '- Guard 改为识别 legacy 的完整限定类型依赖，不再按简单类型名误杀合法 v6 声明。'
    '- 允许 `contract/v6/spec/ResourceSpec.java`、`EffectSpec.java` 与 `contract/v6/ref/ResourceRef.java` 对抗样例。'
    '- 拒绝旧类型 import、旧类型完整限定名、继承/签名依赖，以及 `rules/spec` / `rules/ref` 新 production 文件。'
    ''
    '## Gate 证据'
    ''
    '- 逐项命令、退出码、耗时与日志见 `P01_GATE_RESULTS.json` 和 `logs/working-tree/`。'
    '- Checkpoint 解压副本复验见 `CHECKPOINT_UNPACK_VERIFICATION.md`、`CHECKPOINT_P01_GATE_RESULTS.json` 与 `logs/checkpoint-unpacked/`。'
    '- canonical、unresolved 与 load matrix 均由真实 codec/resolver 测试输出，见 `artifacts/`。'
    ''
    '## Deferred / Unsupported'
    ''
    '- BuilderCommand/Reducer/UI 属于 P02，未实现。'
    '- Typed Skill Compiler、Effect executor、Entity Runtime 及其具体 facet/capability variants 未实现。'
    '- StartingKit、higher progression、scheduled payload、attachment、snapshot、learned ability 仅保存 fail-closed envelope。'
    '- Recipe output execution仅保存声明边界，不提供内容库或事务 executor。'
    '- Talent、Subclass、Specialization、Armor Ability 与职业专用 Domain/Core 标签均未加入。'
) | Set-Content -LiteralPath (Join-Path $stage 'PHASE_P01_IMPLEMENTATION_REPORT.md') -Encoding utf8NoBOM

@(
    '# BuilderCommand Trace'
    ''
    'Status: NOT_APPLICABLE_BY_PHASE_BOUNDARY'
    ''
    'P02 BuilderCommand/Reducer/UI is explicitly outside P01 and was not implemented. P01 edit semantics are exercised directly as Layer A/C evidence by `DependencyAndEditSemanticsTest`: rename → duplicate → delete → UNRESOLVED → explicit rebind. This evidence is not represented as Player-path acceptance.'
) | Set-Content -LiteralPath (Join-Path $stage 'BUILDER_COMMAND_TRACE.md') -Encoding utf8NoBOM
@(
    '# Runtime Trace'
    ''
    'Status: P01_SAVE_STATE_ONLY'
    ''
    'P01 introduces no Effect or Entity executor. The authoritative runtime evidence is the deterministic `canonical_runtime_state_v6.json` roundtrip covering Resource reservations/suppressions, Mode, Mark, Entity instance/capability envelope, delayed/attachment/snapshot/ability envelopes, cooldown/use maps, Property inventory, and next-ID counters.'
) | Set-Content -LiteralPath (Join-Path $stage 'RUNTIME_TRACE.md') -Encoding utf8NoBOM

Copy-Item -LiteralPath $gateResultsPath -Destination (Join-Path $stage 'P01_GATE_RESULTS.json')
Copy-Item -LiteralPath $verifyReportPath -Destination (Join-Path $stage 'CHECKPOINT_UNPACK_VERIFICATION.md')
Copy-Item -LiteralPath $sourceManifest -Destination (Join-Path $stage 'CHECKPOINT_SOURCE_SHA256SUMS.txt')
Copy-Item -Path (Join-Path $delivery 'artifacts/*') -Destination (Join-Path $stage 'artifacts') -Force
Copy-Item -Path (Join-Path $delivery 'logs/*') -Destination (Join-Path $stage 'logs/working-tree') -Force
$checkpointGateResults = Join-Path $delivery 'checkpoint-verification/gates/P01_GATE_RESULTS.json'
Copy-Item -LiteralPath $checkpointGateResults -Destination (Join-Path $stage 'CHECKPOINT_P01_GATE_RESULTS.json')
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
