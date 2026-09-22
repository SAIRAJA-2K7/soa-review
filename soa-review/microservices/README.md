# TalentSync Microservice Setup

This folder contains a working gateway-based microservice skeleton that sits alongside the existing project.

## Services

- Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Job Service: http://localhost:8083
- Application Service: http://localhost:8084

## Run

From this directory:

```bash
mvn clean package
```

Then start each app separately:

```bash
java -jar gateway/target/gateway-1.0.0.jar
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar job-service/target/job-service-1.0.0.jar
java -jar application-service/target/application-service-1.0.0.jar
```

## Example routes

- http://localhost:8080/api/auth/health
- http://localhost:8080/api/jobs
- http://localhost:8080/api/applications

This is a safe gateway-based architecture that does not break the current monolithic app while allowing the project to be split into services.
