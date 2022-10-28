FROM ubuntu:focal-20220922

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
RUN ./setup.sh

ENV JAVA_HOME /opt/openjdk/java17
ENV JDK17 /opt/openjdk/java17
ENV JDK18 /opt/openjdk/java18
ENV JDK19 /opt/openjdk/java19

ENV PATH $JAVA_HOME/bin:$PATH
