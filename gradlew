#!/bin/sh
# Minimal gradlew: prefers wrapper jar, falls back to system gradle (used by CI).
APP_BASE_NAME=`basename "$0"`
APP_HOME=`cd "${0%/*}" >/dev/null; pwd`
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
  exec java -jar "$WRAPPER_JAR" "$@"
elif command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "gradle not found. Install Gradle 8.7 or add gradle/wrapper/gradle-wrapper.jar" >&2
  exit 1
fi
