#!/bin/sh
# Builds EducationalLudo.jar from the sources. Needs a JDK 17 or later; no internet connection.
cd "$(dirname "$0")" || exit 1
command -v javac >/dev/null 2>&1 || { echo "javac was not found: install a JDK 17 or later."; exit 1; }
rm -rf build/classes && mkdir -p build/classes
echo "Compiling the sources..."
javac --release 17 -encoding UTF-8 -d build/classes $(find src -name '*.java') || exit 1
echo "Packaging EducationalLudo.jar..."
jar --create --file EducationalLudo.jar --main-class main.Main -C build/classes . || exit 1
echo "Done: EducationalLudo.jar"
