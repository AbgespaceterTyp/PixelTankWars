FROM openjdk:12-jdk
COPY target/generator.jar generator.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","generator.jar"]
