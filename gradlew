#!/bin/sh
# Gradle wrapper script for Unix
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || \
  java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || \
  echo "Run: gradle build  (install Gradle 8.10 manually if wrapper jar is absent)"
