# Build the jar inside Docker, so `docker compose up --build` works on a clean checkout
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Run on the same Java version the code is compiled for (17)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/p2p-1.0-SNAPSHOT.jar /app/p2p.jar
CMD ["java", "-jar", "p2p.jar"]
