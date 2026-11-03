#!/bin/sh
# Builds the game, compiles the JUnit 5 tests in test/ and runs them. Exit code 0 = all tests passed.
cd "$(dirname "$0")" || exit 1
./build.sh || exit 1
JUNIT=lib/junit-platform-console-standalone-1.10.2.jar
# Java on Windows (Git Bash, MSYS, Cygwin) separates class-path entries with ';', elsewhere with ':'.
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
  *) SEP=':' ;;
esac
[ -f "$JUNIT" ] || { echo "$JUNIT is missing. See README.md, section Tests."; exit 1; }
rm -rf build/test-classes && mkdir -p build/test-classes
find test -name '*.java' > build/test-sources.txt
echo "Compiling the tests..."
javac --release 17 -encoding UTF-8 -d build/test-classes -cp "build/classes${SEP}$JUNIT" @build/test-sources.txt || exit 1
echo "Running the tests..."
java -jar "$JUNIT" execute --class-path "build/classes${SEP}build/test-classes" --scan-class-path --disable-banner --details=tree
