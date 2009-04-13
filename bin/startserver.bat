@echo off

::Check for Windows NT compliance
> reg1.txt echo 1234&rem
type reg1.txt | find "rem"
if not errorlevel 1 goto ERRWIN9X
del reg1.txt 2> nul

::Locate the application directory
for %%x in (%0) do set BATCHPATH=%%~dpsx
SET RAPIDCONTEXT_HOME=%BATCHPATH:~0,-5%
cd %RAPIDCONTEXT_HOME%

::Find Java and setup classpath
CALL bin\common.bat
if errorlevel 1 goto DONE

::Start application
echo Base Dir:   %CD%
echo JAVA_HOME:  %JAVA_HOME%
echo JAVA_OPTS:  %JAVA_OPTS%
echo CLASSPATH:  %CLASSPATH%
echo.
start "RapidContext Server -- Close window to shutdown" /MIN "%JAVA_HOME%\bin\java.exe" %JAVA_OPTS% org.rapidcontext.app.ServerApplication %1 %2 %3 %4 %5 %6 %7 %8 %9

::Restore environment vars
set JAVA_HOME=%_JAVA_HOME%
set JAVA_OPTS=%_JAVA_OPTS%
set CLASSPATH=%_CLASSPATH%

::Open web browser
echo Waiting for RapidContext Server to start (in separate window)...
:WAIT
ping 127.0.0.1 -n 2 -w 1000 > nul
if not exist var\server.port goto WAIT
echo Launching web browser...
for /f %%x in (var\server.port) do set PORT=%%x
if "%PORT%" == "80" goto :NOPORT
start http://localhost:%PORT%/
goto CLEANUP
:NOPORT
start http://localhost/
goto CLEANUP

:ERRWIN9X
del reg1.txt 2> nul
echo ERROR: Windows 98 or previous is not supported.

:CLEANUP
set CLASSPATH=%_CLASSPATH%
echo Done

:DONE
