###############################################################
###########					Buildpart				###########
###############################################################

FROM openjdk:8-jdk as builder

# Generic
RUN apt-get update
RUN apt-get -y install curl tar bash apt-transport-https
RUN apt-get -y install libfontconfig
# Scala
RUN wget http://www.scala-lang.org/files/archive/scala-2.12.6.tgz
RUN tar zxf scala-2.12.6.tgz
RUN mv scala-2.12.6 /usr/share
RUN ln -s /usr/share/scala-2.12.6 /usr/share/scala
RUN ln -s /usr/share/scala/bin/scala /usr/bin/scala
RUN ln -s /usr/share/scala/bin/scalac /usr/bin/scalac
RUN ln -s /usr/share/scala/bin/fsc /usr/bin/fsc
RUN ln -s /usr/share/scala/bin/scaladoc /usr/bin/scaladoc
RUN ln -s /usr/share/scala/bin/scalap /usr/bin/scalap
# sbt
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
RUN apt-get update
RUN apt-get install sbt

# NPM
#RUN apt-get -y install npm
# Maven
ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

VOLUME "$USER_HOME_DIR/.m2"

RUN mkdir -p /build
WORKDIR /build
COPY . /build

CMD ["mvn", "clean", "install", "-P", "generator"]

###############################################################
###########					Runtimepart				###########
###############################################################

FROM openjdk:11-slim as runtime
EXPOSE 8081
#Set app home folder
ENV APP_HOME /app
#Possibility to set JVM options (https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)
ENV JAVA_OPTS=""
 
#Create base app folder
RUN mkdir $APP_HOME
#Create folder to save configuration files
RUN mkdir $APP_HOME/config
#Create folder with application logs
RUN mkdir $APP_HOME/log
 
VOLUME $APP_HOME/log
VOLUME $APP_HOME/config
 
WORKDIR $APP_HOME
#Copy executable jar file from the builder image
COPY --from=builder /build/target/generator.jar generator.jar
 
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar generator.jar" ]
#Second option using shell form:
#ENTRYPOINT exec java $JAVA_OPTS -jar generator.jar $0 $@
