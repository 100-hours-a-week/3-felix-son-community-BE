# 📣 Just For Share

## 📌 프로젝트 소개

자유롭게 의견을 공유하는 **커뮤니티** 프로젝트입니다.

- `Spring Boot`로 서버를 구현하고, `MySQL`로 데이터베이스를 사용했습니다.
- AWS 클라우드 인프라(ECS, lambda, S3 ..)를 활용하여 확장 가능하고 안정적인 서비스를 구축했습니다.
- **월간 활성 사용자(MAU) 100만명**을 목표로 하는 대규모 커뮤니티 플랫폼입니다.
- JWT 기반 이중 토큰 인증 시스템으로 보안성을 강화했습니다.

---

## 👥 개발 인원 및 기간

- **개발 기간**: 2025-9-15 ~ 2025-12-7 (약 12주)
- **개발 인원**: 프론트엔드/백엔드/인프라 1명 (본인)

---

## 🛠️ 사용 기술 및 Tools

### Back-end
![Java](https://img.shields.io/badge/Java_21-007396?style=flat&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=json-web-tokens&logoColor=white)

### Front-end
![Node.js](https://img.shields.io/badge/Node.js-339933?style=flat&logo=node.js&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat&logo=nginx&logoColor=white)

### Infrastructure & DevOps
![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat&logo=amazon-aws&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=github-actions&logoColor=white)

- **AWS Services**: ALB, Fargate, RDS, S3, Lambda, NAT Gateway
- **CI/CD**: GitHub Actions
- **Containerization**: Docker
- **SSL/TLS**: Let's Encrypt

### Front-end Repository
- [Front-end Github](https://github.com/100-hours-a-week/3-felix-son-community-FE)

---

## 📂 폴더 구조
<details>
<summary>폴더 구조 보기</summary>
  준비중
</details>

## 🏛️ 시스템 아키텍처

<img width="471" height="588" alt="Image" src="https://github.com/user-attachments/assets/4ffe89e4-82f5-4142-b314-f6f8a523af2d" />

### 서버 구조
```
Controller Layer
      ↓
Service Layer
      ↓
Repository Layer
      ↓
Database (MySQL)
```

---

## ⚙️ 구현 기능

### 🔐 인증/인가 시스템

**이중 토큰 전략**
```java
- Access Token (30분)
   - 저장 위치: sessionStorage
   - 용도: API 요청 인증

- Refresh Token (14일)
   - 저장 위치: HTTP-only Cookie
   - 용도: Access Token 자동 갱신
   - CSRF 공격 방지
```

**주요 기능**
- JWT 기반 토큰 발급 및 검증
- 자동 토큰 갱신 메커니즘
- 토큰 만료 시 적절한 예외 처리
- XSS 공격 방지를 위한 저장소 분리

### 👤 사용자 관리 (Users)

- 회원가입, 로그인, 로그아웃
- 비밀번호 암호화
- 프로필 이미지 업로드 (S3)
- 회원 정보 수정 및 탈퇴
- JWT 토큰 기반 인증

### 📝 게시글 관리 (Posts)

- 게시글 CRUD 기능
- 이미지 업로드 (S3 + Lambda 처리)
- 조회수 비동기 업데이트 (성능 최적화)
- 인피니티 스크롤
- 게시글 정렬 기능

**성능 최적화**
```java
// Read-only Transaction으로 조회 성능 향상
@Transactional(readOnly = true)
public Post getPost(Long id) {
    // Dirty Checking 비활성화
}

// 조회수는 비동기로 별도 처리
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void incrementViewCount(Long postId) {
    // 메인 트랜잭션과 분리
}
```

### 💬 댓글 관리 (Comments)

- 댓글 CRUD 기능
- 게시글과의 연관 관계 설정(cascade)
- 댓글 작성자 정보 표시
- 댓글 수정/삭제 권한 검증

### 🖼️ 이미지 처리

**AWS Lambda를 활용한 이미지 처리**
- S3 업로드
- 자동 이미지 최적화
- 사용자별 업로드 할당량 관리 (계획 중)

---

## 🗄️ 데이터베이스 설계

### ERD (Entity Relationship Diagram)


### 요구사항 분석

#### 유저 관리
- 사용자는 이메일, 프로필 이미지, 비밀번호, 닉네임 정보 포함
- 각 유저는 고유한 식별자(ID)를 가짐
- 이메일과 닉네임은 UNIQUE 제약 조건으로 중복 방지
- 비밀번호는 BCrypt로 암호화하여 저장

#### 게시글 관리
- 제목, 내용, 이미지, 작성일시, 수정일시, 조회수 정보 포함
- 작성자(User)를 참조하는 외래키 설정
- 이미지는 S3에 저장하고 URL만 DB에 저장

#### 댓글 관리
- 내용, 작성자, 작성일시, 수정일시 정보 포함
- 게시글(Post)을 참조하는 외래키 설정
- 작성자(User)를 참조하는 외래키 설정

### 주요 Entity 설계

#### User Entity
```java
- id: Long (PK, Auto Increment)
- email: String (UNIQUE, NOT NULL)
- password: String (Encrypted, NOT NULL)
- nickname: String (UNIQUE, NOT NULL)
- profileImageUrl: String
- createdAt: Instant (UTC)
- updatedAt: Instant (UTC)
```

#### Post Entity
```java
- id: Long (PK, Auto Increment)
- title: String (NOT NULL)
- content: Text (NOT NULL)
- imageUrl: String
- viewCount: Long (Default: 0)
- userId: Long (FK → User.id)
- createdAt: Instant (UTC)
- updatedAt: Instant (UTC)
```

#### Comment Entity
```java
- id: Long (PK, Auto Increment)
- content: Text (NOT NULL)
- postId: Long (FK → Post.id)
- userId: Long (FK → User.id)
- createdAt: Instant (UTC)
- updatedAt: Instant (UTC)
```

---

## 🔧 주요 기술적 결정사항


### 1. JWT 예외 처리

**선택**: Exception throw 방식

**이유**:
- Boolean 반환 방식은 적절한 HTTP 상태 코드 전달 불가
- Exception을 통해 401 Unauthorized 명확히 반환
- 클라이언트에서 토큰 갱신 로직 구현 용이
```java
// ❌ Bad
if (!isTokenValid(token)) {
    return false; // HTTP 상태 코드 전달 불가
}

// ✅ Good
if (isTokenExpired(token)) {
    throw new JwtExpiredException("Token has expired"); // 401 반환
}
```

### 2. 보안 저장소 전략

**선택**: sessionStorage + HTTP-only Cookie

**이유**:
| 저장소 | 용도 | XSS | CSRF |
|--------|------|-----|------|
| localStorage | ❌ 사용 안 함 | 취약 | 안전 |
| sessionStorage | Access Token | 취약 | 안전 |
| HTTP-only Cookie | Refresh Token | 안전 | 방어 필요 |

- localStorage는 XSS 공격에 취약하여 완전 배제
- Access Token은 sessionStorage (탭 종료 시 자동 삭제)
- Refresh Token은 HTTP-only Cookie (JavaScript 접근 불가)

### 3. Transaction 최적화

**Read-only Transaction**
```java
@Transactional(readOnly = true)
public Post getPost(Long id) {
    // Dirty Checking 비활성화 → 성능 향상
    // Entity 수정 불가
}
```

**비동기 조회수 업데이트**
```java
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void incrementViewCount(Long postId) {
    // 메인 트랜잭션과 분리
    // 조회수 업데이트 실패가 게시글 조회에 영향 없음
}
```

### 4. Timezone 관리: Instant vs LocalDateTime

**선택**: `Instant` 타입 사용

**이유**:
- LocalDateTime은 timezone 정보가 없어 UTC 저장 시 정보 손실 발생
- Instant는 UTC 기준 시간을 저장하여 timezone 정보 보존
- ISO 8601 형식으로 직렬화 가능
- 글로벌 서비스 확장 시 유리
```java
// ❌ Bad
private LocalDateTime createdAt; // Timezone 정보 손실

// ✅ Good
private Instant createdAt; // UTC 저장, Timezone 안전
```

---

## ☁️ AWS 인프라 구성

### 주요 서비스

| 서비스 | 용도 | 설정 |
|--------|------|------|
| **ALB** | 로드 밸런싱 | Path-based Routing |
| **Fargate** | 컨테이너 실행 | Multi-AZ 배포 |
| **RDS MySQL** | 데이터베이스 | Private Subnet |
| **S3** | 이미지 저장 | Public Read |
| **Lambda** | 이미지 처리 | S3 Trigger |
| **Systems Manager** | 보안 접근 | Session Manager |


---

## 🚀 배포 전략

### CI/CD 파이프라인
```yaml
GitHub Push
    ↓
GitHub Actions 트리거
    ↓
Docker Image Build
    ↓
Private Docker Registry Push
    ↓
AWS Fargate 배포
    ↓
ALB Health Check
    ↓
배포 완료
```

## 📊 성능 최적화

### 1. 조회수 비동기 처리
- 메인 트랜잭션과 분리하여 응답 속도 개선
- 조회수 업데이트 실패가 게시글 조회에 영향 없음

### 2. Read-only Transaction
- Dirty Checking 비활성화로 조회 성능 향상
- 불필요한 쓰기 작업 방지

### 3. Multi-AZ 배포
- 고가용성 확보
- 지연시간 감소
- 장애 대응 능력 향상

### 4. ALB Path-based Routing
- Frontend/Backend 트래픽 효율적 분산
- 독립적인 스케일링 가능

### 5. Lambda 이미지 처리
- 백엔드 서버 부하 분산
- 이벤트 기반 자동 확장

---

## 🔍 트러블 슈팅

### 1. JWT 토큰 만료 처리 문제

**문제 상황**
```java
// 기존 코드
public boolean validateToken(String token) {
    if (isTokenExpired(token)) {
        return false; // HTTP 상태 코드를 전달할 방법이 없음
    }
    return true;
}
```

**해결 방법**
```java
// 개선된 코드
public void validateToken(String token) {
    if (isTokenExpired(token)) {
        throw new JwtExpiredException("Token has expired");
        // GlobalExceptionHandler에서 401 반환
    }
}
```

**배운 점**
- Boolean 반환보다 명확한 예외 처리가 클라이언트 대응에 유리
- HTTP 상태 코드를 통한 명확한 에러 커뮤니케이션 중요

---

### 2. Timezone 정보 손실 문제

**문제 상황**
```java
// LocalDateTime 사용 시
private LocalDateTime createdAt; // Timezone 정보 없음
// 저장: 2024-12-07T10:30:00
// 조회: 2024-12-07T10:30:00 (어느 timezone인지 불명확)
```

**해결 방법**
```java
// Instant 사용
private Instant createdAt; // UTC 기준 저장
// 저장: 2024-12-07T01:30:00Z (UTC)
// 조회: 2024-12-07T01:30:00Z (명확한 UTC 시간)
```

**배운 점**
- 글로벌 서비스에서는 반드시 UTC 기준 시간 저장
- Instant는 ISO 8601 형식으로 직렬화되어 API 응답에도 적합

---

### 3. Read-only Transaction에서 수정 불가 문제

**문제 상황**
```java
@Transactional(readOnly = true)
public Post getPost(Long id) {
    Post post = postRepository.findById(id);
    post.incrementViewCount(); // 작동하지 않음!
    return post;
}
```

**해결 방법**
```java
// 조회와 업데이트 분리
@Transactional(readOnly = true)
public Post getPost(Long id) {
    Post post = postRepository.findById(id);
    viewCountService.incrementAsync(id); // 비동기 처리
    return post;
}

@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void incrementAsync(Long postId) {
    postRepository.incrementViewCount(postId);
}
```

**배운 점**
- Read-only 트랜잭션은 Dirty Checking이 비활성화됨
- 비동기 처리로 성능과 기능성 모두 확보 가능

---

### 4. Fargate Private Subnet 접근 문제

**문제 상황**
- Fargate 컨테이너를 Private Subnet에 배포
- SSH 접근 불가, 디버깅 어려움

**해결 방법**
```bash
# AWS Systems Manager Session Manager 사용
aws ssm start-session --target <task-id>
```

**배운 점**
- Private Subnet의 보안성과 접근성 사이 균형 필요
- Systems Manager로 안전하게 컨테이너 접근 가능

---

### 5. 프론트엔드 컨테이너화 우선 진행

**전략**
- 복잡한 Backend-Database 구성 전에 Frontend 먼저 컨테이너화
- 단순한 것부터 시작하여 점진적 복잡도 증가

**배운 점**
- 복잡한 시스템은 단계적 접근이 효과적
- 작은 성공이 모여 큰 시스템 완성

---

## 향후 개선 계획

### 단기 목표 (1-3개월)

- [ ] Rate Limiting 구현 (사용자별 API 호출 제한)
- [ ] 이미지 업로드 할당량 관리
- [ ] 게시글 검색 기능 최적화
- [ ] 댓글 페이지네이션
- [ ] 좋아요 기능 추가

### 중기 목표 (3-6개월)

- [ ] Redis 캐싱 도입 (조회 성능 개선)
- [ ] ElasticSearch 통합 (전문 검색)
- [ ] 알림 시스템 (WebSocket)
- [ ] 관리자 대시보드
- [ ] 모니터링 시스템 (Prometheus, Grafana)

### 장기 목표 (6개월 이상)

- [ ] MSA 전환 고려
- [ ] Kubernetes 마이그레이션
- [ ] CDN 도입 (CloudFront)
- [ ] 다국어 지원
- [ ] AI 기반 콘텐츠 추천

---

## 💭 프로젝트 후기

이 프로젝트는 단순한 CRUD 애플리케이션을 넘어 **대규모 트래픽을 처리할 수 있는 프로덕션 레벨의 시스템**을 구축하는 것을 목표로 시작했습니다.

### 기술적 도전

Spring Boot를 사용하면서 **보안, 성능, 확장성**을 모두 고려한 아키텍처 설계의 중요성을 깊이 이해하게 되었습니다. 특히 JWT 기반 인증 시스템을 구현하면서 Access Token과 Refresh Token의 저장 위치 선택이 보안에 얼마나 중요한지 배웠습니다.

### 인프라 구축

AWS 인프라를 직접 구축하면서 **이론과 실제의 차이**를 체감했습니다. ALB, Fargate, RDS를 연결하고 보안 그룹을 설정하는 과정에서 네트워크와 보안에 대한 이해도가 크게 향상되었습니다. 특히 Private Subnet에 배포된 서비스에 Systems Manager로 안전하게 접근하는 방법을 익히면서 클라우드 네이티브 아키텍처의 장점을 체감했습니다.

### 성능 최적화

Read-only Transaction으로 조회 성능을 개선하고, 조회수를 비동기로 처리하는 등 **작은 최적화들이 모여 큰 성능 개선**을 만들어낸다는 것을 경험했습니다. 특히 Instant 타입을 사용한 Timezone 관리는 글로벌 서비스 확장 시 필수적임을 깨달았습니다.

### 앞으로의 계획

이번 프로젝트를 통해 백엔드 개발의 기초를 탄탄히 다졌다고 생각합니다. 다음 프로젝트에서는 Redis 캐싱, ElasticSearch 검색, MSA 아키텍처 등 더 고급 기술들을 도입하여 **진정한 엔터프라이즈급 애플리케이션**을 구축하고 싶습니다.

100만 MAU를 넘어 **1억 MAU를 처리할 수 있는 시스템**을 만드는 것이 최종 목표입니다.

---
