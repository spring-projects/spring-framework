FROM ubuntu:focal-20210119

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
RUN ./setup.sh java8

ENV JAVA_HOME /opt/openjdk/java8
ENV JDK11 /opt/openjdk/java11
ENV JDK15 /opt/openjdk/java15

ENV PATH $JAVA_HOME/bin:$PATH
