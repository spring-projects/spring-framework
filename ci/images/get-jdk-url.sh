#!/bin/bash
set -e

case "$1" in
	java17)
		echo "https://download.bell-sw.com/java/17.0.9+11/bellsoft-jdk17.0.9+11-linux-amd64.tar.gz"
	;;
	java21)
		echo "https://download.bell-sw.com/java/21.0.1+12/bellsoft-jdk21.0.1+12-linux-amd64.tar.gz"
	;;
	java22)
		echo "https://download.java.net/java/early_access/jdk22/19/GPL/openjdk-22-ea+19_linux-x64_bin.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
