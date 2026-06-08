@echo off
setlocal EnableExtensions
pushd "%~dp0"

echo Stopping LoaderMicexFX...

if exist "%CD%\logs\loader.pid" for /f "usebackq delims=" %%i in ("%CD%\logs\loader.pid") do taskkill /PID %%i /F >nul 2>&1
if exist "%CD%\logs\loader.pid" del "%CD%\logs\loader.pid" 2>nul

powershell -NoProfile -File "%CD%\scripts\stop-loader.ps1"
if errorlevel 1 goto not_found

echo Done.
pause
popd
exit /b 0

:not_found
echo [stop.bat] Process not found.
pause
popd
exit /b 1
