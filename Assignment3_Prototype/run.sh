#!/bin/sh
# Starts the Educational Ludo prototype. Works offline; needs Java 17 or later.
cd "$(dirname "$0")" || exit 1
command -v java >/dev/null 2>&1 || { echo "Java 17 or later is required to run the prototype."; exit 1; }
[ -f EducationalLudo.jar ] || ./build.sh || exit 1
exec java -jar EducationalLudo.jar
