#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 check
popd > /dev/null
