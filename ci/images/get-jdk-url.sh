#!/bin/bash
set -e

case "$1" in
  java17)
    echo "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17%2B35/OpenJDK17-jdk_x64_linux_hotspot_17_35.tar.gz"
  ;;
  java18)
    echo "https://github.com/adoptium/temurin18-binaries/releases/download/jdk-2021-10-22-05-05-beta/OpenJDK-jdk_x64_linux_hotspot_2021-10-21-23-30.tar.gz"
  ;;
  *)
    echo $"Unknown java version"
    exit 1
esac
