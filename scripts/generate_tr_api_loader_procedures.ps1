# Full procedures: tr_buff insert + tr_buff_micex_* insert (no shared register, no PERFORM functions)
$root = Split-Path $PSScriptRoot -Parent
$outPath = Join-Path $root "src\main\resources\sql\generated\19_tr_api_loader_procedures.sql"
$tablesPath = Join-Path $root "docs\tables.txt"
$viewsPath = Join-Path $root "docs\views.txt"
$buffPath = Join-Path $root "docs\buff_target.txt"

function Parse-TablesFile([string]$path) {
    $result = @{}
    $current = $null
    $cols = New-Object System.Collections.Generic.List[string]
    foreach ($line in Get-Content $path) {
        $t = $line.Trim()
        if ($t -match '^CREATE TABLE tr__data_temp\.(tr_buff_micex_\w+)\s*\(') {
            if ($current -and $cols.Count -gt 0) { $result[$current] = @($cols) }
            $current = $Matches[1].ToLower()
            $cols = New-Object System.Collections.Generic.List[string]
            continue
        }
        if ($null -eq $current) { continue }
        if ($t -eq ');' -or $t -eq ')') {
            if ($cols.Count -gt 0) { $result[$current] = @($cols) }
            $current = $null
            continue
        }
        if ($t -match '^"?(\w+)"?\s+(varchar|numeric|timestamp)') {
            $col = $Matches[1].ToLower()
            if ($col -ne 'constraint') { [void]$cols.Add($col) }
        }
    }
    return $result
}

function Parse-ViewsFile([string]$path) {
    $result = @{}
    $current = $null
    $cols = New-Object System.Collections.Generic.List[string]
    foreach ($line in Get-Content $path) {
        $t = $line.Trim()
        if ($t -match '^CREATE OR REPLACE VIEW tr__data_view\.(v_tr_buff_micex_\w+)') {
            if ($current -and $cols.Count -gt 0) { $result[$current] = @($cols) }
            $current = $Matches[1].ToLower()
            $cols = New-Object System.Collections.Generic.List[string]
            continue
        }
        if ($null -eq $current) { continue }
        if ($t -match '^(cat|buff)\.("?)(\w+)\2(?:\s+AS\s+(\w+))?\s*,?$') {
            $name = if ($Matches[4]) { $Matches[4].ToLower() } else { $Matches[3].ToLower() }
            if ($name -ne 'constraint' -and -not $cols.Contains($name)) { [void]$cols.Add($name) }
        }
    }
    if ($current -and $cols.Count -gt 0) { $result[$current] = @($cols) }
    return $result
}

function ViewToTable([string]$viewLegacy) {
    $v = $viewLegacy.Trim().ToUpper()
    if ($v.StartsWith("V_TR_BUFF_MICEX_")) { return "tr_buff_micex_" + $v.Substring("V_TR_BUFF_MICEX_".Length).ToLower() }
    return $v.ToLower()
}

function ViewToPgView([string]$viewLegacy) {
    return "v_" + (ViewToTable $viewLegacy)
}

function McxFunc([string]$f) {
    $x = $f.Trim().ToUpper()
    if ($x.StartsWith("MCX_")) { return "MICEX_" + $x.Substring(4) }
    if (-not $x.StartsWith("MICEX_")) { return "MICEX_" + $x }
    return $x
}

function SqlValForDetailCol([string]$col, [string[]]$tableCols, [string[]]$viewCols, [string]$tableName, [string]$row = 'ior_buff') {
    if ($col -eq 'id') { return 'v_buff_id' }
    if ($col -eq 'text') { return "COALESCE($row.text, '')" }
    if ($tableName -eq 'tr_buff_micex_lotsize' -and $col -in @('insert_date', 'entry_date', 'expiry_date')) {
        if ($col -eq 'insert_date') { return "COALESCE($row.insert_date, NOW())" }
        if ($col -eq 'entry_date') { return "COALESCE($row.entry_date, $row.insert_date, NOW())" }
        return "$row.expiry_date"
    }
    if ($col -eq 'entry_date' -or $col -eq 'insert_date') { return 'NOW()' }
    if ($col -eq 'expiry_date') { return 'NULL' }
    if ($col -like 'raw_*') {
        if ($tableCols -contains $col) { return "$row.$col" }
        return 'NULL'
    }
    $raw = "raw_$col"
    if (($viewCols -contains $col) -or ($tableCols -contains $col)) {
        $hasCol = $viewCols -contains $col
        $hasRaw = ($viewCols -contains $raw) -or ($tableCols -contains $raw)
        if ($hasCol -and $hasRaw) {
            if ($col -match '^(lotsize|decimals)$') {
                return "COALESCE(NULLIF(TRIM($row.$col::text), '')::numeric, NULLIF(TRIM($row.$raw::text), '')::numeric)"
            }
            return "COALESCE(NULLIF(TRIM($row.$col), ''), NULLIF(TRIM($row.$raw), ''))"
        }
        if ($hasRaw) {
            if ($col -match '^(lotsize|decimals)$') {
                return "NULLIF(TRIM($row.$raw::text), '')::numeric"
            }
            return "NULLIF(TRIM($row.$raw), '')"
        }
        if ($hasCol) {
            if ($col -match '^(lotsize|decimals)$') {
                return "NULLIF(TRIM($row.$col::text), '')::numeric"
            }
            if ($col -match '^(varchar|tradeno|orderno)') { return "NULLIF(TRIM($row.$col), '')" }
            return "$row.$col"
        }
    }
    if ($tableCols -contains $col) { return "$row.$col" }
    return 'NULL'
}

$tableCols = Parse-TablesFile $tablesPath
$viewColsMap = Parse-ViewsFile $viewsPath
$buffs = @()
Get-Content $buffPath | Where-Object { $_.Trim() -ne '' } | ForEach-Object {
    $p = $_ -split "`t"
    if ($p.Count -ge 6) {
        $buffs += [PSCustomObject]@{
            type_src = [int]$p[0]
            type_buff = [int]$p[1]
            viewLegacy = $p[2].Trim()
            func = (McxFunc $p[3]).ToLower()
            table = (ViewToTable $p[2])
            pgView = (ViewToPgView $p[2])
        }
    }
}

$lines = @(
    "-- Procedures: tr_buff then tr_buff_micex_* (docs/proc.txt, no shared register)",
    "-- Regenerate: scripts/generate_tr_api_loader_procedures.ps1",
    "CREATE SCHEMA IF NOT EXISTS tr_api_loader;",
    ""
)

foreach ($b in ($buffs | Where-Object { $_.func -ne 'micex_deal_fx_data_ins' } | Sort-Object type_buff)) {
    $cols = $tableCols[$b.table]
    if (-not $cols) { Write-Warning "No table $($b.table)"; continue }
    $insertCols = @($cols | Where-Object { $_ -ne 'id' })
    $viewCols = $viewColsMap[$b.pgView]
    if (-not $viewCols) { Write-Warning "No view columns $($b.pgView)"; $viewCols = @() }
    $insertVals = @('v_buff_id') + @($insertCols | ForEach-Object { SqlValForDetailCol $_ $cols $viewCols $b.table })

    $lines += "-- type_buff=$($b.type_buff) $($b.table)"
    $lines += "CREATE OR REPLACE PROCEDURE tr_api_loader.$($b.func)(INOUT ior_buff tr__data_view.$($b.pgView))"
    $lines += "LANGUAGE plpgsql AS `$`$"
    $lines += "DECLARE"
    $lines += "    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;"
    $lines += "    v_buff_id NUMERIC;"
    $lines += "BEGIN"
    $lines += "    r_buff_cat.type_section := ior_buff.type_section;"
    $lines += "    r_buff_cat.type_buff := $($b.type_buff);"
    if ($viewCols -contains 'type_src') {
        $lines += "    r_buff_cat.type_src := ior_buff.type_src;"
    } else {
        $lines += "    r_buff_cat.type_src := $($b.type_src);"
    }
    # type_format есть не во всех view — только там, где cat.type_format в SELECT (docs/views.txt)
    if ($b.pgView -in @('v_tr_buff_micex_deal_fx', 'v_tr_buff_micex_fx_quote', 'v_tr_buff_micex_board', 'v_tr_buff_micex_sec_oda', 'v_tr_buff_micex_sec_quote', 'v_tr_buff_micex_deal_sec')) {
        $lines += "    r_buff_cat.type_format := ior_buff.type_format;"
    } else {
        $lines += "    r_buff_cat.type_format := NULL;"
    }
    $lines += "    r_buff_cat.insert_datetime := NOW();"
    $lines += "    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)"
    $lines += "    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)"
    $lines += "    RETURNING buff_id INTO v_buff_id;"
    $lines += "    ior_buff.buff_id := v_buff_id;"
    $lines += "    INSERT INTO tr__data_temp.$($b.table) (id, $($insertCols -join ', '))"
    $lines += "    VALUES ($($insertVals -join ', '));"
    $lines += "END;"
    $lines += "`$`$;"
    $lines += ""
}

# DEAL_FX: 3 rowtypes + OUT (each leg: own tr_buff + insert)
$fxTable = 'tr_buff_micex_deal_fx'
$fxCols = $tableCols[$fxTable]
$fxInsertCols = @($fxCols | Where-Object { $_ -ne 'id' })
$fxViewCols = $viewColsMap['v_tr_buff_micex_deal_fx']
if (-not $fxViewCols) { $fxViewCols = @() }
$fxInsertVals = @('v_buff_id') + @($fxInsertCols | ForEach-Object { SqlValForDetailCol $_ $fxCols $fxViewCols $fxTable 'r' })

$lines += "-- type_buff DEAL_FX (3x INOUT + swap OUT)"
$lines += "CREATE OR REPLACE PROCEDURE tr_api_loader.micex_deal_fx_data_ins("
$lines += "    INOUT ior_buff tr__data_view.v_tr_buff_micex_deal_fx,"
$lines += "    INOUT ior_buff_near tr__data_view.v_tr_buff_micex_deal_fx,"
$lines += "    INOUT ior_buff_far tr__data_view.v_tr_buff_micex_deal_fx,"
$lines += "    OUT o_is_swap NUMERIC,"
$lines += "    OUT o_err_message VARCHAR"
$lines += ")"
$lines += "LANGUAGE plpgsql AS `$`$"
$fxBlock = @(
    "        IF NOT (r.raw_tradeno IS NULL OR btrim(r.raw_tradeno) = '') THEN",
    "            r_buff_cat.type_section := r.type_section;",
    "            r_buff_cat.type_buff := 2323;",
    "            r_buff_cat.type_format := r.type_format;",
    "            r_buff_cat.type_src := r.type_src;",
    "            r_buff_cat.insert_datetime := NOW();",
    "            INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)",
    "            VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)",
    "            RETURNING buff_id INTO v_buff_id;",
    "            r.buff_id := v_buff_id;",
    "            INSERT INTO tr__data_temp.$fxTable (id, $($fxInsertCols -join ', '))",
    "            VALUES ($($fxInsertVals -join ', '));",
    "        END IF;"
)
$nearFarBlock = @(
    "        IF NOT (r.raw_tradeno IS NULL OR btrim(r.raw_tradeno) = '') THEN",
    "            r_buff_cat.type_section := r.type_section;",
    "            r_buff_cat.type_buff := 2323;",
    "            r_buff_cat.type_format := r.type_format;",
    "            r_buff_cat.type_src := r.type_src;",
    "            r_buff_cat.insert_datetime := NOW();",
    "            INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)",
    "            VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)",
    "            RETURNING buff_id INTO v_buff_id;",
    "            r.buff_id := v_buff_id;",
    "            INSERT INTO tr__data_temp.$fxTable (id, $($fxInsertCols -join ', '))",
    "            VALUES ($($fxInsertVals -join ', '));",
    "            v_near_ok := TRUE;",
    "        END IF;"
)
$farBlock = @(
    "        IF NOT (r.raw_tradeno IS NULL OR btrim(r.raw_tradeno) = '') THEN",
    "            r_buff_cat.type_section := r.type_section;",
    "            r_buff_cat.type_buff := 2323;",
    "            r_buff_cat.type_format := r.type_format;",
    "            r_buff_cat.type_src := r.type_src;",
    "            r_buff_cat.insert_datetime := NOW();",
    "            INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)",
    "            VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)",
    "            RETURNING buff_id INTO v_buff_id;",
    "            r.buff_id := v_buff_id;",
    "            INSERT INTO tr__data_temp.$fxTable (id, $($fxInsertCols -join ', '))",
    "            VALUES ($($fxInsertVals -join ', '));",
    "            v_far_ok := TRUE;",
    "        END IF;"
)
$lines += "DECLARE"
$lines += "    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;"
$lines += "    v_buff_id NUMERIC;"
$lines += "    v_near_ok BOOLEAN := FALSE;"
$lines += "    v_far_ok BOOLEAN := FALSE;"
$lines += "    r tr__data_view.v_tr_buff_micex_deal_fx;"
$lines += "BEGIN"
$lines += "    o_err_message := NULL;"
$lines += "    r := ior_buff;"
$fxBlock | ForEach-Object { $lines += $_ }
$lines += "    r := ior_buff_near;"
$nearFarBlock | ForEach-Object { $lines += $_ }
$lines += "    r := ior_buff_far;"
$farBlock | ForEach-Object { $lines += $_ }
$lines += "    o_is_swap := CASE WHEN v_near_ok AND v_far_ok THEN 81 ELSE 0 END;"
$lines += "EXCEPTION WHEN OTHERS THEN"
$lines += "    o_is_swap := 0;"
$lines += "    o_err_message := SQLERRM;"
$lines += "END;"
$lines += "`$`$;"
$lines += ""

$lines | Set-Content $outPath -Encoding UTF8
Write-Host "procedures -> $outPath ($($buffs.Count)+1 deal_fx)"
