#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false -Porg.gradle.java.installations.fromEnv=JDK17,JDK21 \
  --no-daemon --max-workers=4 check
popd > /dev/null
