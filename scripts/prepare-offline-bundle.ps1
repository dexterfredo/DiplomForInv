# Скачивает Maven + зависимости в папку проекта (для сборки без интернета).
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$Repo = Join-Path $Root "maven-repository"
$MavenZip = Join-Path $Root "maven\apache-maven-3.9.14-bin.zip"
$MavenHome = Join-Path $Root "maven\apache-maven-3.9.14"
$Mvn = Join-Path $MavenHome "bin\mvn.cmd"
$Settings = Join-Path $Root "settings-local.xml"

New-Item -ItemType Directory -Force -Path (Join-Path $Root "maven") | Out-Null
New-Item -ItemType Directory -Force -Path $Repo | Out-Null

if (-not (Test-Path $Mvn)) {
    $url = "https://dlcdn.apache.org/maven/maven-3/3.9.14/binaries/apache-maven-3.9.14-bin.zip"
    Write-Host "Download Maven 3.9.14..."
    Invoke-WebRequest -Uri $url -OutFile $MavenZip -UseBasicParsing
    Expand-Archive -Path $MavenZip -DestinationPath (Join-Path $Root "maven") -Force
}

if (-not (Test-Path $Mvn)) {
    throw "Maven not found after extract: $Mvn"
}

Write-Host "Download dependencies to maven-repository..."
Push-Location $Root
& $Mvn -s $Settings "-Dmaven.repo.local=$Repo" -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host ""
Write-Host "Done. Copy to offline PC:"
Write-Host "  maven\"
Write-Host "  maven-repository\"
Write-Host "Then run build.bat (offline)."
