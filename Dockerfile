# 실행 전용 경량 이미지 사용 (빌드 도구 필요 없음)
FROM eclipse-temurin:17-jdk-alpine

# 작업 디렉토리 설정
WORKDIR /app

# GitHub Actions에서 빌드해서 넘겨준 JAR 파일만 복사
# (주의: build/libs/*.jar 경로가 아니라 그냥 현재 폴더에 있는 걸 복사하도록 함)
COPY app.jar app.jar

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
