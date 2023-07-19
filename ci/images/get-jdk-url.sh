#!/bin/bash
set -e

case "$1" in
	java17)
		echo "https://github.com/bell-sw/Liberica/releases/download/17.0.7+7/bellsoft-jdk17.0.7+7-linux-amd64.tar.gz"
	;;
	java21)
		echo "https://download.java.net/java/early_access/jdk21/31/GPL/openjdk-21-ea+31_linux-x64_bin.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
