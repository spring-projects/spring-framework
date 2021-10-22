FROM ubuntu:focal-20210827

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
RUN ./setup.sh

ENV JAVA_HOME /opt/openjdk/java17
ENV JDK17 /opt/openjdk/java17

ENV PATH $JAVA_HOME/bin:$PATH
