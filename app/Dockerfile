FROM openjdk:12-jdk
COPY target/app.jar app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
