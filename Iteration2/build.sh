#!/bin/sh
# Builds EducationalLudo.jar from src/. Needs a JDK 17 or later; no internet connection.
cd "$(dirname "$0")" || exit 1
command -v javac >/dev/null 2>&1 || { echo "javac was not found: install a JDK 17 or later."; exit 1; }
rm -rf build/classes && mkdir -p build/classes
find src -name '*.java' > build/sources.txt
echo "Compiling the sources..."
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt || exit 1
echo "Packaging EducationalLudo.jar..."
jar --create --file EducationalLudo.jar --main-class Controller.Main -C build/classes . || exit 1
echo "Done: EducationalLudo.jar"
