FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
WORKDIR /app
COPY --from=build /workspace/target/taskflow-pro-backend-1.0.0.jar app.jar
COPY docker-entrypoint.sh docker-entrypoint.sh
RUN chmod 0555 docker-entrypoint.sh
USER taskflow
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
