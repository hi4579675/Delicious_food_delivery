# 🛵 맛집배달해조

> **광화문 지역 밀착형 배달 주문 관리 플랫폼**

---

##  프로젝트 소개

**맛집배달해조**는 광화문 지역을 기반으로 운영되는 음식 주문 관리 플랫폼입니다.  
고객, 사장님, 관리자의 역할에 맞춘 기능을 제공하며,  
배달 서비스의 전체 라이프사이클을 백엔드 관점에서 구현하는 것을 목표로 합니다.

단순한 기능 구현을 넘어 **확장 가능한 아키텍처**와 **팀 협업 표준화**를 지향합니다.


![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini_API-8E75B2?style=for-the-badge&logo=google&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white)


---

##  핵심 가치


---

## 👥 팀원 역할 분담



---


##  서비스 실행 방법

###  사전 요구사항
-  Java 17 이상
-  Docker & Docker Compose
-  Gradle 8.x

### 1️⃣ 레포지토리 클론
```bash
git clone https://github.com/hi4579675/Delicious_food_delivery.git
cd Delicious_food_delivery
```

### 2️⃣ 환경 변수 설정
```bash
cp .env.example .env
# .env 파일 열어서 비밀번호 등 원하는 값으로 수정
```

### 3️⃣ 앱 실행
```bash
./gradlew bootRun
```

> 💡 `spring-boot-docker-compose` 의존성이 **Docker 컨테이너(Postgres, Redis)를 자동으로 실행**해줍니다.
> 별도로 `docker compose up` 명령을 칠 필요 없어요.

### 접속 확인
- 애플리케이션: http://localhost:8080


---

##  아키텍처

**Layered Architecture + Package by Feature**

도메인별로 최상위 패키지를 분리하고, 각 도메인 내부는 4계층으로 구성합니다.

```
{도메인}/
├── presentation/     → 컨트롤러 + DTO
├── application/      → 서비스 (유스케이스)
├── domain/           → 엔티티 + Repository 인터페이스 + 도메인 예외
└── infrastructure/   → Repository 구현체, 외부 API 클라이언트
```

> ⚠️ 정통 DDD/헥사고날 **적용 X**. 튜터 가이드의 Layered Architecture를 따르며, 도메인 폴더링으로 MSA 전환 가능성을 열어둔 구조.

##  문서

- [📖 상세 아키텍처 가이드](./docs/architecture.md) — 계층별 역할, 코드 배치 규칙, 도메인별 설명
- [📋 팀 협업 컨벤션](./docs/conventions.md) — 브랜치 / 커밋 / PR / 이슈 규칙

