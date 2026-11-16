#!/bin/sh
# Starts Educational Ludo. Works offline; needs Java 17 or later.
cd "$(dirname "$0")" || exit 1
command -v java >/dev/null 2>&1 || { echo "Java 17 or later is required to run Educational Ludo."; exit 1; }
[ -f EducationalLudo.jar ] || ./build.sh || exit 1
exec java -jar EducationalLudo.jar
