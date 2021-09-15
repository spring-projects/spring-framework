#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/adoptium/temurin17-binaries/releases/download/jdk17-2021-09-15-08-15-beta/OpenJDK17-jdk_x64_linux_hotspot_2021-09-15-08-15.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
