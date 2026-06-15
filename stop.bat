@echo off
setlocal
cd /d "%~dp0"
echo Stopping the hermetrics stack...
docker compose -f deploy\compose\docker-compose.yml down
endlocal
