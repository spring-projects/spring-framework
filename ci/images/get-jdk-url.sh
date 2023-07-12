#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u372+7/bellsoft-jdk8u372+7-linux-amd64.tar.gz"
	;;
	java11)
		 echo "https://github.com/bell-sw/Liberica/releases/download/11.0.19%2B7/bellsoft-jdk11.0.19+7-linux-amd64.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
