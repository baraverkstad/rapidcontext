@echo off

::Change to application directory
for %%x in (%0) do pushd %%~dpsx
cd ..

::Set default values
set _JAVA=java.exe
for %%x in (lib\rapidcontext-*.jar) do set _JAR=%%~x
set _OPTS=%JAVA_TOOL_OPTIONS%
set _CONF=lib\logging.properties

::Check for debug flag in options
echo %_OPTS% | find "DDEBUG" > nul
if errorlevel 1 goto CONFIG
set _CONF=lib\debug.properties

::Set options
:CONFIG
set JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% -Djava.util.logging.config.file=%_CONF%
set JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% -Dorg.eclipse.jetty.server.Request.maxFormContentSize=1000000

::Run application
if "%1" == "" (
    start javaw.exe -jar %_JAR%
    goto DONE
)
if exist "%JAVA_HOME%\bin\java.exe" set _JAVA=%JAVA_HOME%\bin\java.exe
"%_JAVA%" %_OPTS% -jar %_JAR% %1 %2 %3 %4 %5 %6 %7 %8 %9

:DONE
set JAVA_TOOL_OPTIONS=%_OPTS%
set _JAVA=
set _JAR=
set _OPTS=
set _CONF=
popd
