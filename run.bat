@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo  Starting the full hermetrics stack:
echo    Kafka + Flink job (JobManager + TaskManager) + API + UI
echo ============================================================
echo.
echo Building images and starting (first run takes a few minutes)...

docker compose -f deploy\compose\docker-compose.yml up --build -d
if errorlevel 1 (
  echo.
  echo Failed to start. Is Docker Desktop running?
  exit /b 1
)

echo.
echo Stack is starting. Allow ~30-60s for Flink to come up and the seed to flow.
echo.
echo   UI         : http://localhost:5173
echo   API docs   : http://localhost:8080/docs
echo   Flink UI   : http://localhost:8081
echo.
echo The demo seeds two documents: g1 matches (EQUAL), g2 differs (DIFF).
echo Stop everything with stop.bat
echo.

timeout /t 6 /nobreak >nul
start "" http://localhost:5173
endlocal
