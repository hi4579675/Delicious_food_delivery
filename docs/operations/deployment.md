# 배포 파이프라인

> **이 문서를 보면**: 코드가 어떻게 서버까지 올라가는지, 배포 중 장애 시 어떻게 복구하는지, 외부 API는 어떻게 동작하는지 파악 가능.
>
> **언제 다시 보나요**: CI/CD 수정 시, 배포 장애 발생 시, 외부 API 정책 변경 시.

> 🔖 인프라 구성(EC2 스펙, 네트워크, 보안그룹 등)은 [인프라 명세](./infrastructure.md)를 먼저 확인.

---

## 한 줄 요약

**GitHub Actions**가 main 브랜치 push를 감지하여 빌드·테스트 후 **SSH로 EC2 접속 → Docker 재기동**. 롤백은 수동 재배포.

---

## 목차

- [1. 파이프라인 흐름](#1-파이프라인-흐름)
- [2. GitHub Actions 워크플로우](#2-github-actions-워크플로우)
- [3. Dockerfile / deploy.sh](#3-dockerfile--deploysh)
- [4. Secrets 관리](#4-secrets-관리)
- [5. 롤백 절차](#5-롤백-절차)
- [6. 외부 API 운영 동작](#6-외부-api-운영-동작)
- [7. 운영 (헬스체크 / 로그)](#7-운영-헬스체크--로그)
- [8. 팀 결정 체크리스트](#8-팀-결정-체크리스트)

---

## 1. 파이프라인 흐름

> **현재 상태**: `.github/workflows/` 디렉토리 **미구현**. 아래는 예정 구조.

```
 개발자 push (main 또는 TBD)
         │
         ▼
 GitHub Actions 트리거
         │
 ┌───────┼────────────────────────────────┐
 │  [1] Build     ./gradlew build          │
 │  [2] Test      ./gradlew test           │
 │  [3] Docker    docker build + tag       │
 │  [4] Push      이미지 레지스트리으로     │  ← TBD
 │  [5] SSH       EC2 접속                 │
 │  [6] Deploy    docker compose pull      │
 │                docker compose up -d     │
 │  [7] Health    /actuator/health 확인    │
 └──────────────────────────────────────────┘
```

---

## 2. GitHub Actions 워크플로우

`.github/workflows/deploy.yml` 예정 초안:

```yaml
name: Deploy to EC2

on:
  push:
    branches: [main]   # TBD: 팀 확정

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build & Test
        run: ./gradlew build

      - name: Build Docker image
        run: docker build -t delivery:${{ github.sha }} .

      - name: Push image          # TBD: GHCR / Docker Hub
        run: echo "TBD"

      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ubuntu
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd /home/ubuntu/app
            docker compose pull
            docker compose up -d
```

### 주요 결정 사항 (TBD)

| 항목 | 선택지 |
|---|---|
| 트리거 브랜치 | `main` push / PR merge / tag push |
| 이미지 레지스트리 | GHCR / Docker Hub / ECR |
| 배포 스크립트 위치 | EC2 내 `deploy.sh` / Actions 인라인 |

---

## 3. Dockerfile / deploy.sh

현재 **미작성**. 팀에서 작성 후 레포 루트에 포함.

### Dockerfile 초안

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/delivery-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 운영 docker-compose 추가 서비스

```yaml
# docker-compose.yml에 추가
backend:
  image: delivery:${IMAGE_TAG:-latest}
  ports:
    - "8080:8080"
  depends_on:
    postgres: { condition: service_healthy }
    redis:    { condition: service_started }
  environment:
    SPRING_PROFILES_ACTIVE: prod
    DB_HOST: postgres
    DB_PORT: 5432
    DB_NAME: ${POSTGRES_DB}
    DB_USERNAME: ${POSTGRES_USER}
    DB_PASSWORD: ${POSTGRES_PASSWORD}
    REDIS_HOST: redis
    REDIS_PASSWORD: ${REDIS_PASSWORD}
    JWT_SECRET: ${JWT_SECRET}
    OPENAI_API_KEY: ${OPENAI_API_KEY}
```

---

## 4. Secrets 관리

### GitHub Secrets → EC2 전달

```
[GitHub Secrets]
 ├── EC2_SSH_KEY, EC2_HOST           ← Actions가 SSH 접속 시 사용
 ├── DB_PASSWORD, REDIS_PASSWORD      ← .env로 EC2에 전달
 ├── JWT_SECRET                       ← 동일
 └── OPENAI_API_KEY                   ← 동일
        │
        ▼ (Actions가 SSH로 .env 업로드 또는 환경변수 주입)
[EC2]
 └── docker compose up -d (컨테이너가 환경변수 읽어 기동)
```

### 로컬 개발 (`.env`)

`.env.example`:
```env
POSTGRES_DB=delivery
POSTGRES_USER=delivery
POSTGRES_PASSWORD=changeme
REDIS_PASSWORD=changeme
JWT_SECRET=your-256-bit-secret-key-here
OPENAI_API_KEY=your-openai-api-key
```

`.env`는 Git 커밋 금지.

---

## 5. 롤백 절차

**자동화 없음.** 문제 발생 시 수동 대응:

1. 문제 커밋 식별
2. `git revert <commit>` 후 `main`에 push → 파이프라인 재실행으로 이전 상태로 복구
3. 긴급한 경우, 이미지 태그 기반이면 EC2에서 수동으로 이전 태그로 재기동:
   ```bash
   docker compose down
   IMAGE_TAG=<previous-sha> docker compose up -d
   ```

### 확장 여지 

> "현재는 git revert 기반 수동 롤백입니다. 실서비스라면 Blue-Green 배포, Actions 워크플로우에 자동 롤백 step 추가, 이미지 태그 버전 관리 정책 등을 도입할 수 있습니다."

---

## 6. 외부 API 운영 동작

### Gemini

| 항목 | 내용 |
|---|---|
| 호출 레이어 | `ai/application/AiService` → `ai/infrastructure/external/gemini/GeminiClient` |
| 클라이언트 | Spring `WebClient` (WebFlux) |
| timeout | 10초 (TBD — 팀 확정) |
| 재시도 / 폴백 | **미적용** (학습 프로젝트 범위 외) |
| 실패 처리 | 예외 발생 시 `AI-001` 에러 코드로 사용자 응답 |
| 입력 한도 | **TBD** ([도메인 명세](../design/domain.md#6-5-ai-연동)=100자 vs `application.yml`=500자 불일치, 팀 확정 필요) |
| 프롬프트 정책 | "답변을 최대한 간결하게 50자 이하로" 자동 삽입 |

### 확장 여지

> "현재는 timeout + 에러 응답만 두고 있습니다. 실서비스라면 Resilience4j로 retry/circuit breaker, Redis 캐시로 동일 요청 캐싱, Rate Limiter로 사용자별 호출 한도 적용 등을 추가할 수 있습니다."

---

## 7. 운영 (헬스체크 / 로그)

### 헬스체크

- `/actuator/health` — 컨테이너 상태 확인 (배포 후 자동 검증)
- `/actuator/info` — 애플리케이션 정보
- prod에서만 노출 (`application.yml`에서 설정)

### 로그

- **현재**: `docker logs <container>`로 확인. 중앙화 X
- **확장 시**: CloudWatch Logs / Loki / ELK 등 도입 가능

### 장애 대응

- 현재: 수동 — 로그 확인 → 재배포 또는 git revert 후 재배포
- 확장 시: Slack Webhook 알림, CloudWatch 알람 규칙 추가 가능

---

## 8. 팀 결정 체크리스트

### 파이프라인
- [ ] 배포 트리거 브랜치 (`main` push? tag?)
- [ ] 이미지 레지스트리 (GHCR / Docker Hub)
- [ ] 배포 스크립트 위치 (Actions 인라인 vs EC2 `deploy.sh`)

### 외부 API
- [ ] Gemini 입력 한도 (100 / 500 / 기타) — domain.md와 `application.yml` 통일 필수
- [ ] timeout 시간 (10초 / 기타)
- [ ] 실패 시 사용자 응답 문구

### 운영
- [ ] 모니터링 수준 (Actuator만? Slack 알림?)
- [ ] 로그 중앙화 여부 (당장은 X, 확장 시?)
- [ ] 롤백 자동화 여부 (당장은 수동)

---

## 관련 문서

- [인프라 명세](./infrastructure.md) — EC2, 네트워크, 컨테이너 구성
- [프로젝트 개요](../overview.md)
- [패키지·계층 구조 가이드](../architecture/package-structure.md)
