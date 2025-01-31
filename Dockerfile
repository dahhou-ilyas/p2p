# Utiliser une image de base Java
FROM openjdk:17-jdk-slim

# Créer un répertoire pour l'application
WORKDIR /app

# Copier le JAR de ton projet dans le conteneur
COPY target/p2p-1.0-SNAPSHOT.jar /app/p2p-1.0-SNAPSHOT.jar

CMD ["java", "-jar", "p2p-1.0-SNAPSHOT.jar"]
