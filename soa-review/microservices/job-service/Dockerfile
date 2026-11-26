FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY job-service/pom.xml job-service/pom.xml
COPY job-service/src job-service/src
RUN mvn -q -f pom.xml -pl job-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/job-service/target/job-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
