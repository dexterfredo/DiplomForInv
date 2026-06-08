@echo off
rem UTF-8 for cmd + JVM stdout (Windows)
chcp 65001 >nul 2>&1
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
