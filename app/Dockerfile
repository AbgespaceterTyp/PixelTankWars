FROM openjdk:12-jdk
#EXPOSE 8080
COPY target/app.jar app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
