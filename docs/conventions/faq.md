# 팀 FAQ — 자주 헷갈리는 것

> **이 문서를 보면**: 팀원이 반복해서 묻는 컨벤션을 한 번에 확인 가능.
>

---

## 한 줄 요약

**DTO는 `presentation/dto/` 한 군데** / **Command DTO 만들지 않음** / **Repository 구현체는 커스텀 쿼리 필요할 때만** / **BaseEntity는 로그성 테이블 빼고 상속** / **Soft Delete는 `@SQLRestriction`만**.

---

##  DTO

### Q. Request DTO / Response DTO 어디에 두나요?

**둘 다 `{도메인}/presentation/dto/`에 평탄하게** 둡니다. `request/`, `response/` 하위 폴더는 만들지 않습니다.

```
user/presentation/dto/
├── SignupRequest.java
├── UserUpdateRequest.java
└── UserInfo.java
```

### Q. Command DTO (`SignupCommand` 같은 거) 만들어야 하나요?

**만들지 않습니다.** Service가 Request DTO를 직접 받습니다.

```java
// ✅ OK
public UserInfo signup(SignupRequest request) { ... }

// ❌ 하지 말 것
public UserInfo signup(SignupCommand command) { ... }
```

필드 수가 적으면 primitive 여러 개로 받아도 OK (`getUserById(Long userId)`).

### Q. Service가 `presentation/dto/`를 import해도 되나요?

**관행상 OK.** 엄격한 "application → presentation 금지" 원칙은 **Controller 클래스 import만 금지**로 완화 해석합니다. DTO import는 허용.

### Q. 엔티티를 컨트롤러 응답으로 쓰면 안 되나요?

**절대 금지.** 반드시 Response DTO로 변환해서 반환하세요. `UserInfo.from(user)` 같은 static factory 권장.

---

##  Repository / infrastructure

### Q. Repository 구현체(`UserRepositoryImpl`)를 무조건 `infrastructure/`에 만들어야 하나요?

**아니요.** 기본 CRUD는 `domain/repository/`의 `JpaRepository` 인터페이스 하나로 충분합니다 (Spring Data JPA 프록시가 자동 구현).

`infrastructure/persistence/repository/`에 구현체를 두는 경우는:
- **QueryDSL 커스텀 쿼리**
- **복잡한 JPQL/Native Query**
- **검색/정렬 전용 메서드**

이런 상황이 올 때만 만듭니다.

### Q. `infrastructure/` 폴더는 언제 필요한가요?

필요할 때만 등장합니다:
- `persistence/repository/` — 커스텀 쿼리 구현체
- `external/` — 외부 API 클라이언트 (`GeminiClient` 등)
- `jwt/` — JWT Provider/Filter (auth 도메인만)

**기본 프로젝트 시작 시엔 비어 있어도 됩니다.**

---

##  엔티티 / BaseEntity / Soft Delete

### Q. 모든 엔티티가 `BaseEntity`를 상속해야 하나요?

**대부분 상속하지만, 로그성 테이블은 예외**입니다.
- **예외 테이블**: `p_order_items`, `p_llm_calls` — 수정/삭제가 없는 로그성이라 `created_at`, `created_by`만 필드로 두고 `BaseEntity` 미상속.
- 나머지 엔티티는 전부 `BaseEntity extends` 필수.

### Q. Soft Delete는 어떻게 구현하나요?

**`@SQLRestriction`만 엔티티에 붙이고, 삭제는 서비스에서 `entity.softDelete(currentUserId)`를 명시 호출**합니다.

```java
@Entity
@SQLRestriction("deleted_at IS NULL")   // 조회 시 자동 필터링
public class Store extends BaseEntity { }
```

```java
@Transactional
public void deleteStore(UUID storeId) {
    Store store = storeRepository.findById(storeId).orElseThrow();
    store.softDelete(currentUserId);   // deleted_at + deleted_by 기록
}
```

### Q. `@SQLDelete`는 왜 쓰지 않나요?

`@SQLDelete`는 DELETE SQL을 UPDATE로 자동 바꿔주지만 **정적 SQL 템플릿이라 `deleted_by` (누가 삭제했는지)를 기록할 수 없습니다.** 감사 추적 위해 수동 호출 방식 채택.

### Q. `findById` 이후에 `.filter(u -> !u.isDeleted())` 체크 필요하나요?

**불필요합니다.** `@SQLRestriction`이 조회 시점에 자동으로 삭제된 행을 제외하므로 중복 체크입니다.

---

##  아키텍처 / 의존성

### Q. 헥사고날 아키텍처인가요?

**아닙니다.** **Layered Architecture + Package by Feature**입니다. DDD 용어(`application`, `domain`, `infrastructure`, `presentation`)만 차용.

### Q. 다른 도메인 서비스를 호출해도 되나요?

**OK.** `StoreService`가 `ReviewService`를 주입받아 호출하는 식. 단, **다른 도메인의 Repository를 직접 주입하는 건 지양** (서비스를 통해 접근).

### Q. Controller가 Repository를 직접 호출해도 되나요?

**NO.** Controller는 Service만 호출. Repository는 Service 안에서만.

---

## ⚠️ 예외 처리

### Q. 도메인별 예외 클래스를 매번 하나씩 만들어야 하나요?

**네.** `ErrorCode enum` 하나당 **개별 예외 클래스 하나**. 서비스에서 던질 때 의미가 바로 드러나게.

```java
// ✅ OK — 읽기 쉽다
throw new UserNotFoundException();

// ❌ 하지 말 것
throw new UserException(UserErrorCode.USER_NOT_FOUND);
```

자세한 근거: [예외 처리 전략](./exception.md)

### Q. 공통 예외(인증/인가/404 등)도 도메인마다 만들어야 하나요?

**아니요.** `CommonErrorCode` + `CommonException`이 이미 `common/exception/`에 있습니다. **도메인 고유 의미가 있을 때만** 새로 만드세요.

### Q. Controller에 `try-catch` 넣어야 하나요?

**불필요.** `GlobalExceptionHandler`가 `BaseException`을 다형 처리합니다.

---

## 응답 포맷

### Q. 컨트롤러 반환 타입은?

**항상** `ResponseEntity<ApiResponse<T>>`로 통일.

### Q. POST 생성 응답은?

```java
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(data));
```

### Q. 페이지 응답은?

`PageResponse.from(page)`로 변환. Spring `Page`를 그대로 반환하면 직렬화 포맷이 지저분해서 팀 컨벤션으로 금지.

---

## 🌿 브랜치 / 커밋 / PR

### Q. 브랜치 이름 규칙?

`타입/기능명` — 예: `feat/login`, `fix/jwt-expiration`.

### Q. PR 머지 조건?

**리뷰어 최소 2명 승인** 후 머지. Squash and Merge 권장.

### Q. 커밋 메시지 타입은?

`feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style` 등. 자세한 표는 [팀 협업 컨벤션](./team.md#commit-type).

---

## 관련 문서

- [패키지·계층 구조 가이드](../architecture/package-structure.md) — 계층 구조 / 의존성 가이드라인 상세
- [JPA 엔티티 설계 가이드](../architecture/jpa.md) — 엔티티/Soft Delete 상세
- [공통 기반 사용 가이드](../architecture/common-foundation.md) — 예외/응답 포맷 상세
- [예외 처리 전략](./exception.md) — 예외 클래스 설계 근거
