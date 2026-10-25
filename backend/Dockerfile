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
ENV DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-}

# 敏感配置（DATABASE_PASSWORD / JWT_SECRET）不写入镜像层：
# 镜像 ENV 可通过 docker inspect 直接读取。
# 请在运行时由 docker-compose 的 env_file 注入项目根目录的 .env。

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
  CMD curl -f http://localhost:8080/api/health || exit 1