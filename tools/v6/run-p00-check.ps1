param(
    [Parameter(Mandatory = $true)][string]$Id,
    [Parameter(Mandatory = $true)][string]$Executable,
    [Parameter(Mandatory = $true)][string]$WorkingDirectory,
    [Parameter(Mandatory = $true)][string]$LogDirectory,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$CommandArguments
)

$ErrorActionPreference = 'Stop'
$logRoot = [System.IO.Path]::GetFullPath($LogDirectory)
[System.IO.Directory]::CreateDirectory($logRoot) | Out-Null
$started = [DateTimeOffset]::Now

$processInfo = [System.Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = $Executable
$processInfo.WorkingDirectory = [System.IO.Path]::GetFullPath($WorkingDirectory)
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
foreach ($argument in $CommandArguments) { $processInfo.ArgumentList.Add($argument) }

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $processInfo
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

$commandText = @($Executable) + $CommandArguments
$record = [ordered]@{
    test_id = $Id
    command = ($commandText -join ' ')
    executable = $Executable
    arguments = @($CommandArguments)
    working_directory = $processInfo.WorkingDirectory
    started_at = $started.ToString('o')
    ended_at = $ended.ToString('o')
    duration_seconds = [Math]::Round(($ended - $started).TotalSeconds, 3)
    exit_code = $process.ExitCode
    status = $status
    root_cause = $rootCause
    stdout = $stdout
    stderr = $stderr
}

$jsonPath = Join-Path $logRoot ($Id + '.json')
$textPath = Join-Path $logRoot ($Id + '.log')
$record | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding utf8NoBOM
@(
    "test_id: $Id"
    "command: $($record.command)"
    "working_directory: $($record.working_directory)"
    "started_at: $($record.started_at)"
    "ended_at: $($record.ended_at)"
    "exit_code: $($record.exit_code)"
    "status: $status"
    "root_cause: $rootCause"
    '--- stdout ---'
    $stdout
    '--- stderr ---'
    $stderr
) | Set-Content -LiteralPath $textPath -Encoding utf8NoBOM

Write-Output "$Id $status exit=$($process.ExitCode) log=$textPath"
exit $process.ExitCode
