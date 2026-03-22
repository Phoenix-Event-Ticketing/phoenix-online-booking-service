FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# Exploded layers: dependency layers change rarely → smaller GCR uploads when only app code changes
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /app
COPY --from=build /app/target/bookingservice-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /app/layers

# Alpine JRE is much smaller than default Ubuntu-based eclipse-temurin:21-jre (~130MB+ savings on base)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=extract /app/layers/dependencies/ ./
COPY --from=extract /app/layers/spring-boot-loader/ ./
COPY --from=extract /app/layers/snapshot-dependencies/ ./
COPY --from=extract /app/layers/application/ ./

EXPOSE 8083

USER spring

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
