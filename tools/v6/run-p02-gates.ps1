param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$Output
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath($Root)
$deliveryRoot = [System.IO.Path]::GetFullPath($Output)
$logRoot = Join-Path $deliveryRoot 'logs'
$artifactRoot = Join-Path $deliveryRoot 'artifacts'
[System.IO.Directory]::CreateDirectory($logRoot) | Out-Null
[System.IO.Directory]::CreateDirectory($artifactRoot) | Out-Null
Get-ChildItem -LiteralPath $logRoot -File -ErrorAction SilentlyContinue | Remove-Item -Force

$javaHome = 'D:\Environment\Java\jdk-21'
$gradle = Join-Path $repoRoot 'gradlew.bat'
$pwsh = 'D:\Environment\PowerShell\7\pwsh.exe'
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = 'D:\Android\android-sdk'
$env:ANDROID_SDK_ROOT = 'D:\Android\android-sdk'
$env:P02_ARTIFACT_DIR = $artifactRoot
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
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $info
    $null = $process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    $ended = [DateTimeOffset]::Now
    $status = if ($process.ExitCode -eq 0) { 'PASS' } else { 'FAIL' }
    $rootCause = ''
    $combined = $stdout + "`n" + $stderr
    if ($process.ExitCode -ne 0) {
        if ($combined -match 'Could not resolve|UnknownHost|network|download') { $rootCause = 'DEPENDENCY_OR_NETWORK' }
        elseif ($combined -match 'SDK location not found|ANDROID_HOME|Android SDK') { $rootCause = 'ANDROID_SDK' }
        elseif ($combined -match 'Compilation failed|compileJava FAILED|compileTestJava FAILED') { $rootCause = 'SOURCE_COMPILATION' }
        elseif ($combined -match 'There were failing tests|test FAILED|AssertionError') { $rootCause = 'TEST_ASSERTION' }
        else { $rootCause = 'COMMAND_FAILURE' }
    }
    $record = [ordered]@{
        test_id = $Id
        command = ((@($Executable) + $Arguments) -join ' ')
        executable = $Executable
        arguments = @($Arguments)
        working_directory = $repoRoot
        started_at = $started.ToString('o')
        ended_at = $ended.ToString('o')
        duration_seconds = [Math]::Round(($ended - $started).TotalSeconds, 3)
        exit_code = $process.ExitCode
        status = $status
        root_cause = $rootCause
        stdout = $stdout
        stderr = $stderr
    }
    $record | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $logRoot ($Id + '.json')) -Encoding utf8NoBOM
    @(
        "test_id: $Id"
        "command: $($record.command)"
        "working_directory: $repoRoot"
        "started_at: $($record.started_at)"
        "ended_at: $($record.ended_at)"
        "exit_code: $($record.exit_code)"
        "status: $status"
        "root_cause: $rootCause"
        '--- stdout ---'
        $stdout
        '--- stderr ---'
        $stderr
    ) | Set-Content -LiteralPath (Join-Path $logRoot ($Id + '.log')) -Encoding utf8NoBOM
    Write-Output "$Id $status exit=$($process.ExitCode)"
    if ($process.ExitCode -ne 0) { $failed.Add($Id) }
}

Invoke-RecordedGate 'p02-source-tree' $pwsh @(
    '-NoProfile', '-File', (Join-Path $repoRoot 'tools/v6/verify-p02-source-tree.ps1'), '-Root', $repoRoot
)
Invoke-RecordedGate 'p02-architecture-guard' $gradle @(
    '--console=plain', '--rerun-tasks', ':core:test',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.architecture.GameplayComponentsV6ArchitectureTest'
)
Invoke-RecordedGate 'p02-r1-form-controller' $gradle @(
    '--console=plain', '--rerun-tasks', ':core:test',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.BuilderFormControllerTest'
)
Invoke-RecordedGate 'p02-builder-kernel' $gradle @(
    '--console=plain', '--rerun-tasks', ':core:test',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.Builder*',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.FormSchemaUiFoundationTest',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.FinalizeBuildContractTest',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.RuntimeNodeCounterValidationTest',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.P02DeliveryArtifactTest'
)
Invoke-RecordedGate 'p01-p02-contract-tests' $gradle @(
    '--console=plain', '--rerun-tasks', ':core:test',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.*'
)
Invoke-RecordedGate 'legacy-smoke' $gradle @(
    '--console=plain', '--rerun-tasks', ':core:test',
    '--tests', 'com.shatteredpixel.shatteredpixeldungeon.qa.LegacyGameplaySmokeTest'
)
Invoke-RecordedGate 'core-all-tests' $gradle @('--console=plain', '--rerun-tasks', ':core:test')
Invoke-RecordedGate 'headless-all-tests' $gradle @('--console=plain', '--rerun-tasks', ':headless:test')
Invoke-RecordedGate 'headless-qa' $gradle @('--console=plain', '--rerun-tasks', ':headless:headlessQa')
Invoke-RecordedGate 'desktop-jvm-compile' $gradle @('--console=plain', '--rerun-tasks', ':desktop:compileJava')
Invoke-RecordedGate 'android-assemble-debug' $gradle @('--console=plain', '--rerun-tasks', ':android:assembleDebug')

$records = @()
Get-ChildItem -LiteralPath $logRoot -Filter '*.json' | Sort-Object Name | ForEach-Object {
    $value = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    $records += [ordered]@{
        test_id = $value.test_id
        command = $value.command
        started_at = $value.started_at
        ended_at = $value.ended_at
        duration_seconds = $value.duration_seconds
        exit_code = $value.exit_code
        status = $value.status
        root_cause = $value.root_cause
        log = ('logs/' + $value.test_id + '.log')
    }
}
$result = [ordered]@{
    phase = 'P02-R1'
    baseline = '3ca4f6913d8fb592feeb813ac85dc07d609be2cc'
    generated_at = [DateTimeOffset]::Now.ToString('o')
    java_home = $javaHome
    android_sdk = 'D:\Android\android-sdk'
    status = if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' }
    failed = @($failed)
    gates = $records
}
$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $deliveryRoot 'P02_GATE_RESULTS.json') -Encoding utf8NoBOM

if ($failed.Count -gt 0) {
    Write-Error ('P02 gates failed: ' + ($failed -join ', '))
    exit 1
}
Write-Output 'P02_GATES_PASS'
