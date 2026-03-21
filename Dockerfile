FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --create-home spring

COPY --from=build /app/target/bookingservice-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]