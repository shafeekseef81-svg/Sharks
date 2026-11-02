@echo off
rem Builds EducationalLudo.jar from the sources. Needs a JDK 17 or later; no internet connection.
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 goto nojdk

if exist build\classes rmdir /s /q build\classes
mkdir build\classes

echo Compiling the sources...
javac --release 17 -encoding UTF-8 -d build\classes src\main\*.java src\model\*.java src\data\*.java src\components\*.java src\view\*.java src\controller\*.java src\devtools\*.java
if errorlevel 1 goto failed

echo Packaging EducationalLudo.jar...
jar --create --file EducationalLudo.jar --main-class main.Main -C build\classes .
if errorlevel 1 goto failed

echo Done: EducationalLudo.jar
exit /b 0

:nojdk
echo [ERROR] javac was not found.
echo Install a Java Development Kit 17 or later, for example Eclipse Temurin, then run build.bat again.
exit /b 1

:failed
echo [ERROR] The build failed. See the messages above.
exit /b 1
