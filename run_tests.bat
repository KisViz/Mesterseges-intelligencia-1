@echo off
setlocal enabledelayedexpansion

@REM cd .\src\

REM Compile the Java player code
javac -cp game_engine.jar --release 8 Agent.java

REM If compilation failed, stop the script
if errorlevel 1 (
    echo Compilation failed. Exiting...
    exit /b 1
)

if "%~1"=="" (
    echo Please provide an iteration number as an argument.
    exit /b
)

set "limit=%~1"


for /l %%x in (1, 1, %limit%) do (

    set /a seed=!RANDOM!

    echo %%x.
    echo Seed: !seed!

    for /f "delims=" %%A in ('java -jar game_engine.jar 99999 game.mario.MarioGame !seed! 1000 Agent') do (
        set "output=%%A"
    )

    REM Remove the prefix to isolate the number
    for /f "tokens=3 delims= " %%A in ("!output!") do (
        set "num=%%A"
    )
    REM cast to int
    for /f "tokens=1 delims=." %%A in ("!num!") do (
        set "num=%%A"
    )

    echo Score: !num!
    echo --------------

    REM Check if num is less than 2000 or equals "RUN"
    if "!num!"=="RUN" (
        echo Test failed run out of time!
        goto :breakLoop
    ) else (
        set /a testNum=!num!
        if !testNum! LSS 2000 (
            echo Test failed to reach 2000 score.
            goto :breakLoop
        )
    )
)

echo All %limit% passed.

:breakLoop
