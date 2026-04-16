# 🛵 맛집배달해조

> **광화문 지역 밀착형 배달 주문 관리 플랫폼**

---

##  프로젝트 소개

**맛집배달하죠**는 광화문 지역을 기반으로 운영되는 음식 주문 관리 플랫폼입니다.  
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

---

##  아키텍처

**Layered Architecture + Package by Feature**

도메인별로 최상위 패키지를 분리하고, 각 도메인 내부는 4계층(presentation / application / domain / infrastructure)으로 구성합니다.

### 📌 이 구조로 작성한 의도

| 기준 | 선택 |
|------|------|
| **커리큘럼 가이드 준수** | 튜터 가이드 "Layered Architecture (Controller / Service / Repository)" 권장을 따름 |
| **Package by Feature** | 도메인별 최상위 폴더 분리 → 팀 협업 시 작업 영역 충돌 최소화 |
| **DDD 용어 차용** | `application`, `domain`, `infrastructure`, `presentation` 용어 사용 (튜터 예시와 동일) |
| **다음 프로젝트(MSA) 대비** | 도메인별 경계를 미리 연습. 향후 도메인 단위로 서비스 추출이 쉬움 |
| **복잡한 DDD 패턴 미도입** | Aggregate Root / Value Object / Domain Event / Internal API 등은 **적용 X** (가이드 준수) |
| **도메인별 예외 관리** | `{도메인}/domain/exception/`에 도메인 전용 예외, `common/exception/`에 전역 핸들러 |

> **⚠️ 우리는 정통 DDD/헥사고날 아키텍처를 적용하지 않습니다.** 튜터 가이드대로 Layered Architecture를 따르되, 도메인별 폴더링으로 MSA 전환 가능성을 열어둔 실용적 구조입니다.

###  공통 폴더 구조

```
{도메인}/
├── presentation/     → 컨트롤러 + DTO (외부 REST API)
├── application/      → 서비스 (평소 쓰던 @Service)
├── domain/
│   ├── entity/       → JPA 엔티티
│   ├── repository/   → Repository 인터페이스
│   └── exception/    → 도메인 전용 예외
└── infrastructure/
    └── persistence/
        └── repository/ → Repository 구현체 (필요 시)
```

###  계층별 역할 (팀 컨벤션)

| 계층 | 역할 | 쉽게 말하면 | 들어가는 것 |
|------|------|-------------|-------------|
| **presentation** | 외부 REST API (클라이언트 앱) | 컨트롤러 + 요청/응답 DTO | `@RestController`, `XxxRequest`, `XxxResponse` |
| **application** | 유스케이스 오케스트레이션 | 평소 쓰던 `@Service` (회원가입, 주문 같은 기능 단위) | `UserService`, `OrderService` |
| **domain** | 비즈니스 핵심 (엔티티 + 레포 인터페이스 + 예외) | `@Entity` + `Repository` 인터페이스 + 도메인 예외 | `User.java`, `UserRepository`, `UserNotFoundException` |
| **infrastructure** | 외부 시스템 연동 (DB, 외부 API) | Repository 구현체, 외부 API 클라이언트, JWT 등 | `UserRepositoryImpl`, `GeminiClient`, `JwtProvider` |

### 의존성 규칙

```
presentation → application → domain
                  ↓              ↑
              infrastructure ────┘
```

- ✅ `presentation`은 `application`을 호출
- ✅ `application`은 `domain`과 `infrastructure` 호출
- ✅ `infrastructure`는 `domain`의 인터페이스를 구현
- ❌ **`domain`은 어떤 계층도 의존하지 않음** (순수!)
- ❌ 거꾸로 호출 금지 (`domain` → `application` X)

> **쉽게**: "컨트롤러 → 서비스 → 엔티티/레포" 평소랑 똑같은데, **엔티티는 아무것도 몰라야 함.**

### 🧭 "이거 어디에 둬야 하지?" 치트시트

| 만들 것 | 위치 |
|---------|------|
| REST API 메서드 | `{도메인}/presentation/` |
| Request/Response DTO | `{도메인}/presentation/dto/` |
| 평소 쓰던 `@Service` | `{도메인}/application/` |
| `@Entity` | `{도메인}/domain/entity/` |
| `JpaRepository` 인터페이스 | `{도메인}/domain/repository/` |
| 도메인 전용 예외 | `{도메인}/domain/exception/` |
| Repository 구현체 (커스텀 쿼리) | `{도메인}/infrastructure/persistence/repository/` |
| 외부 API 호출 클라이언트 | `{도메인}/infrastructure/external/` |
| 전역 예외 핸들러 | `common/exception/` |
| 공통 응답 포맷 | `common/response/` |
| BaseEntity (생성일/수정일) | `common/model/` |
| 전역 설정 (Security 등) | `common/config/{주제}/` |

### 🚫 의도적으로 적용하지 않은 것

커리큘럼 가이드("헥사고날 아키텍처 등 기타 아키텍처 프로젝트 적용 X")를 따라 **아래 패턴은 쓰지 않습니다.**

| 패턴 | 왜 안 쓰나 |
|------|-----------|
| Aggregate Root, Value Object | 정통 DDD 전술 패턴 — 학습 비용 대비 이득 적음 |
| `domain/policy/` | 검증 로직은 `application/` 서비스 안에서 처리 |
| `domain/service/` | 도메인 로직은 엔티티 메서드 or `application/`으로 |
| `domain/event/` + `listener/` | Domain Event 패턴 — MSA 전환 시 본격 학습 예정 |
| `internal/` (모듈간 통신) | Pragmatic DDD 패턴 — 지금은 직접 `@Service` 호출로 충분 |
| Bounded Context 엄격 분리 | 도메인 폴더링으로 느슨하게만 구분 |

### 📚 더 자세한 문서

- [📖 상세 아키텍처 가이드](./docs/architecture.md)

