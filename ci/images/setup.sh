#!/bin/bash
set -ex

###########################################################
# UTILS
###########################################################

apt-get update
apt-get install --no-install-recommends -y ca-certificates net-tools libxml2-utils git curl libudev1 libxml2-utils iptables iproute2 jq fontconfig
rm -rf /var/lib/apt/lists/*

curl https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.3/concourse-java.sh > /opt/concourse-java.sh

curl --output /opt/concourse-release-scripts.jar https://repo.spring.io/release/io/spring/concourse/releasescripts/concourse-release-scripts/0.2.0/concourse-release-scripts-0.2.0.jar

###########################################################
# JAVA
###########################################################
JDK_URL=$( ./get-jdk-url.sh $1 )

mkdir -p /opt/openjdk
cd /opt/openjdk
curl -L ${JDK_URL} | tar zx --strip-components=1
test -f /opt/openjdk/bin/java
test -f /opt/openjdk/bin/javac

###########################################################
# GRADLE ENTERPRISE
###########################################################
cd /
mkdir ~/.gradle
echo 'systemProp.user.name=concourse' > ~/.gradle/gradle.properties
