#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v javac >/dev/null || { echo "Install JDK 21 or later first."; exit 1; }
mkdir -p build/classes
find src/main/java -name '*.java' -print0 | xargs -0 javac --add-modules jdk.httpserver -encoding UTF-8 -d build/classes
mkdir -p build/classes/public
cp -R src/main/resources/public/. build/classes/public/
jar --create --file build/online-examination-web.jar --main-class com.exam.web.ExamWebApplication -C build/classes .
echo "Open http://localhost:8080 in your browser. Press Ctrl+C to stop."
java --add-modules jdk.httpserver -jar build/online-examination-web.jar
