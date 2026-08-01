@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
  echo Gradle is not installed or not on PATH. Install Gradle or add it to PATH, then rerun this command.
  exit /b 1
)
gradle %*
