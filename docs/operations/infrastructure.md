# 인프라 명세

> **이 문서를 보면**: 우리 서비스가 **어떤 서버/네트워크/자원** 위에 구성되어 있는지 파악 가능.
>
> **언제 다시 보나요**: 인스턴스 변경 시, 보안그룹/도메인 변경 시, 새 팀원 온보딩 시.

> 🔖 **용어 주의**
> 이 문서의 "인프라"는 **배포 환경**(EC2, Docker 컨테이너, 네트워크)을 뜻합니다.
> [패키지·계층 구조 가이드](../architecture/package-structure.md)의 `infrastructure/` 폴더는 **코드 계층 이름**으로 이름만 같고 다른 개념입니다.
>
> **배포 파이프라인·Dockerfile·GitHub Actions·롤백**은 [배포 파이프라인](./deployment.md) 참고.

---

## 한 줄 요약

**AWS EC2 1대**에 **Docker**로 Spring Boot / PostgreSQL / Redis 컨테이너가 함께 올라간다. Gemini는 외부 API로 호출.

---

## 목차

- [1. 배포 토폴로지](#1-배포-토폴로지)
- [2. EC2 스펙](#2-ec2-스펙)
- [3. 네트워크 / 보안](#3-네트워크--보안)
- [4. 컨테이너 구성](#4-컨테이너-구성)
- [5. 도메인 / HTTPS](#5-도메인--https)
- [6. 외부 API 연결 지점](#6-외부-api-연결-지점)
- [7. 팀 결정 체크리스트](#7-팀-결정-체크리스트)

---

## 1. 배포 토폴로지

![배포 구조 개요](../assets/infrastructure.webp)

> 위는 개요도입니다. 실제 내부 구성은 아래 "EC2 내부 구성 + 트래픽 흐름"을 따릅니다.

### EC2 내부 구성 + 트래픽 흐름

```
 [사용자]
    ↓  HTTP/HTTPS
 [도메인/IP]              ← TBD: 도메인 도입 여부
    ↓
┌─── EC2 (단일 인스턴스, ap-northeast-2) ───────────────────┐
│ 보안그룹: 인바운드 80 / 443 / 22(관리자 IP)                 │
│                                                             │
│    Nginx (80/443)        ← TBD: 도입 여부                   │
│       │                                                     │
│       ▼                                                     │
│    Spring Boot (8080) ─────────────────→ Gemini API         │
│       │         │                        (외부 API)         │
│       ▼         ▼                                           │
│    Postgres   Redis      ← 외부 포트 노출 X, 내부 통신만    │
│    (5432)     (6379)                                        │
└─────────────────────────────────────────────────────────────┘
```

### 구조 요약

- **운영**: 단일 EC2에 Spring Boot + PostgreSQL + Redis 컨테이너 함께 기동
- **로컬 개발**: PostgreSQL / Redis만 Docker, Spring Boot는 호스트에서 `bootRun`

---

## 2. EC2 스펙

| 항목 | 값 |
|---|---|
| 인스턴스 타입 | **TBD** (t3.small 예상) |
| 리전 | ap-northeast-2 (서울) |
| OS | Ubuntu 22.04 LTS |
| 볼륨 | **TBD** (gp3 30GB 예상) |
| 개수 | 1대 (모놀리스) |
| DB 분리 | **TBD** — 현재는 EC2 내부 Docker, 확장 시 RDS로 분리 가능 |

---

## 3. 네트워크 / 보안

### 보안그룹 (인바운드 규칙)

| 포트 | 허용 범위 | 용도 |
|---|---|---|
| 22 | 관리자 IP만 | SSH 접근 |
| 80 | 0.0.0.0/0 | HTTP (HTTPS 도입 시 리다이렉트용) |
| 443 | 0.0.0.0/0 | HTTPS (TBD) |

- **PostgreSQL(5432), Redis(6379)**: 외부 노출 X, Docker 네트워크 내부 통신만 허용

### SSH 접근

- EC2 키페어(`*.pem`) 기반
- 키 파일은 팀원 개별 관리, **Git 커밋 금지**

### 민감 정보 (운영 환경변수)

`DB_PASSWORD`, `JWT_SECRET`, `OPENAI_API_KEY`, `REDIS_PASSWORD`는 **환경 변수로만** 주입.
- 코드/Git 하드코딩 금지
- 구체적 전달 경로(GitHub Secrets → EC2)는 [배포 파이프라인 #Secrets 관리](./deployment.md#4-secrets-관리) 참고

---

## 4. 컨테이너 구성

현재 `docker-compose.yml`에 정의된 서비스는 **PostgreSQL / Redis 2개**.
Backend(Spring Boot) 컨테이너는 운영 배포 시 추가 예정 — [배포 파이프라인](./deployment.md) 참고.

| 컨테이너 | 이미지 | 포트 | 볼륨 |
|---|---|---|---|
| postgres | `postgres:16-alpine` | 5432 | `./docker-data/postgres` |
| redis | `redis:7-alpine` | 6379 | `./docker-data/redis` |
| (예정) backend | `delivery:<tag>` | 8080 | — |

---

## 5. 도메인 / HTTPS

**TBD** — 팀 결정 선택지:

| 선택지 | 설명 | 비용 | 복잡도 |
|---|---|---|---|
| A. 도입하지 않음 | EC2 퍼블릭 IP 직통 | 0 | 낮음 |
| B. Nginx + Let's Encrypt | 무료 SSL, 간단 | 0 | 중간 |
| C. Route53 + ACM + ALB | 완전 자동화 | 월 ~$20 | 높음 |

---

## 6. 외부 API 연결 지점

### Gemini API

- **연결 위치**: 코드 상 `ai/infrastructure/external/gemini/GeminiClient` ([패키지·계층 구조 가이드](../architecture/package-structure.md) 참고)
- **네트워크**: EC2 → 인터넷 → Gemini API (아웃바운드 허용)
- **키 관리**: `OPENAI_API_KEY` 환경변수
- **운영 동작**(timeout / 에러 처리 / 입력 한도 등): [배포 파이프라인 #외부 API 운영 동작](./deployment.md#6-외부-api-운영-동작) 참고

---

## 7. 팀 결정 체크리스트

회의 전 각자 의견 정리 필요. 이 항목들이 확정되어야 위 `TBD`가 채워짐.

- [ ] EC2 인스턴스 타입 + 월 예산
- [ ] DB 위치 — EC2 내부 Docker 유지 vs RDS 분리
- [ ] Nginx 도입 여부
- [ ] 도메인/HTTPS 전략 (A / B / C)
- [ ] SSH 키 관리 방식 (팀원 개별? 공용?)

---

## 관련 문서

- [배포 파이프라인](./deployment.md) — CI/CD, Dockerfile, 롤백, 외부 API 운영 동작
- [패키지·계층 구조 가이드](../architecture/package-structure.md) — 코드 계층 구조 (`infrastructure/` 계층)
- [프로젝트 개요](../overview.md)
