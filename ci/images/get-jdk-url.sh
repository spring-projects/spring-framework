#!/bin/bash
set -e

case "$1" in
  java17)
    echo "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.1%2B12/OpenJDK17U-jdk_x64_linux_hotspot_17.0.1_12.tar.gz"
  ;;
  java18)
    echo "https://github.com/adoptium/temurin18-binaries/releases/download/jdk-2021-11-17-08-12-beta/OpenJDK-jdk_x64_linux_hotspot_2021-11-16-23-30.tar.gz"
  ;;
  *)
    echo $"Unknown java version"
    exit 1
esac
