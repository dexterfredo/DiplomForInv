@echo off
rem Sets JAVA_HOME for parent cmd. Exit 0 = OK, 1 = JDK not found.
setlocal EnableExtensions EnableDelayedExpansion

if defined JAVA_HOME (
  if not exist "!JAVA_HOME!\bin\java.exe" set "JAVA_HOME="
  if not exist "!JAVA_HOME!\bin\javac.exe" set "JAVA_HOME="
)

if not defined JAVA_HOME (
  for /f "delims=" %%j in ('where java 2^>nul') do (
    for %%i in ("%%~dpj..") do set "JAVA_HOME=%%~fi"
    goto found
  )
)

if not defined JAVA_HOME if exist "%ProgramFiles%\Java\" (
  for /f "delims=" %%d in ('dir /b /ad /o-n "%ProgramFiles%\Java\jdk*" 2^>nul') do (
    set "JAVA_HOME=%ProgramFiles%\Java\%%d"
    if exist "!JAVA_HOME!\bin\javac.exe" goto found
    set "JAVA_HOME="
  )
)

if not defined JAVA_HOME if exist "%ProgramFiles%\Eclipse Adoptium\" (
  for /f "delims=" %%d in ('dir /b /ad /o-n "%ProgramFiles%\Eclipse Adoptium\jdk-*" 2^>nul') do (
    set "JAVA_HOME=%ProgramFiles%\Eclipse Adoptium\%%d"
    if exist "!JAVA_HOME!\bin\javac.exe" goto found
    set "JAVA_HOME="
  )
)

if not defined JAVA_HOME if exist "%ProgramFiles%\Microsoft\" (
  for /f "delims=" %%d in ('dir /b /ad /o-n "%ProgramFiles%\Microsoft\jdk-*" 2^>nul') do (
    set "JAVA_HOME=%ProgramFiles%\Microsoft\%%d"
    if exist "!JAVA_HOME!\bin\javac.exe" goto found
    set "JAVA_HOME="
  )
)

if not defined JAVA_HOME if exist "%ProgramFiles%\BellSoft\" (
  for /f "delims=" %%d in ('dir /b /ad /o-n "%ProgramFiles%\BellSoft\LibericaJDK-*" 2^>nul') do (
    set "JAVA_HOME=%ProgramFiles%\BellSoft\%%d"
    if exist "!JAVA_HOME!\bin\javac.exe" goto found
    set "JAVA_HOME="
  )
)

if not defined JAVA_HOME goto fail

:found
endlocal & set "JAVA_HOME=%JAVA_HOME%" & exit /b 0

:fail
endlocal & exit /b 1
