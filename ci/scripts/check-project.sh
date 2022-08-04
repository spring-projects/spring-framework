#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false -Porg.gradle.java.installations.fromEnv=JDK11,JDK15 \
  -PmainToolchain=${MAIN_TOOLCHAIN} -PtestToolchain=${TEST_TOOLCHAIN} --no-daemon --max-workers=4 check
popd > /dev/null
