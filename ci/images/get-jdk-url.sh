#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u402%2B7/bellsoft-jdk8u402+7-linux-amd64.tar.gz"
	;;
	java17)
		echo "https://github.com/bell-sw/Liberica/releases/download/17.0.10%2B13/bellsoft-jdk17.0.10+13-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
