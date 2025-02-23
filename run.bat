@echo off
cd /d "%~dp0"

rem Start transit-trackers-simulator
cd transit-trackers-simulator
start cmd /k gradlew bootRun

rem Start transit-operations-center
cd ..\transit-operations-center
start cmd /k gradlew bootRun

rem Start transit-sim-dashboard
cd ..\transit-sim-dashboard
start cmd /k npm run dev
