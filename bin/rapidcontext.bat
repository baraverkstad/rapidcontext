@echo off

::Setup default values
set _JAVA=java.exe
set _JAVA_OPTS=%JAVA_OPTS%

::Change to application directory & locate JAR file
for %%x in (%0) do pushd %%~dpsx
cd ..
for %%x in (lib\rapidcontext-*.jar) do set _JARFILE=%%~x

::Check for command-line execution
if not "%1" == "" goto JAVA
start javaw.exe %_JAVA_OPTS% -jar %_JARFILE%
goto DONE

::Check for Java executable
:JAVA
if not exist "%JAVA_HOME%\bin\java.exe" goto OPTIONS
set _JAVA=%JAVA_HOME%\bin\java.exe

::Check for debug flag in Java options
:OPTIONS
set _JAVA_OPTS=%_JAVA_OPTS% -Dorg.mortbay.jetty.Request.maxFormContentSize=1000000
echo %_JAVA_OPTS% | find "DDEBUG" > nul
if errorlevel 1 goto RUNAPP
set _JAVA_OPTS=%_JAVA_OPTS% -Djava.util.logging.config.file=lib\debug.properties

::Run application
:RUNAPP
"%_JAVA%" %_JAVA_OPTS% -jar %_JARFILE% %1 %2 %3 %4 %5 %6 %7 %8 %9

:DONE
set _JAVA=
set _JAVA_OPTS=
set _JARFILE=
popd
