# 1단계: 빌드 단계
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Gradle 의존성 캐싱을 위해 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN gradle clean bootJar --no-daemon -x test

# 2단계: 실행 단계
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 비root 사용자 생성 (보안)
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 소유권 변경
RUN chown appuser:appgroup app.jar

# 비root 사용자로 전환
USER appuser

# 애플리케이션 실행 (JVM 최적화 옵션 추가)
EXPOSE 8080
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]