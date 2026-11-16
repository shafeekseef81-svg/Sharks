@echo off
rem Builds EducationalLudo.jar from src\ (Model, View, View\components, Controller).
rem Needs a JDK 17 or later; no internet connection. Uses only relative paths, so it also works
rem from folders whose names contain Hebrew letters.
rem A new package must be added to the javac line below (owner: Rakan, SM).
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 goto nojdk

if exist build\classes rmdir /s /q build\classes
mkdir build\classes

echo Compiling the sources...
javac --release 17 -encoding UTF-8 -d build\classes src\Model\*.java src\View\*.java src\View\components\*.java src\Controller\*.java
if errorlevel 1 goto failed

echo Packaging EducationalLudo.jar...
jar --create --file EducationalLudo.jar --main-class Controller.Main -C build\classes .
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
