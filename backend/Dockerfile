FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/codeeval-1.0.0.jar app.jar

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV DATABASE_URL=jdbc:postgresql://db:5432/codeeval
ENV DATABASE_USERNAME=codeeval
ENV DATABASE_PASSWORD=codeeval123
ENV JWT_SECRET=your-256-bit-secret-key-here-change-in-production
ENV DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-}

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
  CMD curl -f http://localhost:8080/api/health || exit 1