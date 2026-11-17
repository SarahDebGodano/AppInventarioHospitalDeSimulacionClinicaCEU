@echo off
REM =====================================================================
REM Copia de seguridad diaria de la base de datos del inventario.
REM Se ejecuta en el miniPC con una tarea programada de Windows.
REM La contrasena se lee de pgpass.conf (se configura en la fase 1).
REM =====================================================================

set PG_BIN=C:\Program Files\PostgreSQL\16\bin
set DESTINO=C:\InventarioHospital\copias
set USUARIO=inventario_app
set BASE=inventory

if not exist "%DESTINO%" mkdir "%DESTINO%"

REM Fecha 2026-11-17 (igual en cualquier idioma de Windows)
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd"') do set FECHA=%%i

"%PG_BIN%\pg_dump.exe" -h localhost -U %USUARIO% -d %BASE% -F c -f "%DESTINO%\inventario_%FECHA%.dump"

if errorlevel 1 (
    echo %date% %time% ERROR en la copia >> "%DESTINO%\errores.log"
    exit /b 1
)

REM Borrar las copias de mas de 30 dias
forfiles /p "%DESTINO%" /m *.dump /d -30 /c "cmd /c del @path" 2>nul
