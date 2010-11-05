@echo off

::Locate the application directory
for %%x in (%0) do set BATCHPATH=%%~dpsx
SET RAPIDCONTEXT_HOME=%BATCHPATH:~0,-5%
cd %RAPIDCONTEXT_HOME%

::Find Java and JAR file
CALL bin\common.bat
if errorlevel 1 goto DONE

::Run application
"%JAVA_HOME%\bin\java.exe" %JAVA_OPTS% -cp %JARFILE% org.rapidcontext.app.CmdLineApplication %1 %2 %3 %4 %5 %6 %7 %8 %9

::Restore environment vars
set JAVA_HOME=%_JAVA_HOME%
set JAVA_OPTS=%_JAVA_OPTS%
goto DONE

:DONE
