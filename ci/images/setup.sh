#!/bin/bash
set -ex

###########################################################
# UTILS
###########################################################

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install --no-install-recommends -y tzdata ca-certificates net-tools libxml2-utils git curl libudev1 libxml2-utils iptables iproute2 jq fontconfig
ln -fs /usr/share/zoneinfo/UTC /etc/localtime
dpkg-reconfigure --frontend noninteractive tzdata
rm -rf /var/lib/apt/lists/*

curl https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.4/concourse-java.sh > /opt/concourse-java.sh

###########################################################
# JAVA
###########################################################

mkdir -p /opt/openjdk
pushd /opt/openjdk > /dev/null
for jdk in java17 java21
do
  JDK_URL=$( /get-jdk-url.sh $jdk )
  mkdir $jdk
  pushd $jdk > /dev/null
  curl -L ${JDK_URL} | tar zx --strip-components=1
  test -f bin/java
  test -f bin/javac
  popd > /dev/null
done
popd

###########################################################
# GRADLE ENTERPRISE
###########################################################
cd /
mkdir ~/.gradle
echo 'systemProp.user.name=concourse' > ~/.gradle/gradle.properties
