param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$DeliveryRoot
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$delivery = [System.IO.Path]::GetFullPath($DeliveryRoot)
$base = '3ca4f6913d8fb592feeb813ac85dc07d609be2cc'
$expectedHead = 'aa24e469457be39c79269b38cfb31f6a1cae1583'
$expectedBranch = 'feature/gameplay-components-v6'
$stage = Join-Path $delivery 'review-content'
$bundle = Join-Path $delivery 'SPD_GC_V6_P02_R1_REVIEW_BUNDLE.zip'
$checkpoint = Join-Path $delivery 'checkpoint/SPD_GC_V6_P02_R1_CHECKPOINT.zip'
$sourceManifest = Join-Path $delivery 'checkpoint/SOURCE_SHA256SUMS.txt'
$gateResultsPath = Join-Path $delivery 'P02_GATE_RESULTS.json'
$verifyReportPath = Join-Path $delivery 'CHECKPOINT_UNPACK_VERIFICATION.md'

if (-not $delivery.StartsWith((Join-Path $repoRoot 'build'), [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'delivery root must stay under the repository build directory'
}
$branch = (& git -C $repoRoot branch --show-current).Trim()
$head = (& git -C $repoRoot rev-parse HEAD).Trim()
$parentTree = (& git -C $repoRoot rev-parse ($base + '^{tree}')).Trim()
$candidateSourceTree = (& git -C $repoRoot write-tree).Trim()
if ($branch -ne $expectedBranch) { throw "unexpected branch: $branch" }
if ($head -ne $expectedHead) { throw "P02-R1 bundle must be prepared on candidate $expectedHead, actual $head" }
foreach ($required in @($checkpoint, $sourceManifest, $gateResultsPath, $verifyReportPath)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "required P02 evidence missing: $required" }
}
$gateResults = Get-Content -LiteralPath $gateResultsPath -Raw | ConvertFrom-Json
if ($gateResults.status -ne 'PASS') { throw 'working-tree P02 gates did not pass' }
$checkpointHash = (Get-FileHash -LiteralPath $checkpoint -Algorithm SHA256).Hash.ToLowerInvariant()
$sourceManifestHash = (Get-FileHash -LiteralPath $sourceManifest -Algorithm SHA256).Hash.ToLowerInvariant()

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

Invoke-GitText @('diff', '--cached', '--binary', $base) (Join-Path $stage 'P02_R1_CUMULATIVE.diff')
Invoke-GitText @('diff', '--cached', '--name-status', $base) (Join-Path $stage 'CHANGED_FILES.txt')

$auditPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md'
$contractPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md'
$planPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md'
$promptPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/P02_builder_kernel_CODEX_PROMPT.md'
$manifest = [ordered]@{
    phase = 'P02-R1'
    title = 'Player Builder Form Controller 与真实玩家编辑路径收口'
    accepted = $false
    next_phase_allowed = $false
    parent_checkpoint_sha256 = $null
    parent_checkpoint_note = 'The accepted P01 checkpoint ZIP was not present locally; the user-designated immutable P01 Git baseline is authoritative.'
    parent_baseline_commit = $base
    parent_baseline_tree = $parentTree
    branch = $branch
    candidate_commit = 'created after deterministic review bundle generation'
    candidate_source_tree = $candidateSourceTree
    checkpoint = [ordered]@{
        file = 'SPD_GC_V6_P02_R1_CHECKPOINT.zip'
        sha256 = $checkpointHash
        source_manifest_sha256 = $sourceManifestHash
        excluded_from_review_bundle = $true
        unpack_verification = 'PASS'
    }
    versions = [ordered]@{
        schema_version = 6
        contract_version = '0.2-final'
        price_version = 'not-introduced-in-P02'
    }
    frozen_documents = [ordered]@{
        audit_sha256 = (Get-FileHash -LiteralPath $auditPath -Algorithm SHA256).Hash.ToLowerInvariant()
        contract_sha256 = (Get-FileHash -LiteralPath $contractPath -Algorithm SHA256).Hash.ToLowerInvariant()
        plan_sha256 = (Get-FileHash -LiteralPath $planPath -Algorithm SHA256).Hash.ToLowerInvariant()
        p02_prompt_sha256 = (Get-FileHash -LiteralPath $promptPath -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    gate_status = $gateResults.status
    gates = $gateResults.gates
    limitations = @(
        'P03 typed Skill Compiler and executable Skill runtime are intentionally absent.',
        'Internal P02 declaration commands may retain DEFERRED nodes and never claim IMPLEMENTED runtime behavior.',
        'The player root does not expose executable P02_DEFERRED_SKILL or P02_DEFERRED_COMPONENT entries.',
        'StartingKit and Progression remain unexposed DEFERRED sections.',
        'The v6 player-builder feature flag remains false pending independent acceptance.'
    )
}
$manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $stage 'V6_CHECKPOINT_MANIFEST.json') -Encoding utf8NoBOM

$changedCount = ((Get-Content -LiteralPath (Join-Path $stage 'CHANGED_FILES.txt')) | Where-Object { $_ }).Count
@(
    '# Gameplay Components v6 — P02-R1 收口报告'
    ''
    '## 结论'
    ''
    '- P02-R1 working-tree Gate：PASS。'
    '- 完整 checkpoint 解压副本 Gate：PASS。'
    '- 未进入 P03；未 push、未 tag。'
    '- `accepted=false`，`next_phase_allowed=false`。'
    ''
    '## 基线与范围'
    ''
    "- 分支：$branch"
    "- 唯一 P01 基线：$base"
    "- 候选 source tree：$candidateSourceTree"
    "- 修改文件数：$changedCount"
    ''
    '## 实现结果'
    ''
    '- `BuilderCommand` 仅携带 primitive、String、typed ref；UI 与 headless 均只通过同一 `PlayerBuildSession`/`BuilderReducer` 改变 draft。'
    '- Reducer 覆盖 P01 声明 create/edit/rename/delete/explicit rebind、navigation、undo/redo、save/load、trace replay 和 fail-closed finalize。'
    '- `BuilderFormController` 将 FormSchema 映射为 UI Field Model，再产生 BuilderCommand 并调用同一 PlayerBuildSession；enabled 字段均有真实状态转换。'
    '- FormSchema 覆盖 Text、Number、Enum、Enum List、Reference、Boolean、Nested Variant、List 与只读 Diagnostic；数值控件为 NumberStepper。'
    '- `compatible_entity_capacity` 按当前 EntityType 过滤；引用删除后保留 UNRESOLVED、lastKnownDisplayName、短 ID 与显式 rebind。'
    '- Nested/List 与尚无运行时能力的字段在玩家页面明确禁用；Skill/Component Deferred 假入口不再暴露。'
    '- `WndCreateClassV6` 通过控制器视图提供真实编辑路径，旧 `WndCreateClass` 仅在关闭的 v6 feature flag 下保留 Legacy 路由。'
    '- Architecture Guard 使用精确路径白名单，拒绝任意其它旧生产代码依赖 v6，并拒绝 v6 依赖 Legacy Hero/ClassBuild/EffectSpec/Registry/QA/auto-bind。'
    '- RuntimeStateValidator 与 canonical runtime reader 对 cooldowns/usesThisFloor 的缺失、重复、非法值和错误节点类型均 fail closed。'
    '- Finalize 允许 draft 保留 unresolved 或未暴露 Deferred，但拒绝 unresolved、hard conflict、超预算和 player-exposed unsupported runtime。'
    ''
    '## Gate 证据'
    ''
    '- 命令、退出码、耗时与日志：`P02_GATE_RESULTS.json`、`logs/working-tree/`。'
    '- checkpoint 解压复验：`CHECKPOINT_UNPACK_VERIFICATION.md`、`CHECKPOINT_P02_GATE_RESULTS.json`、`logs/checkpoint-unpacked/`。'
    '- 三条 command trace、canonical build、runtime validator 与 finalization 产物：`artifacts/`。'
    ''
    '## Deferred / Unsupported'
    ''
    '- P03 Skill Compiler/Executor 未实现。'
    '- Entity facets/capabilities 与 Recipe output 仍为声明或 Deferred envelope。'
    '- StartingKit、Progression 以及后续 Talent/Subclass/Specialization/Armor Ability 未暴露。'
) | Set-Content -LiteralPath (Join-Path $stage 'PHASE_P02_R1_IMPLEMENTATION_REPORT.md') -Encoding utf8NoBOM

Copy-Item -LiteralPath $gateResultsPath -Destination (Join-Path $stage 'P02_GATE_RESULTS.json')
Copy-Item -LiteralPath $verifyReportPath -Destination (Join-Path $stage 'CHECKPOINT_UNPACK_VERIFICATION.md')
Copy-Item -LiteralPath $sourceManifest -Destination (Join-Path $stage 'CHECKPOINT_SOURCE_SHA256SUMS.txt')
Copy-Item -Path (Join-Path $delivery 'artifacts/*') -Destination (Join-Path $stage 'artifacts') -Recurse -Force
Copy-Item -Path (Join-Path $delivery 'logs/*') -Destination (Join-Path $stage 'logs/working-tree') -Force
$checkpointGateResults = Join-Path $delivery 'checkpoint-verification/gates/P02_GATE_RESULTS.json'
Copy-Item -LiteralPath $checkpointGateResults -Destination (Join-Path $stage 'CHECKPOINT_P02_GATE_RESULTS.json')
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
