# 004. 아키텍처 가이드

> **이 문서를 보면**: 폴더 구조가 왜 이렇게 생겼는지, 어떤 코드를 어디에 둬야 하는지 파악 가능.
>
> **언제 다시 보나요**: 새 기능 구현 시, 파일 위치 헷갈릴 때, 신규 팀원 온보딩 시.

---

## 한 줄 요약

**Layered Architecture + Package by Feature**. 도메인별 폴더 + 그 안에서 `presentation / application / domain / infrastructure` 4계층. 정통 DDD/헥사고날은 **적용 X**.

---

## 한눈에 보기

- **구조**: Layered Architecture + Package by Feature
- **가이드 기준**: 튜터 권장 "Layered Architecture (Controller / Service / Repository)" 준수
- **스탠스**: 정통 DDD/헥사고날 **적용 X**. DDD 용어(`application`, `domain`, `infrastructure`, `presentation`)만 차용
- **의도**: 다음 프로젝트(MSA) 대비 도메인별 경계를 미리 연습

```
{도메인}/
├── presentation/     → 컨트롤러 + DTO
├── application/      → 서비스 (평소 하던 거)
├── domain/
│   ├── entity/       → JPA 엔티티
│   ├── repository/   → Repository 인터페이스
│   └── exception/    → 도메인 전용 예외
└── infrastructure/
    └── persistence/
        └── repository/   → Repository 구현체 (필요 시)
```

---

## 팀원 학습 가이드

### 학습 순서

#### 1. Layered Architecture + 의존성 방향
**목표**: "계층이 한 방향으로만 의존한다"는 게 뭔지 이해

- 🔍 검색 키워드:
  - `Layered Architecture 스프링`
  - `클린 아키텍처 스프링 적용`
  - `스프링 계층 구조`
- 🎯 이해해야 할 핵심:
  - **의존성 방향**: `presentation → application → domain ← infrastructure`
  - **domain은 아무것도 몰라야 함** (DB도, 외부 API도)
  - **infrastructure가 domain의 인터페이스를 구현** (의존성 역전)
  - 각 계층의 **책임 분리** (컨트롤러는 HTTP, 서비스는 유스케이스, 엔티티는 데이터)

#### 2. Package by Feature + 우리 프로젝트 탐색
**목표**: "왜 `controller/` 최상위가 아니라 `user/` 최상위인지" 이해하고 실제 코드 구조 파악

- 🔍 검색 키워드:
  - `패키지 구조 도메인형 계층형`
  - `Package by Feature 패키지 구조`
  - `스프링 패키지 구조 도메인 기반`
- 🎯 이해해야 할 핵심:
  - 도메인 중심 그루핑의 장점 (응집도, 협업 편의성, MSA 전환 용이)
  - 각 도메인 내부는 4계층으로 구성
- 💻 코드 탐색:
  1. `common/` 폴더 (BaseEntity, ApiResponse, GlobalExceptionHandler 위치)
  2. `user/` 폴더 (가장 기본적인 도메인)
  3. 이 문서의 **"📖 도메인별 설명"** 섹션 정독

### 공부하지 않아도 되는 것

- ❌ **정통 DDD** (Aggregate Root, Value Object, Ubiquitous Language 등)
  - 커리큘럼 가이드상 적용 X
- ❌ **헥사고날 아키텍처** 전면 적용
  - 가이드 명시: "학습은 해보되 프로젝트에 적용은 하지 않도록 합니다"
- ❌ **MSA, Saga Pattern, CQRS, Domain Event**
  - 지금은 모놀리스. 다음 프로젝트에서 집중 학습

### 헷갈릴 때 하는 질문 순서

1. "내가 만드는 게 **어느 계층** 코드인가?" (컨트롤러? 서비스? 엔티티?)
2. "이게 **어느 도메인**에 속하는가?" (user? order? store?)
3. "이 로직이 **외부 의존성을 가지는가**?" (DB/API 호출하면 infrastructure)

---

## "이 코드 어디에 둬야 해?" 완전 가이드

###  빠른 참조 치트시트

| 만들 것 | 위치 | 예시 |
|---------|------|------|
| REST API 메서드 | `{도메인}/presentation/` | `UserController` |
| Request/Response DTO | `{도메인}/presentation/dto/` | `UserCreateRequest` |
| 평소 쓰던 `@Service` | `{도메인}/application/` | `UserService` |
| `@Entity` | `{도메인}/domain/entity/` | `User.java` |
| `JpaRepository` 인터페이스 | `{도메인}/domain/repository/` | `UserRepository` |
| 도메인 전용 예외 | `{도메인}/domain/exception/` | `UserNotFoundException` |
| Repository 구현체 (커스텀 쿼리) | `{도메인}/infrastructure/persistence/repository/` | `UserRepositoryImpl` |
| 외부 API 클라이언트 | `{도메인}/infrastructure/external/` | `GeminiClient` |
| JWT 관련 | `auth/infrastructure/jwt/` | `JwtProvider` |
| 전역 예외 핸들러 | `common/exception/` | `GlobalExceptionHandler` |
| 공통 응답 포맷 | `common/response/` | `ApiResponse` |
| BaseEntity (생성일/수정일) | `common/model/` | `BaseEntity` |
| 전역 설정 | `common/config/{주제}/` | `SecurityConfig` |

### 자주 헷갈리는 케이스

#### Q1. 비즈니스 규칙(검증 로직)은 어디에 두지?

평소처럼 **`application/` 서비스 안에 작성**합니다. (정통 DDD의 `policy/` 같은 별도 폴더는 사용하지 않음)

```java
@Service
@Transactional
public class UserService {
    public UserResponse signUp(UserCreateRequest req) {
        // 1. 중복 체크 (검증 로직)
        if (userRepository.existsByUsername(req.username())) {
            throw new DuplicateUsernameException();
        }
        // 2. 비밀번호 암호화
        // 3. 저장
        // 4. 응답 반환
    }
}
```

- 간단한 검증 → 서비스 메서드에 바로
- 재사용 가능한 복잡한 검증 → 별도 `Validator` 클래스 (필요 시 `common/` 또는 `{도메인}/application/`)

#### Q2. 다른 도메인의 서비스를 호출해도 돼?

**가능합니다.** 다른 도메인의 `@Service`를 직접 주입받아 호출하세요.

```java
@Service
public class StoreService {
    private final ReviewService reviewService;  // ← 다른 도메인 서비스 직접 호출 OK
    
    public StoreDetail getDetail(Long storeId) {
        Double avg = reviewService.getAverageRating(storeId);
        // ...
    }
}
```

> ⚠️ **주의**: 다른 도메인의 **Repository를 직접 주입**하는 건 피하세요. 서비스를 통해 접근.

#### Q3. DTO는 어디에 두지?

- **Request/Response DTO**: `{도메인}/presentation/dto/`
  - 컨트롤러가 받고 응답하는 객체
- 엔티티는 **절대 컨트롤러까지 올라오면 안 됨**. 응답 DTO로 변환해서 반환.

#### Q4. 예외는 어디에?

- **도메인 전용 예외** (`UserNotFoundException` 등): `{도메인}/domain/exception/`
- **전역 공통 예외** (`BaseException`, `ErrorCode`): `common/exception/`
- **전역 핸들러** (`GlobalExceptionHandler`): `common/exception/`

모든 도메인 예외는 `common/exception/BaseException`을 상속해서 작성.

---

##  계층별 역할 (쉽게 설명)

### `presentation/` = 컨트롤러 계층
- **역할**: 외부 REST API (클라이언트 앱과 통신)
- **쉽게**: `@RestController` + 요청/응답 DTO가 사는 곳
- **들어가는 것**:
  - `@RestController` 클래스
  - `Request`/`Response` DTO
- **하지 말 것**: 비즈니스 로직 넣지 않기 (서비스 호출만)

### `application/` = 서비스 계층 (유스케이스)
- **역할**: 유스케이스 오케스트레이션
- **쉽게**: 평소 쓰던 `@Service`. "회원가입하기", "주문하기" 같은 기능 하나당 메서드 하나
- **들어가는 것**:
  - `@Service` + `@Transactional` 붙인 서비스 클래스
- **책임**:
  - 트랜잭션 경계 정의
  - 여러 도메인 조합
  - 외부 API/DB 호출 조율
  - 비즈니스 규칙 검증

### `domain/` = 도메인 계층 (엔티티 + 레포 + 예외)
- **역할**: 도메인 모델 정의
- **쉽게**: `@Entity` + `Repository 인터페이스` + 도메인 예외
- **들어가는 것**:
  - `entity/`: JPA 엔티티
  - `repository/`: Repository 인터페이스 (JpaRepository 상속)
  - `exception/`: 도메인 전용 예외
- **규칙**: **Spring Data JPA 외에는 외부 의존성 최소화**

### `infrastructure/` = 외부 연동 계층
- **역할**: 외부 시스템(DB, API)과 연결
- **쉽게**: Repository 구현체, 외부 API 클라이언트
- **들어가는 것**:
  - `persistence/repository/`: Repository 구현체 (JPA 커스텀 쿼리 등)
  - `external/`: 외부 API 클라이언트 (`GeminiClient` 등)
  - `jwt/`: JWT 관련 구현 (auth 도메인만)
- **책임**: `domain/repository/`의 인터페이스를 **구현**

---

##  의존성 규칙

```
presentation  →  application  →  domain
                     ↓              ↑
                infrastructure ─────┘
```

###  허용되는 방향
- `presentation` → `application` 호출 OK
- `application` → `domain`, `infrastructure` 호출 OK
- `infrastructure` → `domain`의 인터페이스 구현 OK

###  금지되는 방향
- `domain` → 다른 계층 의존 ❌
- `application` → `presentation` 의존 ❌ (거꾸로)
- `infrastructure` → `application` 의존 ❌

###  핵심 규칙
> **"엔티티는 아무것도 몰라야 한다. 컨트롤러는 서비스만 호출한다. 서비스가 모든 조율을 담당한다."**

---

##  도메인별 설명

> 아래 트리는 **현재 존재하는 폴더**만 반영. 각 폴더에 배치될 클래스는 구현 진행 중.

### 1. `common/` — 공통 모듈

```
common/
├── config/
│   └── security/
├── exception/
├── model/
└── response/
```

> - `config/security/` : SecurityConfig 등 보안 설정
> - `exception/` : 전역 예외 핸들러, BaseException, ErrorCode
> - `model/` : `BaseEntity`
> - `response/` : 공통 응답 포맷
> - 필요 시 추가: `util/`, `validator/`, `enums/`, `config/{web,openapi,jpa}/`

---

### 2. `auth/` — 인증 모듈
로그인, JWT 발급/검증 담당. **회원가입은 `user/`, 로그인은 `auth/`로 분리.**

```
auth/
├── presentation/
├── application/
├── domain/
│   └── exception/
└── infrastructure/
    └── jwt/
```

> - `infrastructure/jwt/` : JWT 관련 구현 (Provider, Filter 등)

---

### 3. `user/` — 사용자 모듈
회원 가입, 조회, 수정, 권한 관리.

```
user/
├── presentation/
├── application/
├── domain/
│   ├── entity/
│   ├── repository/
│   └── exception/
└── infrastructure/
    └── persistence/
        └── repository/
```

---

### 4. `address/` — 배송지 모듈
사용자의 배송지 CRUD. `user/`와 동일한 5계층 구조.

---

### 5. `region/` — 지역 모듈
배달 가능 지역, 지역별 가게 필터링. `user/`와 동일한 구조.

---

### 6. `store/` — 가게 모듈
가게 등록, 조회, 검색. `user/`와 동일한 구조.

> - 검색/정렬 쿼리는 `infrastructure/persistence/repository/`에서 구현

---

### 7. `product/` — 메뉴/상품 모듈
가게의 메뉴 관리. `user/`와 동일한 구조.

---

### 8. `order/` — 주문 모듈
주문 생성, 상태 전이. `user/`와 동일한 구조.

> - 주문 플로우: `주문요청(CUSTOMER)` → `수락` → `조리완료` → `배송수령` → `배송완료+주문완료(OWNER)`
> - 상태 전이 검증 로직은 `application/` 서비스 안에서 처리
> - `OrderItem`은 `Order`와 같은 Aggregate (컬렉션 매핑 예외 허용, [005. JPA 가이드](./005-jpa-guidelines.md) 2-3 참고)

---

### 9. `payment/` — 결제 모듈
주문 결제 처리. `user/`와 동일한 구조.

---

### 10. `review/` — 리뷰/평점 모듈
리뷰 작성, 가게 평균 평점 집계. `user/`와 동일한 구조.

> - 주문 완료 여부 체크는 리뷰 서비스에서 주문 서비스를 호출하여 처리

---

### 11. `ai/` — Gemini AI 연동

```
ai/
├── presentation/
├── application/
├── domain/
│   ├── entity/
│   ├── repository/
│   └── exception/
└── infrastructure/
    ├── persistence/
    │   └── repository/
    └── external/
        └── gemini/
```

> - `infrastructure/external/gemini/` : Gemini API 호출 클라이언트
> - 입력 글자수 제한 + "50자 이하로" 자동 삽입은 `application/` 서비스에서 처리

---

## 온보딩 체크리스트

새 팀원 합류 시 이 순서로:

- [ ] 이 문서 정독
- [ ] `common/` 폴더 열어보기
- [ ] `user/` 전체 구조 파악
- [ ] `order/` 도메인 훑어보기
- [ ] 헷갈리면 팀 채널에 질문

---

## 정리 한줄

> **"도메인별로 폴더를 나누고, 각 폴더 안에서 presentation-application-domain-infrastructure 4계층으로 분리한다. 튜터 가이드의 Layered Architecture를 따르며, 정통 DDD/헥사고날은 적용하지 않는다. Package by Feature 방식으로 MSA 전환 가능성을 열어둔다."**

---

## 참고 검색 키워드

- `Layered Architecture 스프링부트`
- `클린 아키텍처 스프링 적용`
- `스프링 패키지 구조 도메인 기반`
- `Package by Feature 스프링`
