# 배포 파이프라인

> **이 문서를 보면**: 현재 운영 배포가 어떤 흐름으로 실행되는지, 어떤 파일과 Secret이 필요한지, 장애가 났을 때 어디를 먼저 봐야 하는지 파악할 수 있습니다.
>
> **언제 다시 보나요**: CI/CD 수정 시, 배포 실패 시, 운영 배포 절차를 새 팀원에게 설명할 때.

> 🔖 인프라 구성(EC2, 네트워크, 보안그룹, 컨테이너 배치)은 [인프라 명세](./infrastructure.md)를 먼저 확인하세요.

---

## 한 줄 요약

`main` 브랜치에 push가 발생하면 **GitHub Actions CD**가 테스트를 수행하고 Docker 이미지를 **GHCR**에 업로드한 뒤, **EC2에 SSH 접속하여 `deploy.sh`를 실행**해 컨테이너를 재기동합니다.

---

## 목차

- [1. 현재 배포 흐름](#1-현재-배포-흐름)
- [2. GitHub Actions 워크플로우](#2-github-actions-워크플로우)
- [3. 배포에 사용되는 파일](#3-배포에-사용되는-파일)
- [4. Secrets 및 환경 변수](#4-secrets-및-환경-변수)
- [5. 롤백 및 장애 대응](#5-롤백-및-장애-대응)
- [6. 외부 API 운영 동작](#6-외부-api-운영-동작)
- [7. 운영 확인 포인트](#7-운영-확인-포인트)
- [8. 현재 한계와 개선 포인트](#8-현재-한계와-개선-포인트)

---

## 1. 현재 배포 흐름

### CI

- 트리거: `dev`, `main` 브랜치의 `push`, `pull_request`
- 실행 내용:
  - `./gradlew testClasses --no-daemon`
  - `./gradlew test --no-daemon`

### CD

- 트리거:
  - `main` 브랜치 `push`
  - `workflow_dispatch` 수동 실행

### 전체 흐름

```text
개발자 -> GitHub push / merge
          |
          +-> CI (dev, main)
          |     - testClasses
          |     - test
          |
          +-> CD (main only)
                1. test
                2. Docker image build
                3. GHCR push
                4. docker-compose.prod.yml / deploy.sh 를 EC2로 전송
                5. EC2에서 deploy.sh 실행
                6. app / postgres / redis 재기동
                7. /actuator/health 확인
```

---

## 2. GitHub Actions 워크플로우

### CI 워크플로우

핵심 포인트:
- `dev`, `main` 브랜치의 `push`, `pull_request`에서 실행
- Java 17 환경에서 테스트 수행
- 목적은 **머지 전 컴파일 및 테스트 자동 검증**

### CD 워크플로우

잡 구성:

1. `build-and-push`
   - 소스 checkout
   - Java 17 설정
   - `./gradlew test --no-daemon`
   - 이미지명 계산: `ghcr.io/<owner>/<repo>`
   - Docker image build
   - GHCR에 `latest`, `sha-${github.sha}` 태그 push

2. `deploy`
   - `docker-compose.prod.yml`, `scripts/deploy.sh`를 EC2로 복사
   - EC2에서 `deploy.sh` 실행
   - 실행 시 `APP_IMAGE`, `GHCR_USERNAME`, `GHCR_TOKEN` 환경변수 전달

현재 배포는 **SSH 기반**입니다. 즉 GitHub Actions가 EC2에 직접 SSH 접속하여 배포 스크립트를 실행하는 구조입니다.

---

## 3. 배포에 사용되는 파일

### 1) Dockerfile

- Java 17 멀티스테이지 빌드
- `bootJar` 생성 후 실행 이미지에 복사
- 컨테이너 내부 포트 `8080`
- 기본 실행 프로필은 `prod`

### 2) 운영용 Compose

구성 서비스:
- `app`
- `postgres`
- `redis`

주요 특징:
- 앱은 `127.0.0.1:8080:8080`으로만 바인딩
- PostgreSQL, Redis는 외부 포트 미노출
- 앱은 `APP_IMAGE` 환경변수로 전달받은 이미지를 사용

### 3) 배포 스크립트

역할:
- 필수 환경변수(`APP_IMAGE`) 검증
- `.env` 존재 여부 확인
- GHCR 로그인
- 앱 이미지 pull
- `postgres`, `redis`, `app` 순서대로 기동
- `curl http://127.0.0.1:8080/actuator/health` 헬스체크

---

## 4. Secrets 및 환경 변수

### GitHub Actions Secrets

현재 배포에 필요한 주요 Secret:

- `EC2_HOST`
- `EC2_USERNAME`
- `EC2_PORT`
- `EC2_SSH_KEY`
- `EC2_APP_DIR`
- `GHCR_USERNAME`
- `GHCR_TOKEN`

주의:
- `EC2_SSH_KEY`에는 **pem 파일 내용 전체**가 들어가야 합니다.
- 파일명이나 경로가 아니라, `BEGIN ... END`를 포함한 **개인키 본문 전체**여야 합니다.

### EC2 `.env`

운영 서버에서는 `${EC2_APP_DIR}/.env` 파일을 직접 관리합니다.

주요 값:

```env
POSTGRES_DB=delivery
POSTGRES_USER=delivery
POSTGRES_PASSWORD=changeme
REDIS_PASSWORD=changeme
JWT_SECRET=your-256-bit-secret-key-here
OPENAI_API_KEY=your-openai-api-key
```

원칙:
- `.env`는 Git에 커밋하지 않음
- 운영 비밀번호와 키는 EC2에만 보관

---

## 5. 롤백 및 장애 대응

### 현재 롤백 방식

현재는 **자동 롤백 없음**, 수동 대응 기반입니다.

가능한 대응:

1. 문제 커밋 `git revert`
2. `main`에 다시 push
3. CD를 재실행해 이전 상태로 복구

또는, 이미지 태그를 알고 있으면 수동 재배포도 가능합니다.

예:

```bash
cd /home/ubuntu/app
APP_IMAGE=ghcr.io/hi4579675/delicious_food_delivery:sha-<commit> ./scripts/deploy.sh
```

### 실제 겪은 이슈

- `EC2_SSH_KEY` Secret 값 형식 오류로 SSH 접속 실패
- 보안그룹 22 포트 정책 때문에 GitHub Actions가 EC2에 접속하지 못한 적이 있음

즉 현재 배포 구조에서 가장 먼저 볼 것은:
- GitHub Actions 로그
- Secret 값
- EC2 보안그룹

---

## 6. 외부 API 운영 동작

### OpenAI

현재 배포 관점에서 외부 AI 연동은 `OPENAI_API_KEY` 환경변수를 사용합니다.

운영 특성:
- 서버에서 외부 OpenAI API로 아웃바운드 요청
- 별도 재시도 / circuit breaker는 아직 없음
- 현재는 기본 timeout 및 예외 응답 처리 수준

---

## 7. 운영 확인 포인트

### GitHub Actions

- CI 초록불 확인
- CD의 `build-and-push`, `deploy` 잡 성공 여부 확인

### GHCR

- `ghcr.io/hi4579675/delicious_food_delivery`
- `latest`, `sha-...` 태그 push 여부 확인

### EC2

```bash
docker ps
curl http://127.0.0.1:8080/actuator/health
```

정상 기대값:
- `delivery-app`
- `delivery-postgres`
- `delivery-redis`
가 실행 중

### 외부 접근

- HTTP 기준 Swagger 확인
- 예: `http://<EC2-IP>/swagger-ui.html`

현재는 **HTTP(80)만 실제 동작 중**이며, HTTPS는 아직 적용 전입니다.

---

## 8. 현재 한계와 개선 포인트

- 배포는 아직 **SSH 기반 수동 운영 친화형 구조**
- `APP_IMAGE`를 런타임에 주입하는 방식이라 서버에서 `docker compose ps` 확인 시 별도 값이 필요할 수 있음
- 롤백 자동화 없음
- CD는 현재 `latest` 태그를 배포 기준으로 사용
- DB 마이그레이션 도구(Flyway/Liquibase) 미도입
- HTTPS 미적용

향후 개선 후보:
- `sha` 태그 기반 배포
- CD concurrency 추가
- 헬스체크 실패 시 `docker compose logs` 자동 출력
- Flyway/Liquibase 도입 후 `ddl-auto: validate` 복귀
- HTTPS 적용
- SSH 대신 SSM / self-hosted runner 검토

---

## 관련 문서

- [인프라 명세](./infrastructure.md)
- [프로젝트 개요](../overview.md)
