#!/bin/bash
set -e

case "$1" in
	java17)
		echo "https://github.com/bell-sw/Liberica/releases/download/17.0.10%2B13/bellsoft-jdk17.0.10+13-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
