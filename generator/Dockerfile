FROM openjdk:12-jdk
EXPOSE 8081
COPY target/generator.jar generator.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","generator.jar"]
