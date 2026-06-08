@echo off
setlocal EnableExtensions EnableDelayedExpansion
pushd "%~dp0"

call "%CD%\scripts\find-java.cmd"
if errorlevel 1 goto err_java
echo Using JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version 2>&1
if errorlevel 1 goto err_java

call "%CD%\scripts\find-maven.cmd"
if errorlevel 1 goto err_mvn
echo Using Maven: %MVN_CMD%

if not exist "%CD%\lib\mte-client-1.3.6.jar" goto err_lib

set "MVN_OPTS=-DskipTests package"
set "OFFLINE_FLAG="

if exist "%CD%\maven-repository\org\springframework\boot\spring-boot-starter-parent\3.3.4\spring-boot-starter-parent-3.3.4.pom" (
  set "MVN_OPTS=-s settings-local.xml -o %MVN_OPTS%"
  set "OFFLINE_FLAG=1"
  echo Offline build: maven-repository
) else (
  echo Online build: need access to repo.maven.apache.org
)

echo Building in: %CD%
call "%MVN_CMD%" %MVN_OPTS%
if errorlevel 1 goto err_build

call "%CD%\scripts\find-jar.cmd" "%CD%"
if errorlevel 1 (
  echo.
  echo [build.bat] BUILD SUCCESS but JAR not found in target\
) else (
  echo.
  echo Done. JAR: %LOADER_JAR%
  echo Run: run.bat
)
pause
popd
exit /b 0

:err_java
echo.
echo [build.bat] JDK 17+ not found or JAVA_HOME is wrong.
echo Install JDK 17 or 21, set JAVA_HOME to JDK root ^(without \bin^).
echo Example: setx JAVA_HOME "C:\Program Files\BellSoft\LibericaJDK-21"
pause
popd
exit /b 1

:err_mvn
echo [build.bat] Maven not found.
echo Install Maven or run on PC with internet:
echo   powershell -ExecutionPolicy Bypass -File scripts\prepare-offline-bundle.ps1
echo Then copy maven\ and maven-repository\ to this PC.
pause
popd
exit /b 1

:err_lib
echo [build.bat] Put MICEX JAR files into lib\
pause
popd
exit /b 1

:err_build
echo.
echo [build.bat] Build failed.
if not defined OFFLINE_FLAG (
  echo No internet to Maven Central?
  echo   1. On PC with internet: scripts\prepare-offline-bundle.ps1
  echo   2. Copy maven\ + maven-repository\ here, run build.bat again
  echo   3. Or copy ready target\LoaderMicexFX.jar and use run.bat
) else (
  echo Offline cache incomplete. Re-run prepare-offline-bundle.ps1 on PC with internet.
)
pause
popd
exit /b 1
