FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY domain/pom.xml ./domain/pom.xml
COPY application/pom.xml ./application/pom.xml
COPY infrastructure/pom.xml ./infrastructure/pom.xml
COPY adapters/pom.xml ./adapters/pom.xml
COPY bootstrap/pom.xml ./bootstrap/pom.xml

COPY domain ./domain
COPY application ./application
COPY infrastructure ./infrastructure
COPY adapters ./adapters
COPY bootstrap ./bootstrap

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
