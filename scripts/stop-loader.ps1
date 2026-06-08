$n = 0
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
  ForEach-Object {
    if ($_.CommandLine -and $_.CommandLine -match 'LoaderMicexFX') {
      Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
      Write-Host "[stop.bat] Stopped PID $($_.ProcessId)"
      $n++
    }
  }
if ($n -eq 0) { exit 1 }
exit 0
