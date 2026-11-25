#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p build/classes
javac -encoding UTF-8 -cp 'lib/hsqldb.jar' -d build/classes src/main/java/com/strikermall/*.java
java -cp 'build/classes:lib/hsqldb.jar' com.strikermall.Server
