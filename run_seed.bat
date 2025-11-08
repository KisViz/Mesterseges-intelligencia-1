@echo off
setlocal

cd .\src\

REM Compile the Java player code
javac -cp ".;game_engine.jar" --release 8 Agent.java

if errorlevel 1 (
    echo Compilation failed. Exiting...
    exit /b 1
)

if "%~1"=="" (
    echo Please provide a seed number as an argument.
    exit /b
)

set "seed=%~1"

REM Run the game engine with Agent
java -cp ".;game_engine.jar" game.mario.MarioGame 60 %seed% 1000 Agent

echo Seed: %seed%
