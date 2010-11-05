@echo off

::Backup environment vars
set _JAVA_HOME=%JAVA_HOME%
set _JAVA_OPTS=%JAVA_OPTS%

::Check for existing JAVA_HOME
if exist "%JAVA_HOME%\bin\java.exe" goto OPTIONS

::Find the current (most recent) Java version
reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment" > reg1.txt
type reg1.txt | find "CurrentVersion" > reg2.txt
if errorlevel 1 goto ERROR
for /f "tokens=3" %%x in (reg2.txt) do set JAVA_VERSION=%%x
if errorlevel 1 goto ERROR
del reg1.txt reg2.txt 2> nul

::Find the Java home directory
reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment\%JAVA_VERSION%" > reg1.txt
type reg1.txt | find "JavaHome" > reg2.txt
if errorlevel 1 goto ERROR
for /f "tokens=3,4" %%x in (reg2.txt) do set JAVA_HOME=%%x %%y
if errorlevel 1 goto ERROR
del reg1.txt reg2.txt 2> nul

::Check for existence of java.exe
if not exist "%JAVA_HOME%\bin\java.exe" goto ERROR

::Setup Java options variable
:OPTIONS
set JAVA_OPTS=-Xbootclasspath/p:lib/js.jar %JAVA_OPTS%
echo %JAVA_OPTS% | find "DDEBUG" > reg1.txt
if errorlevel 1 goto FINDJAR
set JAVA_OPTS=%JAVA_OPTS% -Djava.util.logging.config.file=lib\debug.properties

::Setup the JARFILE variable
:FINDJAR
del reg1.txt 2> nul
for %%x in (lib\rapidcontext-*.jar) do set JARFILE=%%~x
goto DONE

:ERROR
del reg1.txt reg2.txt 2> nul
echo ERROR: Failed to find a Java 1.4 compatible installation.
exit /b 1

:DONE
