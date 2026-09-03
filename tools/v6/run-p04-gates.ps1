param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$Output,
    [string]$JavaHome = 'C:\User\Environment\Java\jdk-24',
    [string]$AndroidSdk = 'C:\User\Application\Android\SDK'
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$deliveryRoot = [System.IO.Path]::GetFullPath($Output)
$logRoot = Join-Path $deliveryRoot 'logs'
$artifactRoot = Join-Path $deliveryRoot 'artifacts'
[System.IO.Directory]::CreateDirectory($logRoot) | Out-Null
[System.IO.Directory]::CreateDirectory($artifactRoot) | Out-Null
Get-ChildItem -LiteralPath $logRoot -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem -LiteralPath $artifactRoot -File -ErrorAction SilentlyContinue | Remove-Item -Force

$gradle = Join-Path $repoRoot 'gradlew.bat'
$pwsh = (Get-Process -Id $PID).Path
$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:P04_ARTIFACT_DIR = $artifactRoot
$failed = [System.Collections.Generic.List[string]]::new()

function Invoke-RecordedGate {
    param([string]$Id, [string]$Executable, [string[]]$Arguments)
    $started = [DateTimeOffset]::Now
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $Executable
    $info.WorkingDirectory = $repoRoot
    $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
    $process = [System.Diagnostics.Process]::new(); $process.StartInfo = $info
    $null = $process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync(); $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $stdout = $stdoutTask.GetAwaiter().GetResult(); $stderr = $stderrTask.GetAwaiter().GetResult()
    $ended = [DateTimeOffset]::Now
    $status = if ($process.ExitCode -eq 0) { 'PASS' } else { 'FAIL' }
    $record = [ordered]@{
        test_id = $Id
        command = ((@($Executable) + $Arguments) -join ' ')
        working_directory = $repoRoot
        started_at = $started.ToString('o')
        ended_at = $ended.ToString('o')
        duration_seconds = [Math]::Round(($ended - $started).TotalSeconds, 3)
        exit_code = $process.ExitCode
        status = $status
        stdout = $stdout
        stderr = $stderr
    }
    $record | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $logRoot ($Id + '.json')) -Encoding utf8NoBOM
    @("test_id: $Id", "command: $($record.command)", "working_directory: $repoRoot", "started_at: $($record.started_at)", "ended_at: $($record.ended_at)", "exit_code: $($record.exit_code)", "status: $status", '--- stdout ---', $stdout, '--- stderr ---', $stderr) |
        Set-Content -LiteralPath (Join-Path $logRoot ($Id + '.log')) -Encoding utf8NoBOM
    Write-Output "$Id $status exit=$($process.ExitCode)"
    if ($process.ExitCode -ne 0) { $failed.Add($Id) }
}

Invoke-RecordedGate 'p04-source-tree' $pwsh @('-NoProfile', '-File', (Join-Path $repoRoot 'tools/v6/verify-p04-source-tree.ps1'), '-Root', $repoRoot)
Invoke-RecordedGate 'p04-architecture-guard' $gradle @('--console=plain', '--rerun-tasks', ':core:test', '--tests', 'com.shatteredpixel.shatteredpixeldungeon.architecture.GameplayComponentsV6ArchitectureTest', '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.P03TypedSkillArchitectureTest', '--no-daemon')
Invoke-RecordedGate 'p04-completion-evidence' $gradle @('--console=plain', '--rerun-tasks', ':core:test', '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.P04ImplementationEvidenceTest', '--no-daemon')
Invoke-RecordedGate 'p00-p04-contract-tests' $gradle @('--console=plain', '--rerun-tasks', ':core:test', '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.*', '--no-daemon')
Invoke-RecordedGate 'legacy-smoke' $gradle @('--console=plain', '--rerun-tasks', ':core:test', '--tests', 'com.shatteredpixel.shatteredpixeldungeon.qa.LegacyGameplaySmokeTest', '--no-daemon')
Invoke-RecordedGate 'core-all-tests' $gradle @('--console=plain', '--rerun-tasks', ':core:test', '--no-daemon')
Invoke-RecordedGate 'headless-all-tests' $gradle @('--console=plain', '--rerun-tasks', ':headless:test', '--no-daemon')
Invoke-RecordedGate 'headless-qa' $gradle @('--console=plain', '--rerun-tasks', ':headless:headlessQa', '--no-daemon')
Invoke-RecordedGate 'build-integrity-qa' $gradle @('--console=plain', '--rerun-tasks', ':headless:buildIntegrityQa', '--no-daemon')
Invoke-RecordedGate 'desktop-jvm-compile' $gradle @('--console=plain', '--rerun-tasks', ':desktop:compileJava', '--no-daemon')
Invoke-RecordedGate 'android-assemble-debug' $gradle @('--console=plain', '--rerun-tasks', ':android:assembleDebug', '--no-daemon')

$records = @()
Get-ChildItem -LiteralPath $logRoot -Filter '*.json' | Sort-Object Name | ForEach-Object {
    $value = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    $records += [ordered]@{ test_id=$value.test_id; command=$value.command; started_at=$value.started_at; ended_at=$value.ended_at; duration_seconds=$value.duration_seconds; exit_code=$value.exit_code; status=$value.status; log=('logs/' + $value.test_id + '.log') }
}
$result = [ordered]@{
    phase = 'P04'
    baseline = 'fac6261f09de340c64fd1c2fdc82f6dea3024570'
    generated_at = [DateTimeOffset]::Now.ToString('o')
    java_home = $JavaHome
    android_sdk = $AndroidSdk
    status = if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' }
    failed = @($failed)
    gates = $records
}
$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $deliveryRoot 'P04_GATE_RESULTS.json') -Encoding utf8NoBOM
if ($failed.Count -gt 0) { Write-Error ('P04 gates failed: ' + ($failed -join ', ')); exit 1 }
Write-Output 'P04_GATES_PASS'
