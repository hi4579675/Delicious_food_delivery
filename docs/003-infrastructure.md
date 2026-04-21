# 003. 인프라 명세

> **이 문서를 보면**: 어디에 어떻게 배포되는지, 어떤 기술 스택/환경변수가 필요한지 파악 가능.
>
> **언제 다시 보나요**: 배포 시, 환경변수 추가할 때, 신규 인프라 요소 도입 시.

---

## 한 줄 요약

**GitHub Actions**로 빌드하여 **AWS EC2** 위 **Docker**로 배포. EC2 안에 **Spring Boot / PostgreSQL / Redis** 세 컨테이너가 함께 올라간다.

---

## 목차

- [1. 배포 아키텍처](#1-배포-아키텍처)
- [2. 기술 스택](#2-기술-스택)
- [3. 환경별 Profile](#3-환경별-profile)
- [4. 환경 변수](#4-환경-변수)
- [5. 보안 설정](#5-보안-설정)
- [6. CI/CD 파이프라인](#6-cicd-파이프라인)

---

## 1. 배포 아키텍처

![인프라 아키텍처](./assets/infrastructure.webp)

### 구성 요소

| 구성 | 역할 |
|------|------|
| GitHub Repository | 소스 코드 저장 |
| GitHub Actions | CI/CD 파이프라인 (빌드 → 배포) |
| AWS EC2 | Docker 호스트 |
| Backend API Server (Docker) | Spring Boot 애플리케이션 |
| PostgreSQL (Docker) | 주 데이터베이스 |
| Redis (Docker) | 캐시 / 세션 스토어 |

### 구조 요약

- 운영 환경은 단일 EC2 인스턴스에 **Spring Boot + PostgreSQL + Redis** 컨테이너가 함께 올라감
- **GitHub Actions**가 빌드 후 EC2로 배포
- 로컬 개발은 PostgreSQL / Redis만 Docker로 기동, Spring Boot는 IntelliJ/`bootRun`으로 실행 (`spring-boot-docker-compose`가 `docker-compose.yml`을 자동 기동)

---

## 2. 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| Language | Java 17 | |
| Framework | Spring Boot 3.5.13 | |
| Security | Spring Security + JWT (`jjwt 0.11.5`) | HS256, 매 요청 DB 권한 재검증 |
| ORM | JPA / Hibernate | `ddl-auto`: local=`create-drop`, prod=`validate` |
| DB | PostgreSQL 16 (alpine) | Docker 컨테이너 |
| Cache / Session | Redis 7 (alpine) | Docker 컨테이너 |
| Build | Gradle 9.4.1 | |
| API 문서 | `springdoc-openapi 2.8.15` (Swagger UI) | `/swagger-ui.html` |
| AI | Google Gemini (WebClient / WebFlux) | REST 호출 |
| Monitoring | Spring Boot Actuator | `health`, `info` (prod) |
| AOP | Spring Boot Starter AOP | |
| Test | JUnit 5, Spring Security Test, Testcontainers, H2 (runtime) | |
| Local Dev | `spring-boot-docker-compose` | `bootRun` 시 `docker-compose.yml` 자동 기동 |
| 컨테이너 | Docker / docker-compose | |
| CI/CD | GitHub Actions | **미구현** — `.github/workflows/` 추가 예정 |
| 서버 | AWS EC2 | |

---

## 3. 환경별 Profile

현재 `application.yml`에 정의된 프로필은 **`local`, `prod`** 두 가지.

| 프로필 | DB | Redis | 실행 방식 |
|--------|-----|-------|----------|
| `local` (기본값) | PostgreSQL (Docker, localhost:5432) | Redis (Docker, localhost:6379) | `./gradlew bootRun` — `spring-boot-docker-compose`가 Postgres/Redis 컨테이너 자동 기동 |
| `prod` | PostgreSQL (환경변수 주입) | Redis (환경변수 주입) | EC2에서 Docker로 실행, `SPRING_PROFILES_ACTIVE=prod` |

> 테스트는 별도 프로필 없이 `runtimeOnly` H2 + Testcontainers 조합으로 수행.

### Profile 활성화

```bash
# 로컬 (기본값)
./gradlew bootRun

# 운영 (EC2)
SPRING_PROFILES_ACTIVE=prod
```

---

## 4. 환경 변수

### 4-1. 운영 필수 환경 변수

| 변수명 | 설명 |
|--------|------|
| `SPRING_PROFILES_ACTIVE` | 프로필 선택 (`prod`) |
| `DB_HOST` | PostgreSQL 컨테이너 호스트명 |
| `DB_PORT` | PostgreSQL 포트 (기본 5432) |
| `DB_NAME` | DB 이름 |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 컨테이너 호스트명 |
| `REDIS_PORT` | Redis 포트 (기본 6379) |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 (HS256, 256bit 이상) |
| `GEMINI_API_KEY` | Google AI Studio 발급 API 키 |

### 4-2. 로컬 개발 (`.env`)

`.env` 파일로 Docker Compose + 애플리케이션 변수 관리. 실제 `.env.example`:

```env
# DB
POSTGRES_DB=delivery
POSTGRES_USER=delivery
POSTGRES_PASSWORD=changeme

# Redis
REDIS_PASSWORD=changeme

# JWT
JWT_SECRET=your-256-bit-secret-key-here

# Gemini AI
GEMINI_API_KEY=your-gemini-api-key
```

> `.env`는 **Git에 커밋 금지** (`.gitignore` 포함). 템플릿은 `.env.example` 참고.

---

## 5. 보안 설정

### 인증 / 인가

- JWT Access Token 헤더: `Authorization: Bearer {token}`
- 서명 알고리즘: **HS256** (`jjwt 0.11.5`)
- 토큰 만료: 1시간 (`jwt.expiration=3600000`ms)
- 비밀번호 저장: **BCrypt** 해시
- 매 요청 시 JWT payload의 `role`과 DB의 현재 `role` 비교
- Redis 캐싱으로 권한 재검증 부하 완화 (도입 여부 추후 결정)

### 민감 정보 관리

- `GEMINI_API_KEY`, `DB_PASSWORD`, `JWT_SECRET`, `REDIS_PASSWORD`는 **환경 변수로만** 관리
- 코드/Git에 하드코딩 금지
- `.env.example`에는 변수명과 더미 값만 공개

---

## 6. CI/CD 파이프라인

GitHub Actions로 브랜치 푸시 시 빌드·배포가 이루어지도록 구성할 예정.

> **현재 상태**: `.github/workflows/` 디렉토리 **미구현**. 워크플로우 정의와 EC2 배포 스크립트, Docker 이미지 빌드 (Dockerfile) 모두 추후 작성.
>
> Backend Docker 이미지 빌드용 `Dockerfile` 및 운영 배포용 `docker-compose` 구성도 함께 추가 필요.

---

## 관련 문서

- [000. 프로젝트 개요](./000-overview.md)
- [002. 데이터 명세](./002-data-spec.md)
- [006. 팀 협업 컨벤션](./006-conventions.md)
