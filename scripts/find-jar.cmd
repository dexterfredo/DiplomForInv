@echo off
rem Sets LOADER_JAR to the Spring Boot fat JAR in target\. Exit 0 = found, 1 = not found.
setlocal EnableExtensions
set "ROOT=%~1"
if "%ROOT%"=="" set "ROOT=%CD%"

if exist "%ROOT%\target\LoaderMicexFX.jar" (
  endlocal & set "LOADER_JAR=%ROOT%\target\LoaderMicexFX.jar"
  exit /b 0
)
if exist "%ROOT%\target\LoaderMicexFX-new-1.0.0-SNAPSHOT.jar" (
  endlocal & set "LOADER_JAR=%ROOT%\target\LoaderMicexFX-new-1.0.0-SNAPSHOT.jar"
  exit /b 0
)

endlocal
exit /b 1
