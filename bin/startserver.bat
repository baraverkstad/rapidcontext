@echo off

::Locate the application directory
for %%x in (%0) do set BATCHPATH=%%~dpsx
SET RAPIDCONTEXT_HOME=%BATCHPATH:~0,-5%
cd %RAPIDCONTEXT_HOME%

::Find Java and JAR file
CALL bin\common.bat
if errorlevel 1 goto DONE

::Start application
echo Base Dir:   %CD%
echo JAVA_HOME:  %JAVA_HOME%
echo JAVA_OPTS:  %JAVA_OPTS%
echo.
start "RapidContext Server -- Close window to shutdown" /MIN "%JAVA_HOME%\bin\java.exe" %JAVA_OPTS% -jar %JARFILE% --server %1 %2 %3 %4 %5 %6 %7 %8 %9

::Restore environment vars
set JAVA_HOME=%_JAVA_HOME%
set JAVA_OPTS=%_JAVA_OPTS%

::Open web browser
echo Waiting for RapidContext Server to start (in separate window)...
:WAIT
ping 127.0.0.1 -n 2 -w 1000 > nul
if not exist var\server.port goto WAIT
echo Launching web browser...
for /f %%x in (var\server.port) do set PORT=%%x
if "%PORT%" == "80" goto :NOPORT
start http://localhost:%PORT%/
goto DONE
:NOPORT
start http://localhost/
goto DONE

:DONE
echo Done
