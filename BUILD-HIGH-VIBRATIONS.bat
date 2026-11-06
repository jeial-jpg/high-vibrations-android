@echo off
rem ===========================================================================
rem  BUILD-HIGH-VIBRATIONS.bat - ЕДИНСТВЕННАЯ точка сборки приложения
rem  "На высоких вибрациях" (ru.highvibrations.browser).
rem
rem  Использует только локальные компоненты: JDK 17, Android SDK 34,
rem  Gradle 8.11.1 и AGP 8.6.1 из кэша. Ничего не скачивает.
rem
rem  Режимы:
rem    build    - собрать debug APK                          (по умолчанию)
rem    install  - собрать, установить на устройство и запустить
rem    clean    - очистить артефакты сборки
rem    check    - показать окружение, ничего не собирать
rem ===========================================================================
setlocal EnableExtensions EnableDelayedExpansion
title High Vibrations build

set "PROJ=%~dp0"
if "%PROJ:~-1%"=="\" set "PROJ=%PROJ:~0,-1%"
set "APK=%PROJ%\app\build\outputs\apk\debug\app-debug.apk"
set "PKG=ru.highvibrations.browser"
set "ACTIVITY=ru.highvibrations.browser/.MainActivity"

set "MODE=%~1"
if "%MODE%"=="" set "MODE=build"
if /i "%MODE%"=="release" set "APK=%PROJ%\app\build\outputs\apk\release\app-release.apk"

echo ============================================================
echo  На высоких вибрациях  (%PKG%)
echo  project : %PROJ%
echo  mode    : %MODE%
echo ============================================================

rem ---- JDK -----------------------------------------------------------------
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK 17 not found: "%JAVA_HOME%"
    goto :fail
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ---- Android SDK ---------------------------------------------------------
if not defined ANDROID_HOME set "ANDROID_HOME=C:\apK\Root\ANDROID\VS CODE\VSCODE\Sdk"
if not defined ANDROID_SDK_ROOT set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
if not exist "%ANDROID_HOME%\platforms\android-34" (
    echo [ERROR] Android SDK platform 34 not found in "%ANDROID_HOME%"
    goto :fail
)
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

rem ---- Gradle из локального кэша (без загрузки) ----------------------------
set "GRADLE_CACHE=%GRADLE_USER_HOME%"
if not defined GRADLE_CACHE set "GRADLE_CACHE=C:\apK\Root\ANDROID\gradle-home"
set "GRADLE_EXE="
for %%v in (8.11.1 8.7) do (
    if not defined GRADLE_EXE (
        for /f "delims=" %%p in ('dir /b /s "%GRADLE_CACHE%\wrapper\dists\gradle-%%v-bin\gradle.bat" 2^>nul') do if not defined GRADLE_EXE set "GRADLE_EXE=%%p"
    )
)
if not defined GRADLE_EXE (
    echo [ERROR] no local Gradle 8.x found under "%GRADLE_CACHE%\wrapper\dists"
    goto :fail
)

echo.
echo  JDK          : %JAVA_HOME%
echo  ANDROID_HOME : %ANDROID_HOME%
echo  Gradle       : %GRADLE_EXE%
echo.

if /i "%MODE%"=="check" (
    echo [check] nothing was built.
    goto :end_ok
)

rem ---- сборка --------------------------------------------------------------
pushd "%PROJ%"
if /i "%MODE%"=="clean" (
    call "%GRADLE_EXE%" --console=plain clean
) else (
    if /i "%MODE%"=="release" (
        call "%GRADLE_EXE%" --console=plain :app:assembleRelease
    ) else (
        call "%GRADLE_EXE%" --console=plain :app:assembleDebug
    )
)
set "RC=%ERRORLEVEL%"
popd

if not "%RC%"=="0" (
    echo.
    echo [ERROR] build failed, exit code %RC%
    goto :fail
)

echo.
if not exist "%APK%" (
    echo [ERROR] APK not found: %APK%
    goto :fail
)
for %%f in ("%APK%") do echo [apk] %%~ff  ^(%%~zf bytes^)

if /i not "%MODE%"=="install" goto :end_ok

rem ---- устройство: сначала BlueStacks, иначе подключённый телефон ---------
echo.
"%ADB%" connect 127.0.0.1:5555 >nul 2>&1
set "TARGET="
for /f "tokens=1,2" %%a in ('"%ADB%" devices 2^>nul') do (
    if "%%b"=="device" if "%%a"=="127.0.0.1:5555" set "TARGET=%%a"
)
if not defined TARGET (
    for /f "tokens=1,2" %%a in ('"%ADB%" devices 2^>nul') do (
        if "%%b"=="device" if not defined TARGET set "TARGET=%%a"
    )
)
if not defined TARGET (
    echo [warn] no device / emulator found - install skipped
    goto :end_ok
)

echo [device] %TARGET%
"%ADB%" -s %TARGET% install -r "%APK%"
if errorlevel 1 (
    echo [ERROR] install failed
    goto :fail
)

echo [start] %ACTIVITY%
"%ADB%" -s %TARGET% shell am force-stop %PKG% >nul 2>&1
"%ADB%" -s %TARGET% shell am start -n %ACTIVITY% >nul 2>&1
echo [done] приложение установлено и запущено
goto :end_ok

:fail
echo.
echo [FAILED]
endlocal
exit /b 1

:end_ok
echo.
endlocal
exit /b 0
