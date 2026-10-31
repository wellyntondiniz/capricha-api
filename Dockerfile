# Etapa 1: Build com Maven 3.9 e Java 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m"

COPY pom.xml ./
COPY src/ src/
RUN mvn package -DskipTests -B

# Etapa 2: Runtime enxuto com JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-Xmx512m"

RUN mkdir -p /app/uploads/eventos
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
