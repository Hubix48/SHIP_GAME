@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "DIST=%~dp0Statki"
set "FX_SRC=%~dp0javafx-sdk-25.0.1"
set "FX=%FX_SRC%\lib"
set "SRC=%~dp0src"
set "BUILD=%DIST%\build"
set "OUT=%BUILD%\classes"
set "SF=%TEMP%\statki_src.txt"

where javac >nul 2>nul && (set "JC=javac" & set "JR=jar" & goto :ok)
for %%d in (openjdk-25.0.1 corretto-23.0.2) do (
    if exist "%USERPROFILE%\.jdks\%%d\bin\javac.exe" (
        set "JC=%USERPROFILE%\.jdks\%%d\bin\javac.exe"
        set "JR=%USERPROFILE%\.jdks\%%d\bin\jar.exe"
        goto :ok
    )
)
echo [ERROR] javac not found! & pause & exit /b 1

:ok
echo Using: %JC%
echo [1/8] Cleaning...
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%"
mkdir "%OUT%"

echo [2/8] Compiling...
(for /f "delims=" %%f in ('dir /s /B "%SRC%\*.java"') do (set "P=%%f" & echo "!P:\=/!")) > "%SF%"
"%JC%" --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -d "%OUT%" "@%SF%"
if %errorlevel% neq 0 (echo FAILED! & del "%SF%" & pause & exit /b 1)
del "%SF%"

echo [3/8] Copying resources...
xcopy /s /i /y "%SRC%\images" "%OUT%\images" >nul
mkdir "%OUT%\pl\zubrzycki\statki\ui" 2>nul
copy /y "%SRC%\pl\zubrzycki\statki\ui\style.css" "%OUT%\pl\zubrzycki\statki\ui\" >nul

echo [4/8] Packaging JAR...
echo Main-Class: pl.Launcher > "%TEMP%\manifest.txt"
"%JR%" cfm "%BUILD%\Statki.jar" "%TEMP%\manifest.txt" -C "%OUT%" .
del "%TEMP%\manifest.txt"

echo [5/8] Creating Server.jar...
copy /y "%BUILD%\Statki.jar" "%BUILD%\Server.jar" >nul

echo [6/8] Creating Client.jar...
copy /y "%BUILD%\Statki.jar" "%BUILD%\Client.jar" >nul

echo [7/8] Copying JavaFX...
xcopy /s /i /y "%FX_SRC%" "%DIST%\javafx-sdk-25.0.1" >nul

echo [8/8] Generating run scripts...
call :gen_run "%DIST%\run.bat"         "Statki"                   "build\Statki.jar"  ""
call :gen_run "%DIST%\run_server.bat"  "Statki - SERVER (HOST)"   "build\Server.jar"  " --mode=host"
call :gen_run "%DIST%\run_client.bat"  "Statki - CLIENT (GUEST)"  "build\Client.jar"  " --mode=client"

echo.
echo BUILD SUCCESS!
pause
exit /b 0



:gen_run
set "F=%~1"
(
    echo @echo off
    echo chcp 65001 ^>nul
    echo set "FX=%%~dp0javafx-sdk-25.0.1\lib"
) > "%F%"

echo title %~2>> "%F%"

(
    echo.
    echo where java ^>nul 2^>nul ^&^& ^(set "J=java" ^& goto :ok^)
    echo for %%%%d in ^(openjdk-25.0.1 corretto-23.0.2^) do ^(
    echo     if exist "%%USERPROFILE%%\.jdks\%%%%d\bin\java.exe" ^(set "J=%%USERPROFILE%%\.jdks\%%%%d\bin\java.exe" ^& goto :ok^)
    echo ^)
    echo echo [ERROR] Java not found^^! ^& pause ^& exit /b 1
    echo.
    echo :ok
    echo "%%J%%" --module-path "%%FX%%" --add-modules javafx.controls,javafx.fxml -jar "%%~dp0%~3"%~4
    echo pause
) >> "%F%"
exit /b 0
