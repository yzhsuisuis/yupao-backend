FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY target/user-center-backend-master-1.0-SNAPSHOT.jar ./app/application.jar

EXPOSE 8080

CMD ["java","-jar","./app/application.jar","--spring.profiles.active=prod"]




# 后端
# docker build -t user-center-backend:v0.0.1 .

# 前端
# docker build -t user-center-front:v0.0.1 .
