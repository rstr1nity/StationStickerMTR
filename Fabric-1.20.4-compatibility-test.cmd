@echo off
cd /d "%~dp0fabric"
call gradlew.bat build --console=plain
if errorlevel 1 goto end
call gradlew.bat -p compat-1.20.4 runClient --console=plain
:end
pause
