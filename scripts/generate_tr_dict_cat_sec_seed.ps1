# Генерация INSERT для tr__data.tr_dict_cat_sec из docs/TR_DICT_CAT_SEC.txt
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Docs = Join-Path $Root "docs"
$Src = Join-Path $Docs "TR_DICT_CAT_SEC.txt"
$Out = Join-Path $Root "src\main\resources\sql\generated\22_seed_tr_dict_cat_sec.sql"

$Columns = @(
    "id", "sec", "sec_name", "sec_type", "sec_sub_type_id", "code_ref", "code_registry", "code_isin",
    "issuer_id", "face_value_amount", "face_value_ccy_id", "issue_date", "maturity_date", "sec_mode",
    "operation_mode", "tag", "pay_date", "depositary_id", "aci_tag", "placing_date", "placing_out_date",
    "placing_price", "placing_amount", "issue_amount", "book_code", "pnote_type", "pnote_number", "pnote_set",
    "pnote_income_type", "pnote_rate", "pnote_maturity_type", "pnote_maturity_term", "pnote_city_pay",
    "pnote_drawee_id", "on_serv_date", "out_serv_date", "cust_type", "invest_value", "invest_value_ccy_id",
    "issue_type", "pnote_mask", "pnote_card_code", "pnote_card_number", "pnote_first_endorser", "pnote_endorser",
    "pnote_aval_mode", "pnote_accept_mode", "pnote_get_out_date", "pnote_place_compile", "pnote_blank_num",
    "pnote_correct_mode", "depositary_mode", "last_maturity_date", "acc_code", "depo_code", "if_not_discount",
    "if_early_repay", "aci_value_date", "sec_short_name", "tf_sec_depo_rec_type", "tf_def_aci_rate",
    "tf_aci_period_type", "tf_aci_period", "tf_code_reuters", "tf_code_di", "tf_if_aci_without_round",
    "tf_sec_group_id", "audit_id", "id_ref", "type_status", "if_use", "issue_number", "if_key_rate",
    "if_float_rate", "swift_code", "if_error_aci_amort", "aci_calc_mode", "tf_code_micex", "tf_lot_micex",
    "tf_def_aci_amount", "type_sec_guarantee", "aci_accuracy", "face_value_memo", "pnote_code4depo",
    "main_id", "hist_id", "sys_entry_date", "sys_expiry_date", "oper_entry_date", "oper_expiry_date",
    "base_asset_id", "base_issuer_id", "tf_pfl_id", "agreement_id", "if_own", "code_forts",
    "if_investment_deduction", "parent_id", "if_face_value_inflation_index", "country_id",
    "parent_issuer_company_id", "cfi", "sec_issue_form", "dt_sec_short_name_del", "if_struct_note",
    "if_auto_update", "min_updated_aci_date", "percent_amount_from", "batch_id", "batch_det_id", "bill_id",
    "tf_frequency", "db_source", "rate_type"
)

$NumericCols = [System.Collections.Generic.HashSet[string]]::new(
    [string[]]@(
        "id", "sec_type", "sec_sub_type_id", "issuer_id", "face_value_amount", "face_value_ccy_id",
        "sec_mode", "operation_mode", "depositary_id", "aci_tag", "placing_price", "placing_amount",
        "issue_amount", "pnote_type", "pnote_income_type", "pnote_rate", "pnote_maturity_type",
        "pnote_maturity_term", "pnote_drawee_id", "cust_type", "invest_value", "invest_value_ccy_id",
        "issue_type", "pnote_card_number", "pnote_aval_mode", "pnote_accept_mode", "pnote_correct_mode",
        "depositary_mode", "if_not_discount", "if_early_repay", "tf_sec_depo_rec_type", "tf_def_aci_rate",
        "tf_aci_period_type", "tf_aci_period", "tf_if_aci_without_round", "tf_sec_group_id", "audit_id",
        "type_status", "if_use", "if_key_rate", "if_float_rate", "if_error_aci_amort", "aci_calc_mode",
        "tf_lot_micex", "tf_def_aci_amount", "type_sec_guarantee", "aci_accuracy", "main_id", "hist_id",
        "base_asset_id", "base_issuer_id", "tf_pfl_id", "agreement_id", "if_own", "if_investment_deduction",
        "parent_id", "if_face_value_inflation_index", "country_id", "parent_issuer_company_id",
        "sec_issue_form", "if_struct_note", "if_auto_update", "batch_id", "batch_det_id", "bill_id",
        "tf_frequency", "rate_type"
    )
)

$TimestampCols = [System.Collections.Generic.HashSet[string]]::new(
    [string[]]@(
        "issue_date", "maturity_date", "pay_date", "placing_date", "placing_out_date", "on_serv_date",
        "out_serv_date", "pnote_get_out_date", "last_maturity_date", "aci_value_date", "sys_entry_date",
        "sys_expiry_date", "oper_entry_date", "oper_expiry_date", "min_updated_aci_date", "percent_amount_from"
    )
)

function Format-SqlValue {
    param([string]$Col, [string]$Raw)
    if ($null -eq $Raw) { $Raw = "" }
    $v = $Raw.Trim()
    if ($v -eq "") { return "NULL" }
    if ($NumericCols.Contains($Col)) { return $v }
    $escaped = $v -replace "'", "''"
    return "'$escaped'"
}

function Clean-MergedField {
    param([string]$Value)
    $v = $Value.Trim()
    if ($v.StartsWith('"')) { $v = $v.Substring(1) }
    if ($v.EndsWith('"')) { $v = $v.Substring(0, $v.Length - 1) }
    return $v.Trim()
}

# Oracle export: лишний TAB перед ISIN / tf_code_micex ("\tRU000...)
function Normalize-RowParts {
    param([string[]]$Parts, [int]$Expected)
    $list = [System.Collections.Generic.List[string]]::new()
    foreach ($p in $Parts) { $list.Add($p) }

    $guard = 0
    while ($list.Count -gt $Expected -and $guard -lt 50) {
        $guard++
        $merged = $false
        for ($i = 0; $i -lt $list.Count - 1; $i++) {
            $cur = $list[$i].Trim()
            if ($cur -eq '"' -or $cur -eq '""') {
                $list[$i] = Clean-MergedField ($list[$i] + $list[$i + 1])
                $list.RemoveAt($i + 1)
                $merged = $true
                break
            }
            if ($cur.StartsWith('"') -and -not $cur.EndsWith('"') -and $list[$i + 1].Length -gt 0) {
                $list[$i] = Clean-MergedField ($list[$i] + $list[$i + 1])
                $list.RemoveAt($i + 1)
                $merged = $true
                break
            }
        }
        if (-not $merged) {
            # хвост: ... ORA \t \t
            if ($list.Count -ge 2 -and $list[$list.Count - 1].Trim() -eq "" -and $list[$list.Count - 2].Trim() -eq "ORA") {
                $list.RemoveAt($list.Count - 1)
                $merged = $true
            }
        }
        if (-not $merged) { break }
    }

    while ($list.Count -lt $Expected) { $list.Add("") }
    if ($list.Count -gt $Expected) {
        $list = [System.Collections.Generic.List[string]]($list.GetRange(0, $Expected))
    }
    return ,$list.ToArray()
}

$lines = Get-Content -Path $Src -Encoding UTF8 | Where-Object { $_.Trim() -ne "" }
$inserts = New-Object System.Collections.Generic.List[string]
$warn = 0

foreach ($line in $lines) {
    $raw = $line -split "`t", -1
    $parts = Normalize-RowParts -Parts $raw -Expected $Columns.Count
    if ($raw.Count -ne $Columns.Count) { $warn++ }
    $vals = @()
    for ($i = 0; $i -lt $Columns.Count; $i++) {
        $vals += Format-SqlValue -Col $Columns[$i] -Raw $parts[$i]
    }
    $colList = ($Columns -join ", ")
    $inserts.Add("INSERT INTO tr__data.tr_dict_cat_sec ($colList) VALUES ($($vals -join ', '));")
}

$header = @"
-- Seed tr__data.tr_dict_cat_sec
-- Source: docs/TR_DICT_CAT_SEC.txt
-- Schema: docs/table3.txt; view: docs/view3.txt (v_tr_dict_sec)

BEGIN;

DELETE FROM tr__data.tr_dict_cat_sec;

"@

$footer = @"

COMMIT;
"@

$dir = Split-Path $Out -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$content = $header + ($inserts -join "`n") + $footer
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($Out, $content, $utf8NoBom)
Write-Host "rows: $($inserts.Count)"
if ($warn -gt 0) { Write-Host "normalized (extra tabs/quotes): $warn rows" }
Write-Host "written: $Out"
