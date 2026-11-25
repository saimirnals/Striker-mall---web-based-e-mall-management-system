#!/usr/bin/env bash
# Build & run without Maven. Needs a JDK 17+ and lib/mysql-connector-j-*.jar
cd "$(dirname "$0")/.."
JAR=$(ls lib/mysql-connector-j-*.jar 2>/dev/null | head -1)
[ -z "$JAR" ] && { echo "Put mysql-connector-j-8.x.jar into the lib/ folder first."; exit 1; }
rm -rf out && mkdir out
javac -d out -cp "$JAR" $(find src/main -name '*.java') || exit 1
java -cp "out:$JAR" com.emall.Main
