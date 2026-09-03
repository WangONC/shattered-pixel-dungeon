param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$DeliveryRoot
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$delivery = [System.IO.Path]::GetFullPath($DeliveryRoot)
$base = 'fac6261f09de340c64fd1c2fdc82f6dea3024570'
$expectedBranch = 'feature/gameplay-components-v6'
$expectedMessage = '实现 v6 核心效果、资源事务与生命值边界'
$stage = Join-Path $delivery 'review-content'
$bundle = Join-Path $delivery 'SPD_GC_V6_P04_REVIEW_BUNDLE.zip'
$checkpoint = Join-Path $delivery 'checkpoint/SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip'
$sourceManifest = Join-Path $delivery 'checkpoint/SOURCE_SHA256SUMS.txt'
$gateResultsPath = Join-Path $delivery 'working-tree/P04_GATE_RESULTS.json'
$checkpointGateResultsPath = Join-Path $delivery 'checkpoint-verification/gates/P04_GATE_RESULTS.json'
$verifyReportPath = Join-Path $delivery 'CHECKPOINT_UNPACK_VERIFICATION.md'

if (-not $delivery.StartsWith((Join-Path $repoRoot 'build'), [System.StringComparison]::OrdinalIgnoreCase)) { throw 'delivery root must stay under repository build' }
$branch = (& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot branch --show-current).Trim()
$head = (& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot rev-parse HEAD).Trim()
$parent = (& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot rev-parse HEAD^).Trim()
$message = (& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot show -s --format=%s HEAD)
$status = @(& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot status --porcelain)
if ($branch -ne $expectedBranch) { throw "unexpected branch: $branch" }
if ($parent -ne $base) { throw "P04 candidate parent must be $base, actual $parent" }
if ($message -ne $expectedMessage) { throw "unexpected P04 commit message: $message" }
if ($status.Count -gt 0) { throw 'review bundle requires a clean committed source tree' }
foreach ($required in @($checkpoint, $sourceManifest, $gateResultsPath, $checkpointGateResultsPath, $verifyReportPath)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "required P04 evidence missing: $required" }
}
$gateResults = Get-Content -LiteralPath $gateResultsPath -Raw | ConvertFrom-Json
$checkpointGateResults = Get-Content -LiteralPath $checkpointGateResultsPath -Raw | ConvertFrom-Json
if ($gateResults.status -ne 'PASS' -or $checkpointGateResults.status -ne 'PASS') { throw 'working-tree and checkpoint gates must pass' }

if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
[System.IO.Directory]::CreateDirectory((Join-Path $stage 'artifacts')) | Out-Null
[System.IO.Directory]::CreateDirectory((Join-Path $stage 'logs/working-tree')) | Out-Null
[System.IO.Directory]::CreateDirectory((Join-Path $stage 'logs/checkpoint-unpacked')) | Out-Null

function Invoke-GitText([string[]]$Arguments, [string]$Output) {
    $info = [System.Diagnostics.ProcessStartInfo]::new(); $info.FileName = 'git'; $info.WorkingDirectory = $repoRoot; $info.UseShellExecute = $false; $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
    $process = [System.Diagnostics.Process]::new(); $process.StartInfo = $info; $null = $process.Start()
    $stdout = $process.StandardOutput.ReadToEnd(); $stderr = $process.StandardError.ReadToEnd(); $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "git command failed: $stderr" }
    [System.IO.File]::WriteAllText($Output, $stdout, [System.Text.UTF8Encoding]::new($false))
}
Invoke-GitText @('diff', '--binary', $base, $head) (Join-Path $stage 'P04_CUMULATIVE.diff')
Invoke-GitText @('diff', '--name-status', $base, $head) (Join-Path $stage 'CHANGED_FILES.txt')

$checkpointHash = (Get-FileHash -LiteralPath $checkpoint -Algorithm SHA256).Hash.ToLowerInvariant()
$sourceManifestHash = (Get-FileHash -LiteralPath $sourceManifest -Algorithm SHA256).Hash.ToLowerInvariant()
$parentTree = (& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot rev-parse ($base + '^{tree}')).Trim()
$candidateTree = (& git -c safe.directory='C:/User/Code/shattered-pixel-dungeon' -C $repoRoot rev-parse 'HEAD^{tree}').Trim()
$auditPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md'
$contractPath = Join-Path $repoRoot 'docs/SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md'
$planPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md'
$promptPath = Join-Path $repoRoot 'docs/SPD_GAMEPLAY_V6_DEV_PLAN/P04_core_effects_resource_hp_CODEX_PROMPT.md'
$manifest = [ordered]@{
    phase = 'P04'
    title = 'v6 核心效果、资源事务与生命值边界'
    accepted = $false
    next_phase_allowed = $false
    parent_checkpoint_sha256 = $null
    parent_checkpoint_status = 'NOT_PROVIDED; the user-authorized exact Git parent baseline is authoritative'
    parent_baseline_commit = $base
    cumulative_diff_baseline = $base
    parent_baseline_tree = $parentTree
    branch = $branch
    candidate_commit = $head
    candidate_source_tree = $candidateTree
    checkpoint = [ordered]@{ file='SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip'; sha256=$checkpointHash; source_manifest_sha256=$sourceManifestHash; excluded_from_review_bundle=$true; unpack_verification='PASS' }
    versions = [ordered]@{ schema_version=6; contract_version='0.2-final'; price_version='v6-p04-1'; runtime_version='v6-p04-r1-1' }
    frozen_documents = [ordered]@{
        audit_sha256 = (Get-FileHash -LiteralPath $auditPath -Algorithm SHA256).Hash.ToLowerInvariant()
        contract_sha256 = (Get-FileHash -LiteralPath $contractPath -Algorithm SHA256).Hash.ToLowerInvariant()
        plan_sha256 = (Get-FileHash -LiteralPath $planPath -Algorithm SHA256).Hash.ToLowerInvariant()
        p04_prompt_sha256 = (Get-FileHash -LiteralPath $promptPath -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    gate_status = $gateResults.status
    gates = $gateResults.gates
    checkpoint_gate_status = $checkpointGateResults.status
    checkpoint_gates = $checkpointGateResults.gates
    completion_evidence_source = 'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6/P04ImplementationEvidence.java'
    limitations = @(
        'Mandatory Test 1 Skill/ClassOperation/ResourceFlow core is complete; Device Payload parity remains P07 and is not declared COMPLETE.',
        'Mark, Mode, Entity, Transfer, Delay, Echo, Talent, Subclass, Specialization and Armor Ability remain fail-closed for later phases.',
        'Only EXACT_ATOMIC resource conversion and REJECT_IF_WOULD_KILL HP cost are exposed.',
        'Public gameplay and player-builder feature flags remain disabled pending independent acceptance.',
        'No accepted P03 checkpoint ZIP was present locally; the exact parent Git commit supplied by the user was used and no checkpoint hash was invented.'
    )
}
$manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $stage 'V6_CHECKPOINT_MANIFEST.json') -Encoding utf8NoBOM

$changedCount = ((Get-Content -LiteralPath (Join-Path $stage 'CHANGED_FILES.txt')) | Where-Object { $_ }).Count
@('# Gameplay Components v6 — P04 实施报告', '', '## 结论', '', '- P04 working-tree Gate：PASS。', '- 完整 checkpoint 解压副本 Gate：PASS。', '- 未进入 P05；未 push、未 tag。', '- accepted=false，next_phase_allowed=false。', '', '## 基线与范围', '', "- 分支：$branch", "- 唯一父基线 / 累计 Diff 基线：$base", "- 候选提交：$head", "- 候选 source tree：$candidateTree", "- 修改文件数：$changedCount", '', '## 实现结果', '', '- Hero restore 在载入 Spec/State 后重新 resolver、validator、compiler；只安装 executable plan，失败保留 diagnostic。', '- 相同 buildId 且兼容时安装与恢复保留 Resource、cooldown、uses 等运行状态；不一致构筑明确拒绝。', '- 全构筑稳定 ID 唯一性与 Runtime counter node 引用均 fail-closed。', '- 执行顺序冻结为 dependency、target、全部 effect capability、cost preflight、atomic cost、time/cooldown、effect；支付后 BLOCKED 不退款且可追踪。', '- Relation filter 明确区分 ENEMY、ALLY、SELF、NEUTRAL。Primary/Secondary 预检、激活、结果与 cause chain 均进入 trace，cost 只支付一次。', '- 完成 Damage、Status、Movement、Recovery/Defense、ResourceOperation、BuiltinStat/Value/Condition、P04 Delivery/Targeting/Filter/Modifier/Cost。', '- 完成 BasicAttack、ResourceFlow、ActiveResourceOperation、Resource ClassOperation 与 declaration/state-aware HUD。', '- Rage→Focus 由空白 PlayerBuildSession 的真实 commands 构造；Skill、ClassOperation、ResourceFlow 共用 ResourceOperationSpec、ResourceTransaction 与同一 compiled transaction path。', '- HP 保持 Hero built-in stat；Barrier 与 Temporary HP 使用独立防御状态，不能代付 HP cost。', '- 66 个本阶段玩家暴露 variant 均有显式、可实际运行的 Completion Evidence row。', '', '## Gate 证据', '', '- 工作树：P04_GATE_RESULTS.json 与 logs/working-tree/。', '- checkpoint 解压副本：CHECKPOINT_P04_GATE_RESULTS.json 与 logs/checkpoint-unpacked/。', '- Builder command、canonical build/runtime、transaction trace、HUD：artifacts/。', '- Android、Desktop、Core、Headless、Headless QA、Build Integrity、Architecture Guard、Legacy smoke 均有独立退出码。', '', '## Deferred / Unsupported', '', '- Device Payload 的同 executor 验收到 P07；Mandatory Test 1 不宣称最终 COMPLETE。', '- Mark、Mode、Entity、Transfer、Delay/Echo 及职业层级节点继续 fail-closed。', '- ALLOW_LETHAL 未暴露；Resource Convert 仅 EXACT_ATOMIC。', '- public/player-builder feature flag 均保持 false。') |
    Set-Content -LiteralPath (Join-Path $stage 'PHASE_P04_IMPLEMENTATION_REPORT.md') -Encoding utf8NoBOM

Copy-Item -LiteralPath $gateResultsPath -Destination (Join-Path $stage 'P04_GATE_RESULTS.json')
Copy-Item -LiteralPath $checkpointGateResultsPath -Destination (Join-Path $stage 'CHECKPOINT_P04_GATE_RESULTS.json')
Copy-Item -LiteralPath $verifyReportPath -Destination (Join-Path $stage 'CHECKPOINT_UNPACK_VERIFICATION.md')
Copy-Item -LiteralPath $sourceManifest -Destination (Join-Path $stage 'CHECKPOINT_SOURCE_SHA256SUMS.txt')
Copy-Item -Path (Join-Path $delivery 'working-tree/artifacts/*') -Destination (Join-Path $stage 'artifacts') -Recurse -Force
Copy-Item -Path (Join-Path $delivery 'working-tree/logs/*') -Destination (Join-Path $stage 'logs/working-tree') -Force
Copy-Item -Path (Join-Path $delivery 'checkpoint-verification/gates/logs/*') -Destination (Join-Path $stage 'logs/checkpoint-unpacked') -Force

$checksumLines = [System.Collections.Generic.List[string]]::new()
Get-ChildItem -LiteralPath $stage -Recurse -File | Sort-Object FullName | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($stage, $_.FullName).Replace('\', '/')
    $checksumLines.Add("$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant())  $relative")
}
$checksumLines | Set-Content -LiteralPath (Join-Path $stage 'SHA256SUMS.txt') -Encoding utf8NoBOM

Add-Type -AssemblyName System.IO.Compression; Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $bundle) { Remove-Item -LiteralPath $bundle -Force }
$stream = [System.IO.File]::Open($bundle, [System.IO.FileMode]::CreateNew)
try {
    $archive = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try { Get-ChildItem -LiteralPath $stage -Recurse -File | Sort-Object FullName | ForEach-Object { $relative=[System.IO.Path]::GetRelativePath($stage,$_.FullName).Replace('\','/'); $entry=$archive.CreateEntry($relative,[System.IO.Compression.CompressionLevel]::Optimal); $entry.LastWriteTime=[DateTimeOffset]::new(2000,1,1,0,0,0,[TimeSpan]::Zero); $input=[System.IO.File]::OpenRead($_.FullName); $output=$entry.Open(); try{$input.CopyTo($output)}finally{$output.Dispose();$input.Dispose()} } } finally { $archive.Dispose() }
} finally { $stream.Dispose() }
Write-Output "review_bundle=$bundle"
Write-Output "review_bundle_sha256=$((Get-FileHash -LiteralPath $bundle -Algorithm SHA256).Hash.ToLowerInvariant())"
Write-Output "checkpoint_sha256=$checkpointHash"
