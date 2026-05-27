#!/bin/sh

set -eu
DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
