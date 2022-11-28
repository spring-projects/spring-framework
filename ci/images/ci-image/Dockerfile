FROM ubuntu:focal-20220922

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
RUN ./setup.sh java8

ENV JAVA_HOME /opt/openjdk/java8
ENV JDK17 /opt/openjdk/java17

ENV PATH $JAVA_HOME/bin:$PATH
