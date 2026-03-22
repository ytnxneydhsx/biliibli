FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml ./
RUN mvn -s /root/.m2/settings.xml -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -s /root/.m2/settings.xml clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /build/target/bilibili-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
