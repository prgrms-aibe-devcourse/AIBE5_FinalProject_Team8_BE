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
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 타임존 + curl (healthcheck 용)
RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 빌드 산출물만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV SPRING_PROFILES_ACTIVE=prod
# 로컬/배포 컨테이너에서 JVM이 호스트 메모리를 과하게 잡지 않도록 heap 상한을 명시합니다.
# JAVA_TOOL_OPTIONS는 java 실행 시 자동 적용되므로 ENTRYPOINT가 바뀌어도 기본 안전장치로 남습니다.
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
# 실제 운영 리소스가 다르면 JAVA_OPTS로 추가 JVM 옵션을 넘깁니다.
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
