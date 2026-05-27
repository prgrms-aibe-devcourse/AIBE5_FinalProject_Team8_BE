# Stage 1: Build
FROM gradle:8.7-jdk17 AS builder

# 캐시 최적화, 별도의 레이어 분리
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스코드 복사, JAR 빌드(테스트 제외)
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존 설정 (Docker는 영국기준이라서 한국보다 9시간 느림)
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

# 빌드 결과물만 복사(소스코드나 Gradle 캐시 등은 버림)
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 설정
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]