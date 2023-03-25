#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u345+1/bellsoft-jdk8u345+1-linux-amd64.tar.gz"
	;;
	java17)
		echo "https://github.com/bell-sw/Liberica/releases/download/17.0.4+8/bellsoft-jdk17.0.4+8-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
