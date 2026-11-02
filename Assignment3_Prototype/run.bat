@echo off
rem Starts the Educational Ludo prototype. Works offline; needs Java 17 or later.
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 goto nojava

if exist EducationalLudo.jar goto start
echo EducationalLudo.jar was not found, building it first...
call "%~dp0build.bat"
if errorlevel 1 goto failed

:start
where javaw >nul 2>nul
if errorlevel 1 goto console
start "" javaw -jar EducationalLudo.jar
exit /b 0

:console
java -jar EducationalLudo.jar
exit /b 0

:nojava
echo Java 17 or later is required to run the prototype.
echo Install it, for example Eclipse Temurin 17 or later, and start run.bat again.
pause
exit /b 1

:failed
pause
exit /b 1
