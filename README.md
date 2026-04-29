# 🛵 맛집배달해조

> **광화문 지역 밀착형 배달 주문 관리 플랫폼**

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini_API-8E75B2?style=for-the-badge&logo=google&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white)

---

## 프로젝트 목적 및 상세

본 프로젝트는 실질적인 배달 서비스의 전체 라이프사이클을 백엔드 관점에서 구현하며, 단순 기능 구현을 넘어 **다음 단계의 MSA 전환을 미리 준비하는 도메인 중심 설계**를 지향합니다.

###  핵심 가치

#### 1. 다층 보안 — JWT 의 즉시 무효화 메커니즘
- **JWT + DB tokenVersion 매 요청 비교** — 발제의 핵심 요구사항인 "역할 강등 / 비밀번호 변경 / 탈퇴 / 로그아웃 시 즉시 무효화"를 정확히 충족
- **빠뜨릴 수 없는 구조** — `incrementTokenVersion()` 호출을 도메인 메서드(`changeRole`, `changePassword`, `withdraw`, `forceLogout`) 안에 캡슐화하여 서비스 레이어 실수에 의존하지 않음
- **권한 매트릭스** — MASTER/MANAGER/OWNER/CUSTOMER 4단계 RBAC, MASTER self-demotion 차단, MANAGER 의 MANAGER/MASTER 변경 불가

#### 2. MSA 전환 대비 — 느슨한 결합 도메인 설계
- **Package by Feature** — 10개 도메인(user · auth · address · region · store · product · order · payment · review · ai) × 4계층(presentation / application / domain / infrastructure) 일관 적용
- **도메인 간 직접 import 0건** — `UserRole` enum 만 공유, 그 외엔 ID 기반 FK(UUID/Long) 참조로 격리
- **결과: JPA 관계 매핑 1건만**(Order ↔ OrderItem) — N+1 함정 자체를 회피하고 `findAllByIdIn` batch 쿼리 패턴으로 일관

#### 3. AI 기반 상품 설명 자동화
- **LLM 연동** — 사장님이 상품 등록 시 프롬프트 기반으로 자동 설명 생성, 운영 번거로움 최소화
- **호출 이력 관리** — `LlmCall` 엔티티로 모든 호출 audit (요청 스냅샷, 응답, 비용 추적 가능)
- **수동/AI 출처 구분** — `Product.descriptionSource` 로 사장님이 직접 작성한 설명과 AI 생성 설명을 명시적으로 구분

#### 4. 정교한 주문 / 배송지 / 결제 워크플로우
- **지역 기반 배달 검증** — `Region` depth 3(동 단위)으로 가게-사용자 지역 일치 여부 사전 검증
- **5분 취소 제한 타이머** — 주문 생성 시 `cancelDeadlineAt` 설정, 그 이후엔 사장님 권한으로만 처리
- **주문 상태 전이 검증** — PENDING → ACCEPTED → COOKING → DELIVERING → DELIVERED → COMPLETED 흐름을 도메인 레이어에서 강제

#### 5. 표준화된 시스템 인프라
- **공통 응답 포맷** (`ApiResponse<T>`) + **글로벌 예외 처리** + **도메인별 ErrorCode 체계** — 일관된 API 경험
- **컨벤션 정착 문화** — 사고 발생 시 단발 핫픽스가 아닌 팀 룰로 정착 (ex. `@PreAuthorize` 필수 규칙, PR 리뷰 체크리스트)
- **e2e 테스트 자산** — Postman 통합 컬렉션으로 회원가입 → 주문 → 결제 → 리뷰 풀 시나리오 공유

---

## 팀원 역할분담

| 성명 | 담당 도메인 | 주요 업무 |
|---|---|---|
| **이한나** *(팀장)* | Common Infra & **User / Auth** | 공통 응답·예외·감사(audit) 전략 / 글로벌 에러 처리 / JWT 인증·인가, RBAC, tokenVersion 다층 보안 |
| **신여원** | **Payment / Review** | 가상 결제 흐름, 결제 상태 전이 / 리뷰 작성·평점 집계, 주문 완료(`COMPLETED`) 검증 |
| **이재범** | **Product / AI** | 상품 CRUD, 숨김·품절 상태 관리 / LLM 연동, 프롬프트 기반 상품 설명 자동 생성, 호출 이력 관리 |
| **임광조** | **Store / Region** | 가게 CRUD 및 카테고리 구조 / 지역 계층(시·도 → 구·군 → 동) 관리, 가게 등록 가능 단위(depth 3) 검증 |
| **김재중** | **Order / Address** | 주문 생성·상태 전이 / 5분 취소 타이머 / 사용자 배송지 CRUD 및 기본 배송지 관리 |

---

## 서비스 구성 및 실행 방법

본 프로젝트는 운영에 필요한 인프라(DB, Cache)를 **Docker Compose** 로 컨테이너화하여 관리합니다.

### 시스템 아키텍처

- **Database**: PostgreSQL 16
- **Cache & Auth Store**: Redis 7
- **Application**: Spring Boot 3.x (Java 17)
- **Deploy**: AWS EC2 + GitHub Container Registry (GHCR)

### 1. 레포지토리 클론

```bash
git clone [레포지토리 URL]
cd [프로젝트 폴더명]
```

### 2. 환경 변수 설정

`.env` 파일을 프로젝트 루트에 생성합니다.

```env
POSTGRES_USER=delivery
POSTGRES_PASSWORD=delivery
POSTGRES_DB=delivery
REDIS_PASSWORD=redis-password
JWT_SECRET=[충분히-긴-랜덤-문자열]
JWT_EXPIRATION=3600000
```

### 3. 인프라 컨테이너 실행

PostgreSQL 과 Redis 를 백그라운드에서 실행합니다.

```bash
docker compose up -d
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

> 로컬 프로필(`local`)에서 실행 시 `TestFixtureSeeder` 가 테스트 계정 7개를 자동 생성합니다.
> (master · manager · owner1 · owner2 · alice · bob · charlie / 공통 비밀번호: `Pass1234!`)

### ⚙ 포트 설정

| 서비스 | 포트 | 비고 |
|---|---|---|
| Spring Boot | `8080` | API Main Server |
| PostgreSQL | `5432` | Main Database |
| Redis | `6379` | Auth/Cache Server |
| Swagger UI | `/swagger-ui/index.html` | API 문서 |

---

## 기술 스택

| 카테고리 | 기술 |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.x |
| **Security** | Spring Security, JWT (`jjwt` 0.12), BCrypt |
| **ORM** | Spring Data JPA, Hibernate |
| **Database** | PostgreSQL 16 |
| **Cache** | Redis 7 |
| **Validation** | Spring Validation (Jakarta) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Build** | Gradle 8.x |
| **Test** | JUnit 5, Mockito, Testcontainers (PostgreSQL), H2 |
| **Container** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions, GHCR (이미지 레지스트리) |
| **Cloud** | AWS EC2 |
| **AI** | LLM API 연동 (OpenAI / Anthropic 등) |
| **Collaboration** | GitHub Issue/PR Template, Postman Collection |

---

## ERD

📌 [ERD 다이어그램 보기](docs/design/erd.md)

> 도메인별 테이블 구조와 관계는 `docs/design/` 참고.
> 핵심: User(Long PK)가 의존성 루트, 그 외 도메인은 UUID PK + ID 기반 FK 참조.

---

## API 명세서

📖 **Swagger UI**: <http://15.164.50.29/swagger-ui/index.html#/>

운영 환경 API 문서를 실시간으로 확인할 수 있습니다.
로컬 환경에선 앱 실행 후 <http://localhost:8080/swagger-ui/index.html> 로 접근 가능.

---

## 문서

추가 문서는 `docs/` 디렉토리 참고:

- [📐 architecture/](docs/architecture/) — 패키지 구조, 의존성 규칙
- [📋 conventions/](docs/conventions/) — 코드/Git/PR 컨벤션
- [🎨 design/](docs/design/) — 도메인 설계, ERD
- [⚙ operations/](docs/operations/) — 인프라, 배포 가이드
- [🔍 troubleshooting/](docs/troubleshooting/) — 트러블슈팅 사례
- [📝 adr/](docs/adr/) — 주요 의사결정 기록

---

## 라이선스

내일배움캠프 팀 프로젝트 — 학습 목적
