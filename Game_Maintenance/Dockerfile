# syntax=docker/dockerfile:1

# Reproducible build: the project currently compiles with Java 25.
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy POM files first so Docker can cache downloaded Maven dependencies.
COPY pom.xml ./pom.xml
COPY Models/pom.xml ./Models/pom.xml
COPY Negocio/pom.xml ./Negocio/pom.xml
COPY Persistencia/pom.xml ./Persistencia/pom.xml
RUN mvn -B -pl Persistencia -am dependency:go-offline

COPY Models/src ./Models/src
COPY Negocio/src ./Negocio/src
COPY Persistencia/src ./Persistencia/src
RUN mvn -B -pl Persistencia -am -DskipTests package

# A small runtime image: classes, resources, and the exact runtime jars only.
FROM eclipse-temurin:25.0.4_7-jre-ubi10-minimal
WORKDIR /app

COPY --from=build /workspace/Persistencia/target/classes ./classes
COPY --from=build /workspace/Persistencia/target/dependency ./lib

# Render overrides PORT at runtime. This value is only useful when running the
# image locally without passing -e PORT.
ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["java", "-cp", "/app/classes:/app/lib/*", "ServerMain.ServerMain"]
