@echo off
rem Sets MVN_CMD to mvn executable. Exit 0 = OK, 1 = not found.
setlocal EnableExtensions

set "MVN_CMD="

if exist "%CD%\maven\apache-maven-3.9.14\bin\mvn.cmd" (
  set "MVN_CMD=%CD%\maven\apache-maven-3.9.14\bin\mvn.cmd"
  goto ok
)

where mvn >nul 2>&1
if not errorlevel 1 (
  for /f "delims=" %%m in ('where mvn 2^>nul') do (
    set "MVN_CMD=%%m"
    goto ok
  )
)

if exist "%CD%\mvnw.cmd" (
  set "MVN_CMD=%CD%\mvnw.cmd"
  goto ok
)

endlocal & exit /b 1

:ok
endlocal & set "MVN_CMD=%MVN_CMD%" & exit /b 0
