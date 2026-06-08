# Parses docs/views.txt -> resources/metadata/view_columns.json
$root = Split-Path $PSScriptRoot -Parent
$viewsPath = Join-Path $root "docs\views.txt"
$outPath = Join-Path $root "src\main\resources\metadata\view_columns.json"
New-Item -ItemType Directory -Force -Path (Split-Path $outPath) | Out-Null

$map = @{}
$current = $null
$cols = New-Object System.Collections.Generic.List[string]

foreach ($line in Get-Content $viewsPath) {
    $t = $line.Trim()
    if ($t -match '^CREATE OR REPLACE VIEW tr__data_view\.(v_tr_buff_micex_\w+)') {
        if ($current -and $cols.Count -gt 0) { $map[$current] = @($cols) }
        $current = $Matches[1]
        $cols = New-Object System.Collections.Generic.List[string]
        continue
    }
    if ($null -eq $current) { continue }
    if ($t -match '^\s*FROM\b') {
        if ($cols.Count -gt 0) { $map[$current] = @($cols) }
        $current = $null
        continue
    }
    if ($t -match '(?:^|\s)(cat|buff)\.("?)(\w+)\2(?:\s+AS\s+(\w+))?\s*,?\s*$') {
        $name = if ($Matches[4]) { $Matches[4].ToLower() } else { $Matches[3].ToLower() }
        if (-not $cols.Contains($name)) { [void]$cols.Add($name) }
    }
}

if ($current -and $cols.Count -gt 0) { $map[$current] = @($cols) }
$map | ConvertTo-Json -Depth 5 | Set-Content $outPath -Encoding UTF8
Write-Host "views=$($map.Count) -> $outPath"
