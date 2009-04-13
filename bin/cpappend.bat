@echo off
if "%CLASSPATH%" == "" goto EMPTY
set CLASSPATH=%1;%CLASSPATH%
goto DONE
:EMPTY
set CLASSPATH=%1
:DONE
