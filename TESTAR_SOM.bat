@echo off
setlocal
cd /d "%~dp0"
if exist "VanishCam.exe" (
  "VanishCam.exe" --teste-som %*
) else (
  call INICIAR.bat --teste-som %*
)
echo.
pause
