# 인프라 명세

> **이 문서를 보면**: 현재 운영 테스트 환경이 어떤 서버/네트워크/컨테이너 구조로 구성되어 있는지 파악할 수 있습니다.
>
> **언제 다시 보나요**: 인스턴스 변경 시, 보안그룹 수정 시, 배포 구조를 새 팀원에게 설명할 때.

> 🔖 배포 워크플로우, Dockerfile, GitHub Actions, Secrets, 롤백은 [배포 파이프라인](./deployment.md) 참고.

---

## 한 줄 요약

현재 운영 테스트 환경은 **AWS EC2 1대** 위에서 **Nginx + Docker(app / PostgreSQL / Redis)** 구조로 동작하며, 외부 AI 연동은 **OpenAI API**를 사용합니다.

---

## 목차

- [1. 배포 토폴로지](#1-배포-토폴로지)
- [2. EC2 구성](#2-ec2-구성)
- [3. 네트워크 / 보안](#3-네트워크--보안)
- [4. 컨테이너 구성](#4-컨테이너-구성)
- [5. 도메인 / HTTPS](#5-도메인--https)
- [6. 외부 API 연결 지점](#6-외부-api-연결-지점)
- [7. 현재 한계와 개선 포인트](#7-현재-한계와-개선-포인트)

---

## 1. 배포 토폴로지

![배포 구조 개요](../assets/infrastructure.webp)

> 위 이미지는 개요도입니다. 실제 운영 테스트 환경은 아래 텍스트 구조를 기준으로 이해하면 됩니다.

### 현재 구조

```text
[사용자]
   |
   v   HTTP(80)
[EC2 Public IP]
   |
   v
Nginx (host)
   |
   v   proxy_pass http://127.0.0.1:8080
Spring Boot app container
   | \
   |  \--> Redis container
   |
   +----> PostgreSQL container

Spring Boot app -> OpenAI API
GitHub Actions -> GHCR push -> EC2 SSH deploy
EC2 -> GHCR pull
```

즉:
- 외부 요청은 **Nginx**가 받음
- 앱은 **Docker 컨테이너**로 동작
- DB와 Redis도 같은 EC2 내부 Docker 네트워크에서 운영

---

## 2. EC2 구성

현재 기준:

| 항목 | 내용 |
|---|---|
| 리전 | ap-northeast-2 (서울) |
| OS | Ubuntu 22.04 |
| 서버 수 | 1대 |
| 역할 | Nginx 호스트 + Docker 실행 노드 |
| 배포 경로 | `/home/ubuntu/app` |

이 문서는 **단일 EC2 기준 운영 테스트 환경**을 설명합니다.  
장기적으로는 RDS 분리, 다중 서버, 오토스케일링 등으로 확장할 수 있지만 현재는 범위 밖입니다.

---

## 3. 네트워크 / 보안

### 보안그룹

현재 운영 테스트 기준:

| 포트 | 용도 | 상태 |
|---|---|---|
| 22 | SSH 배포 / 서버 접근 | 사용 중 |
| 80 | HTTP | 사용 중 |
| 443 | HTTPS | 예정 |
| 5432 | PostgreSQL | 외부 미노출 |
| 6379 | Redis | 외부 미노출 |

설명:
- PostgreSQL, Redis는 **외부에서 직접 접근하지 않음**
- 앱도 외부에 직접 노출하지 않고 `127.0.0.1:8080`만 사용
- 외부 노출은 Nginx가 담당

### 민감 정보 관리

운영 비밀값은 코드에 두지 않고 환경변수로 관리합니다.

대표 값:
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `OPENAI_API_KEY`

실제 전달 경로는 [배포 파이프라인](./deployment.md#4-secrets-및-환경-변수) 참고.

---

## 4. 컨테이너 구성

현재 구성:

| 컨테이너 | 역할 | 외부 노출 |
|---|---|---|
| `delivery-app` | Spring Boot API 서버 | `127.0.0.1:8080` |
| `delivery-postgres` | PostgreSQL | 없음 |
| `delivery-redis` | Redis | 없음 |

### 특징

- `delivery-app`
  - `SPRING_PROFILES_ACTIVE=prod`
  - Nginx가 `127.0.0.1:8080`으로 프록시
- `delivery-postgres`
  - Docker volume으로 데이터 유지
- `delivery-redis`
  - 비밀번호 기반 실행

즉 **app / db / cache가 모두 같은 EC2 안에서 Docker로 함께 동작**하는 구조입니다.

---

## 5. 도메인 / HTTPS

### 현재 상태

- 외부 접근은 **HTTP(80)** 기준으로 동작
- Swagger 예시:
  - `http://<EC2-IP>/swagger-ui.html`

### 예정 상태

- HTTPS(443)는 아직 미적용
- 추후 필요 작업:
  - 도메인 연결
  - SSL 인증서 발급
  - Nginx `443 ssl` 설정
  - `80 -> 443` 리다이렉트

즉 현재는 **HTTP 기반 운영 테스트 환경**, HTTPS는 **예정**입니다.

---

## 6. 외부 API 연결 지점

현재 외부 AI 연동은 **OpenAI API** 기준으로 동작합니다.

운영 방식:
- EC2의 앱 컨테이너가 인터넷 아웃바운드로 OpenAI API 호출
- API 키는 `OPENAI_API_KEY` 환경변수 사용

---

## 7. 현재 한계와 개선 포인트

현재 구조의 한계:
- 단일 EC2 구조라 이중화 없음
- SSH 기반 배포라 보안그룹 / 키 관리 부담이 있음
- PostgreSQL이 EC2 내부 컨테이너라 장애 시 영향 범위가 큼
- HTTPS 미적용

향후 개선 후보:
- HTTPS 적용
- DB 마이그레이션 도구 도입
- RDS 분리 검토
- SSH 대신 SSM / self-hosted runner 검토
- 로그/모니터링 체계 강화

---

## 관련 문서

- [배포 파이프라인](./deployment.md)
- [프로젝트 개요](../overview.md)
