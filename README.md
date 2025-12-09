# 📣 Just For Share

## 📌 프로젝트 소개
- 자유롭게 의견을 공유하는 커뮤니티 서비스의 백엔드 레포지토리입니다.
- Spring Boot 기반의 REST API 서버, MySQL 기반의 RDS를 사용하였습니다.
- AWS 인프라(ALB, ECS Fargate, S3, Lambda, RDS 등)를 활용해 확장 가능하고 안정적인 서비스를 목표로 합니다.
- 월간 활성 사용자(MAU) 100만 명 이상을 견딜 수 있는 구조를 지향합니다.

---

## 👥 개발 인원 및 기간

- **개발 기간**: 2025-9-15 ~ 2025-12-07 (약 12주)
- **개발 인원**: FE/BE/인프라 1명 (본인)

---

## 🛠️ 사용 기술 및 Tools

### Backend
![Java](https://img.shields.io/badge/Java_21-007396?style=flat&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=json-web-tokens&logoColor=white)

### Infrastructure & DevOps
![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat&logo=amazon-aws&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=github-actions&logoColor=white)

- **AWS Services**: ALB, ECS Fargate, RDS, S3, Lambda, SSM
- **CI/CD**: GitHub Actions
- **Containerization**: Docker
- **SSL/TLS**: ACM(Amazon Certificate Manager)

### ※ Frontend Repository
- [Frontend Github](https://github.com/100-hours-a-week/3-felix-son-community-FE)

---
## 🏛️ 시스템 아키텍처

<img width="471" height="588" alt="Image" src="https://github.com/user-attachments/assets/4ffe89e4-82f5-4142-b314-f6f8a523af2d" />

### 3-Tier 아키텍처

| Layer | 서비스 | 주요 설정 |
| :--- | :--- | :--- |
| **Presentation** | ALB | Path-based Routing, HTTPS(ACM) |
| **Application** | ECS Fargate | Multi-AZ 배포, Auto Scaling |
| **Data** | RDS MySQL | Private Subnet, Multi-AZ |
| **Storage** | S3 + Lambda | 이미지 자동 최적화, Public Read |

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
- 게시글과의 연관 관계 설정
- 댓글 작성자 정보 표시
- 댓글 수정/삭제 권한 검증

### 🖼️ 이미지 처리

**AWS Lambda를 활용한 이미지 처리**
- PresignUrl을 통한 S3 업로드
- S3 업로드 이후 자동 이미지 최적화
- 사용자별 업로드 할당량 관리

---

## 🗄️ 데이터베이스 설계

### 📋 요구사항 분석
#### 1. 기능적 요구사항
<details>
<summary>기능적 요구사항 보기</summary>
  
**1.1 사용자 인증 및 관리**
- 사용자는 이메일과 비밀번호로 회원가입 및 로그인할 수 있어야 함
- 이메일과 닉네임은 중복되지 않아야 함
- 비밀번호는 안전하게 암호화되어 저장되어야 함
- 사용자는 프로필 이미지와 닉네임을 수정할 수 있어야 함
- 사용자는 계정을 비활성화할 수 있어야 함
- JWT 기반 인증으로 API 접근 제어가 이루어져야 함
- Refresh Token을 통한 자동 로그인 연장이 지원되어야 함

**1.2 게시글 관리**
- 사용자는 제목과 본문을 포함한 게시글을 작성할 수 있어야 함
- 게시글에는 최대 5장의 이미지를 첨부할 수 있어야 함
- 사용자는 자신이 작성한 게시글을 수정 및 삭제할 수 있어야 함
- 게시글 목록은 페이징 처리되어 조회되어야 함
- 게시글 상세 조회 시 조회수가 자동으로 증가해야 함
- 게시글에는 좋아요 수, 조회수, 댓글 수가 표시되어야 함

**1.3 댓글 관리**
- 사용자는 게시글에 댓글을 작성할 수 있어야 함
- 사용자는 자신이 작성한 댓글을 수정 및 삭제할 수 있어야 함
- 댓글 작성/삭제 시 게시글의 댓글 수가 자동으로 업데이트되어야 함
- 댓글은 작성 시간 순으로 정렬되어 표시되어야 함

**1.4 좋아요 기능**
- 사용자는 게시글에 좋아요를 추가하거나 취소할 수 있어야 함
- 한 사용자는 동일 게시글에 한 번만 좋아요를 할 수 있어야 함
- 좋아요 추가/취소 시 게시글의 좋아요 수가 자동으로 업데이트되어야 함

**1.5 이미지 관리**
- 이미지는 AWS S3에 업로드되어야 함
- 업로드된 이미지의 URL이 데이터베이스에 저장되어야 함
- 게시글 삭제 시 관련 이미지도 함께 삭제되어야 함
- 이미지는 등록된 순서대로 정렬되어 표시되어야 함
  
</details>

#### 2. 비기능적 요구사항
<details>
<summary>비기능적 요구사항 보기</summary>
  
**2.1 보안**
- 모든 비밀번호는 BCrypt 알고리즘으로 해싱되어 저장
- JWT 토큰 기반 인증으로 API 보안 유지
- Refresh Token은 안전하게 관리되고 만료 시간이 적용됨
- 사용자는 자신의 리소스(게시글, 댓글)만 수정/삭제 가능

**2.2 성능**
- LAZY Loading을 통한 N+1 문제 방지
- 게시글 통계(조회수, 좋아요 수, 댓글 수)는 별도 테이블로 분리하여 조회 성능 최적화
- 페이징 처리로 대량 데이터 조회 시 성능 보장
- UUID 사용으로 분산 환경에서의 ID 충돌 방지

**2.3 확장성**
- 컨테이너 기반(Docker) 배포로 수평 확장 가능
- AWS ECS Fargate를 통한 서버리스 컨테이너 실행
- S3를 통한 이미지 저장으로 스토리지 확장성 보장
- RDS를 통한 데이터베이스 관리 및 백업

**2.4 데이터 무결성**
- 외래키 제약 조건으로 참조 무결성 보장
- 유니크 제약으로 이메일/닉네임 중복 방지
- Cascade 설정으로 부모 엔티티 삭제 시 자식 엔티티 자동 삭제
- 복합 유니크 제약으로 좋아요 중복 방지
- JPA Auditing으로 생성/수정 시간 자동 관리

**2.5 가용성**
- CI/CD 파이프라인(GitHub Actions)을 통한 자동 배포
- Blue-Green 배포 전략으로 무중단 배포 지원
- 컨테이너 헬스체크를 통한 장애 자동 복구
</details>

### 📎 엔티티 연관관계
<details>
  <summary>엔티티 연관관계 보기</summary>
  
#### User (사용자)
- **PK**: `userId` (UUID, BINARY(16))
- **생성 전략**: UUID 자동 생성
- **관계**: Post, Comment, PostLike, RefreshToken의 참조 대상

#### Post (게시글)
- **PK**: `postId` (UUID, BINARY(16))
- **생성 전략**: UUID 자동 생성
- **외래키**: `user_id` → User (작성자)
- **관계**:
  - User와 N:1 관계 (한 사용자가 여러 게시글 작성)
  - PostImage와 1:N 관계 (한 게시글에 여러 이미지)
  - PostStats와 1:1 관계 (게시글당 하나의 통계)
  - Comment와 1:N 관계 (한 게시글에 여러 댓글)
  - PostLike와 1:N 관계 (한 게시글에 여러 좋아요)

#### PostImage (게시글 이미지)
- **PK**: `imageId` (Long, Auto Increment)
- **외래키**: `post_id` → Post
- **관계**: Post와 N:1 관계

#### PostStats (게시글 통계)
- **PK**: `postId` (UUID, BINARY(16)) - **Post의 PK를 공유 (@MapsId)**
- **외래키**: `post_id` → Post
- **관계**: Post와 1:1 관계 (좋아요 수, 조회 수, 댓글 수 관리)

#### Comment (댓글)
- **PK**: `commentId` (UUID, BINARY(16))
- **생성 전략**: UUID 자동 생성
- **외래키**: 
  - `user_id` → User (작성자)
  - `post_id` → Post (소속 게시글)
- **관계**: User 및 Post와 N:1 관계

#### PostLike (좋아요)
- **PK**: `likeId` (Long, Auto Increment)
- **유니크 제약**: `(post_id, user_id)` - 중복 좋아요 방지
- **외래키**:
  - `post_id` → Post
  - `user_id` → User
- **관계**: Post 및 User와 N:1 관계

#### RefreshToken (리프레시 토큰)
- **PK**: `refreshId` (Long, Auto Increment)
- **외래키**: `user_id` → User
- **관계**: User와 N:1 관계 (한 사용자가 여러 디바이스에서 로그인 가능)

</details>

### 핵심 설계 특징
<details>
<summary>핵심 설계 특징 보기</summary>
  
1. **UUID vs Auto Increment**
   - UUID: User, Post, Comment (분산 환경 대비, 예측 불가능한 ID)
   - Auto Increment: PostImage, PostLike, RefreshToken (순차적 접근이 유리한 경우)

2. **성능 최적화**
   - 모든 `@ManyToOne`은 `FetchType.LAZY` 적용 (지연 로딩)
   - N+1 문제 방지를 위한 전략적 페치 조인 사용

3. **데이터 무결성**
   - PostStats는 `@MapsId`로 Post의 PK를 공유하여 1:1 관계 보장
   - PostLike는 `(post_id, user_id)` 복합 유니크 제약으로 중복 방지
   - `Cascade.ALL` + `orphanRemoval = true`로 부모 삭제 시 자식 자동 삭제

4. **감사(Auditing)**
   - `@CreatedDate`, `@LastModifiedDate`로 생성/수정 시간 자동 관리
   - `@EntityListeners(AuditingEntityListener.class)` 적용
     
</details>

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

---

## 🔍 트러블 슈팅

### 1. JWT 토큰 저장소 선택 문제
**문제**
```java
// 초기 설계: localStorage에 모든 토큰 저장
localStorage.setItem('accessToken', token);
localStorage.setItem('refreshToken', refreshToken);
// 문제점: XSS 공격 시 모든 토큰 탈취 가능
```

**해결**
```java
// Access Token: sessionStorage (단기, 탭 종료 시 삭제)
sessionStorage.setItem('accessToken', token);

// Refresh Token: HTTP-only Cookie (장기, JS 접근 불가)
// 서버에서 Set-Cookie 헤더로 설정
response.setHeader("Set-Cookie", 
    "refreshToken=" + token + "; HttpOnly; Secure; SameSite=Strict");
```

**배운 점**
- 단일 저장소 사용은 보안 위험을 증가시킴
- Access Token과 Refresh Token을 분리 저장하여 위험 분산
- sessionStorage는 탭 종료 시 자동 삭제되어 단기 토큰에 적합
- HTTP-only Cookie는 JavaScript 접근이 불가하여 장기 토큰에 적합
- SameSite=Strict 설정으로 CSRF 공격 추가 방어

---

### 2. JWT 토큰 만료 처리 문제

**문제**
```java
// 기존 코드
public boolean validateToken(String token) {
    if (isTokenExpired(token)) {
        return false; // HTTP 상태 코드를 전달할 방법이 없음
    }
    return true;
}
```

**해결**
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
- 클라이언트는 정확한 에러 응답을 통해 Refresh Token을 통한 재생성 고려
- HTTP 상태 코드를 통한 명확한 에러 커뮤니케이션 중요

---

### 3. Read-only Transaction에서 수정 불가 문제

**문제**
```java
@Transactional(readOnly = true)
public Post getPost(Long id) {
    Post post = postRepository.findById(id);
    post.incrementViewCount(); // 작동하지 않음!
    return post;
}
```

**해결**
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

### 4. Timezone 정보 손실 문제

**문제**
```java
// LocalDateTime 사용 시
private LocalDateTime createdAt; // Timezone 정보 없음
// 저장: 2024-12-07T10:30:00
// 조회: 2024-12-07T10:30:00 (어느 timezone인지 불명확)
```

**해결**
```java
// Instant 사용
private Instant createdAt; // UTC 기준 저장
// 저장: 2024-12-07T01:30:00Z (UTC)
// 조회: 2024-12-07T01:30:00Z (명확한 UTC 시간)
```

**배운 점**
- LocalDateTime은 timezone 정보가 없어 UTC 저장 시 정보 손실 발생
- Instant는 UTC 기준 시간을 저장하여 timezone 정보 보존
  
---

## 💭 프로젝트 회고

### 기술적 도전: 보안과 확장성을 고려한 설계

Spring Boot 기반의 백엔드 시스템을 구축하면서 보안, 성능, 확장성을 모두 고려한 아키텍처 설계의 중요성을 깊이 이해하게 되었습니다.

- **JWT 이중 토큰 전략**: Access Token과 Refresh Token을 분리 저장하여 XSS와 CSRF 공격을 동시에 방어하는 전략을 수립했습니다. 특히 sessionStorage와 HTTP-only Cookie를 조합하여 각 토큰의 특성에 맞는 최적의 저장소를 선택하는 과정에서 보안 설계의 중요성을 체감했습니다.

- **컨테이너 환경의 무상태성**: ECS Fargate에 배포하면서 세션 대신 JWT 토큰 기반 인증이 컨테이너 환경에 얼마나 적합한지 이해하게 되었습니다. 각 Task가 독립적으로 요청을 처리할 수 있어 수평 확장이 용이했습니다.

### 인프라 구축: 서버리스 컨테이너(ECS Fargate)

- **서버 관리 부담 경감**: Fargate를 사용함으로써 EC2 인스턴스 프로비저닝, OS 패치, 클러스터 용량 관리 등의 인프라 관리 부담을 크게 줄일 수 있었습니다.

- **탄력적인 확장성**: ALB, Fargate, RDS를 연결하고 보안 그룹을 설정하는 과정에서 로드 밸런서 → Task → RDS로 이어지는 트래픽 흐름과 보안에 대한 이해도가 크게 향상되었습니다. Auto Scaling 정책에 따라 Task가 자동으로 증가하거나 감소하는 과정을 통해 클라우드 네이티브 아키텍처의 탄력성을 체감했습니다.

- **안전한 운영 환경**: Private Subnet에 배포된 Task에 Systems Manager Session Manager로 안전하게 접근하는 방법을 익히면서, Bastion Host 없이도 VPC 내부 컨테이너를 안전하게 운영할 수 있게 되었습니다.

### 성능 최적화: 자원 효율 극대화

- **자원 할당 최적화**: ECS Task Definition에서 CPU와 메모리를 Task 단위로 설정하고, CloudWatch를 통해 모니터링하면서 컨테이너화된 애플리케이션의 자원 효율을 높이는 방법을 경험했습니다.

- **비동기 처리 전략**: 조회수를 비동기로 처리하여 메인 트랜잭션의 부하를 줄이는 등 작은 최적화들이 모여 큰 성능 개선을 만들어낸다는 것을 경험했습니다. 이는 Task당 처리량을 높이는 핵심 요소였습니다.

### 앞으로의 계획

이번 프로젝트를 통해 Spring Boot와 ECS Fargate 기반의 클라우드 배포 기초를 탄탄히 다졌다고 생각합니다.

다음 프로젝트에서는 컨테이너 환경을 더욱 고도화하여:

**관찰 가능성 강화**
- Prometheus로 컨테이너 메트릭(CPU, 메모리, 요청 수) 수집
- Grafana로 실시간 대시보드 구축
- CloudWatch Logs와 통합하여 중앙 집중식 로깅 시스템 구축

**성능 개선**
- ElastiCache for Redis로 조회 성능 대폭 향상
- CDN(CloudFront) 도입으로 정적 콘텐츠 배포 최적화

**아키텍처 진화**
- MSA로 전환하여 서비스별 독립 배포 가능한 구조 구축
- API Gateway와 Lambda를 활용한 이벤트 기반 아키텍처 도입

궁극적으로는 100만 MAU를 넘어 1억 MAU를 처리할 수 있는 탄력적이고 안정적인 클라우드 네이티브 시스템을 만들어가고 싶습니다.

---
