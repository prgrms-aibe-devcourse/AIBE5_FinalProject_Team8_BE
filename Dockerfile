# ─────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────
FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

# 의존성 레이어 캐시 최적화
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 복사 & JAR 빌드 (테스트 제외)
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ─────────────────────────────────────────
# Stage 2: Run
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 타임존 + curl (healthcheck 용)
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

# 빌드 산출물만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

