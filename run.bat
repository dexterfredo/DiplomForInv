@echo off
setlocal EnableExtensions
pushd "%~dp0"

if /i "%~1"=="__run" goto do_run
cmd /k call "%CD%\run.bat" __run %*
popd
exit /b

:do_run
call "%CD%\scripts\find-jar.cmd" "%CD%"
if errorlevel 1 goto err_no_jar
set "JAR=%LOADER_JAR%"

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not exist "%JAR%" goto err_no_jar
if not exist "%CD%\native\win64\mtejni.dll" goto err_no_dll
if not exist "%CD%\lib\mte-client-1.3.6.jar" goto err_no_lib

set "PATH=%CD%\native\win64;%PATH%"
call "%CD%\scripts\set-console-utf8.cmd"

echo Starting: %JAR%
echo Folder: %CD%
echo Browser: http://localhost:8080/
echo Logs: %CD%\logs\loader.log
echo Errors: %CD%\logs\errors.log
echo Stop: Ctrl+C or stop.bat
echo.

"%JAVA_EXE%" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dapp.home="%CD%" -jar "%JAR%" %*
if errorlevel 1 goto err_java

pause
popd
exit /b 0

:err_no_jar
echo [run.bat] JAR not found in target\
echo Expected: target\LoaderMicexFX.jar after build.bat
echo Run build.bat and check for BUILD SUCCESS.
pause
popd
exit /b 1

:err_no_dll
echo [run.bat] Missing native\win64\mtejni.dll
pause
popd
exit /b 1

:err_no_lib
echo [run.bat] Missing lib\mte-client-1.3.6.jar
pause
popd
exit /b 1

:err_java
echo [run.bat] Start failed. Check: java -version
pause
popd
exit /b 1
