# Imagen opcional para hosting con Docker (p. ej. Azure Container Apps). App Service con Java SE no la necesita: ver AZURE.md.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# Límite de memoria pensado para planes gratuitos de 512 MB.
ENTRYPOINT ["java", "-Xmx320m", "-jar", "app.jar"]
