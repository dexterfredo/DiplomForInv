$home = $env:LOADER_HOME
$jar = $env:LOADER_JAR
$java = $env:LOADER_JAVA
$pidFile = $env:LOADER_PID_FILE
$log = $env:LOADER_LOG

if (-not $home -or -not $jar -or -not $java) {
  Write-Error 'Missing LOADER_* environment variables'
  exit 1
}

if (Test-Path $pidFile) {
  $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue
  if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
    Write-Host "Already running PID $oldPid. Run stop.bat first."
    exit 1
  }
}

$p = Start-Process -FilePath $java `
  -ArgumentList @("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dapp.home=$home", "-jar", $jar) `
  -WorkingDirectory $home `
  -WindowStyle Hidden `
  -PassThru `
  -RedirectStandardOutput $log `
  -RedirectStandardError $log

$p.Id | Out-File -Encoding utf8 $pidFile
Write-Host "PID $($p.Id) saved to logs\loader.pid"
exit 0
