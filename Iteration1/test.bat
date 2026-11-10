@echo off
rem Builds the game, compiles the JUnit 5 tests in test\ and runs them.
rem Exit code 0 = all tests passed. JUnit (lib\) is used only for tests; it is not packed into the JAR.
rem Every test package must be listed on the javac line below, as in build.bat (owner: Rakan, SM).
setlocal
cd /d "%~dp0"

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

set JUNIT=lib\junit-platform-console-standalone-1.10.2.jar
if not exist %JUNIT% goto nojunit

if exist build\test-classes rmdir /s /q build\test-classes
mkdir build\test-classes

echo Compiling the tests...
javac --release 17 -encoding UTF-8 -d build\test-classes -cp "build\classes;%JUNIT%" test\Model\*.java test\View\*.java test\Architecture\*.java
if errorlevel 1 goto failed

echo Running the tests...
java -jar %JUNIT% execute --class-path "build\classes;build\test-classes" --scan-class-path --disable-banner --details=tree
if errorlevel 1 goto failed
exit /b 0

:nojunit
echo [ERROR] %JUNIT% is missing. See README.md, section "Tests".
exit /b 1

:failed
echo [ERROR] Tests failed or did not compile. See the messages above.
exit /b 1
