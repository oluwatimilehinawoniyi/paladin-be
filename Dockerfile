FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:resolve

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
EXPOSE 8080

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
