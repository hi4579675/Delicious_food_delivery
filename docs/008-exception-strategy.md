# 007. 공통 기반(Common Foundation) 사용 가이드

> **이 문서를 보면**: 공통 모듈에서 제공하는 응답 포맷 / 예외 처리 / 감사 필드 / 보안 뼈대를 내 도메인에서 어떻게 써야 하는지 파악 가능.
>
> **언제 다시 보나요**: 새 도메인 착수 시, 예외 던지는 코드 처음 작성할 때, 컨트롤러 응답 감 안 잡힐 때.

---

## 한 줄 요약

모든 엔티티는 `BaseEntity` 상속 / 모든 예외는 `BaseException` 상속 / 모든 컨트롤러 응답은 `ApiResponse` 포맷 / 페이지 응답은 `PageResponse.from(page)`.

---

## 목차

- [1. 패키지 구조 복습](#1-패키지-구조-복습)
- [2. 의존성 방향](#2-의존성-방향)
- [3. 엔티티 만들 때](#3-엔티티-만들-때)
- [4. 예외 던지는 방법](#4-예외-던지는-방법)
- [5. 컨트롤러 응답 작성](#5-컨트롤러-응답-작성)
- [6. common 패키지 활용법](#6-common-패키지-활용법)

---

## 1. 패키지 구조 복습

구조는 2단계로 나뉩니다.

1. **도메인별 폴더** (user, order, store 등)
2. 각 도메인 안에서 **4계층** (presentation / application / domain / infrastructure)

"새 코드 어디에 둘까?" 생각할 때 순서:

- Q1. 어느 **도메인**? (user? order? store?)
- Q2. 어느 **계층**? (컨트롤러? 서비스? 엔티티?)

| 만들 것 | 위치 |
|---|---|
| REST API 메서드 | `{domain}/presentation/` |
| Request/Response DTO | `{domain}/presentation/dto/` |
| `@Service` | `{domain}/application/` |
| `@Entity` | `{domain}/domain/entity/` |
| Repository 인터페이스 | `{domain}/domain/repository/` |
| 도메인 전용 예외 | `{domain}/domain/exception/` |
| 커스텀 쿼리 구현체 | `{domain}/infrastructure/persistence/repository/` |

> 더 자세한 위치 가이드는 [`docs/004-architecture.md`](./004-architecture.md) 참고.

---

## 2. 의존성 방향

```
presentation → application → domain ← infrastructure
```

- 컨트롤러는 **서비스만** 호출
- 서비스는 엔티티·Repository를 조합
- Repository 구현체(infrastructure)는 `domain/repository/`의 인터페이스를 **구현**하고 엔티티를 반환 — **의존성 역전**
- `domain`은 다른 계층을 **절대 모름**

---

## 3. 엔티티 만들 때

```java
@Entity
@Table(name = "p_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {   // 🔴 무조건 BaseEntity 상속

    @Id @GeneratedValue
    private UUID storeId;

    private Long userId;        // 🔴 @ManyToOne User 금지, Long FK 만
    private UUID regionId;      // 🔴 @ManyToOne Region 금지
    // ...
}
```

**지키면 되는 것**

- `createdAt / updatedAt / deletedAt / createdBy / updatedBy / deletedBy`는 **작성하지 않음** — `BaseEntity`가 자동 관리
- 삭제 시 `store.softDelete(currentUserId)` 호출 (물리 삭제 금지)
- 다른 도메인 엔티티는 **Long/UUID FK 로만** 참조
- 모든 연관관계는 `LAZY`

> 세부 규칙은 [`docs/005-jpa-guidelines.md`](./005-jpa-guidelines.md) 참고.

---

## 4. 예외 던지는 방법

**제일 자주 하게 되는 작업.** 3단계로 정리.

### Step 1. 도메인 ErrorCode enum 작성

위치: `{domain}/domain/exception/{Domain}ErrorCode.java`

```java
// 예: user/domain/exception/UserErrorCode.java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER-003", "비밀번호가 올바르지 않습니다."),
    FORBIDDEN_ROLE_CHANGE(HttpStatus.FORBIDDEN, "USER-004", "권한을 변경할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

> 코드 네이밍 규칙: `{DOMAIN}-{001부터}` (예: `USER-001`, `AUTH-001`, `ORDER-001`)

### Step 2. 개별 예외 클래스 작성

위치: `{domain}/domain/exception/{예외이름}Exception.java`

**ErrorCode 하나당 예외 클래스 하나.** 서비스에서 던질 때 의미가 바로 드러남.

```java
// user/domain/exception/UserNotFoundException.java
public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}

// user/domain/exception/DuplicateEmailException.java
public class DuplicateEmailException extends BaseException {
    public DuplicateEmailException() {
        super(UserErrorCode.DUPLICATE_EMAIL);
    }
}

// user/domain/exception/InvalidPasswordException.java
public class InvalidPasswordException extends BaseException {
    public InvalidPasswordException() {
        super(UserErrorCode.INVALID_PASSWORD);
    }
}
```

### Step 3. 서비스에서 throw

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .role(UserRole.valueOf(request.role().toUpperCase()))
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(UserNotFoundException::new);

        return UserResponse.from(user);
    }
}
```

> 💡 `GlobalExceptionHandler`가 `BaseException`을 다형 처리하므로 **컨트롤러에 try-catch 필요 없음**.

### 응답 확인

아래 포맷으로 자동 내려감:

```json
{
  "success": false,
  "status": 404,
  "errorCode": "USER-001",
  "message": "사용자를 찾을 수 없습니다.",
  "data": null
}
```

### 자주 묻는 것

- **Q. `@Valid` 검증 실패도 따로 처리해야 하나요?**
  → 아니요. `GlobalExceptionHandler`가 자동으로 `COMMON-001` + 필드 에러 목록으로 내려줍니다.
- **Q. 공통 예외(인증/인가/404 등)도 따로 만들어야 하나요?**
  → 아니요. `CommonErrorCode` + `CommonException`이 이미 준비돼 있습니다. 꼭 도메인 고유 의미가 있을 때만 새 enum 추가.
- **Q. 왜 단일 `UserException` 대신 개별 클래스?**
  → `throw new UserNotFoundException()`이 `throw new UserException(UserErrorCode.USER_NOT_FOUND)`보다 읽기 쉽고, catch할 때도 특정 예외만 잡을 수 있음.

---

## 5. 컨트롤러 응답 작성

모든 컨트롤러 메서드는 **`ResponseEntity<ApiResponse<T>>`** 로 반환합니다.

> HTTP 상태 코드와 `ApiResponse.status` 를 일치시키기 위해 `ResponseEntity` 로 감싸는 게 팀 컨벤션.
> `GlobalExceptionHandler` 도 같은 시그니처라 예외 응답과 정상 응답의 구조가 동일합니다.

```java
@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 생성 (201)
    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    // 단건 조회 (200)
    @Operation(summary = "마이페이지 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(principal.getId())));
    }

    // 수정 (200)
    @Operation(summary = "내 정보 수정")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(principal.getId(), request)));
    }

    // 데이터 없는 성공 (200)
    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 페이지네이션 (관리자)
    @Operation(summary = "사용자 목록 검색 (관리자)")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.searchUsers(pageable)));
    }
}
```

**지키면 되는 것**

- ⚠️ **엔티티를 그대로 반환하지 말 것.** 반드시 Response DTO 로 변환 (`UserResponse.from(user)` 같은 static factory 권장)
- ⚠️ 정적 팩토리는 목적에 맞게: `ApiResponse.success(data)` / `ApiResponse.ok()` / `ApiResponse.created(data)`
- ⚠️ 생성(POST) 응답은 `ResponseEntity.status(HttpStatus.CREATED).body(...)` 로 HTTP 201 명시
- ⚠️ 페이지는 항상 `PageResponse.from(page)` 로 변환 (Spring `Page` 직노출 금지)
- ⚠️ Swagger 어노테이션: 컨트롤러에 `@Tag`, 메서드에 `@Operation(summary = "...")` 필수
- ⚠️ 인증 필요한 API는 `@AuthenticationPrincipal UserPrincipal principal` 로 사용자 정보 주입
- ⚠️ 권한 제한은 `@PreAuthorize("hasRole('MASTER')")` 또는 `hasAnyRole(...)` 사용

### 성공 응답 예시

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": { "userId": 1, "email": "user@example.com", "name": "alice" }
}
```

### 페이지 응답 예시

```json
{
  "success": true,
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "data": {
    "content": [ { "..." }, { "..." } ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "last": false
  }
}
```

---

## 6. common 패키지 활용법

공통으로 쓰는 것들은 전부 `common/` 아래에 있습니다.

| 클래스 | 위치 | 용도 |
|---|---|---|
| `BaseEntity` | `common/model/` | 모든 엔티티가 상속 (감사 필드 + soft delete) |
| `ApiResponse` | `common/response/` | 모든 컨트롤러 응답 포맷 |
| `PageResponse` | `common/response/` | 페이지네이션 응답 변환 |
| `ErrorCode` | `common/exception/` | 도메인별 ErrorCode enum이 구현할 인터페이스 |
| `BaseException` | `common/exception/` | 모든 도메인 예외의 부모 |
| `CommonErrorCode` / `CommonException` | `common/exception/` | 공통 예외(입력값 오류, 인증 실패 등) |
| `GlobalExceptionHandler` | `common/exception/` | 전역 예외 처리 (건드릴 일 없음) |
| `JpaAuditingConfig` | `common/config/jpa/` | `@CreatedBy` / `@LastModifiedBy` 자동 채움 |
| `SecurityConfig` | `common/config/security/` | 전역 보안 설정 (JWT 필터는 auth 담당자) |
| `UserPrincipal` | `common/config/security/` | auth 쪽에서 구현할 인증 주체 인터페이스 |

### 꼭 기억할 것

- `common/` 은 **모든 도메인이 의존** — 여기서 뭘 바꾸면 전 도메인 영향. 건드릴 때 팀에 공유
- 내 도메인 전용 Util/Enum/Validator 는 `common/` 이 아니라 `{domain}/` 안에 두기
- 여러 도메인에서 **실제로 쓰게 된 시점**에 `common/` 으로 승격

---

## 체크리스트 (새 도메인 착수 시)

- [ ] 엔티티가 `BaseEntity` 를 상속했는가
- [ ] 연관관계는 Long/UUID FK 만 사용했는가 (`@ManyToOne {다른도메인엔티티}` 금지)
- [ ] `{Domain}ErrorCode` enum을 작성했는가
- [ ] ErrorCode 하나당 개별 예외 클래스를 작성했는가 (예: `UserNotFoundException`)
- [ ] 서비스에서 `.orElseThrow(XxxException::new)` 패턴으로 던지는가
- [ ] 컨트롤러는 `ResponseEntity<ApiResponse<T>>` 로 반환하는가
- [ ] Swagger 어노테이션(`@Tag`, `@Operation`)을 달았는가
- [ ] 엔티티를 컨트롤러까지 올리지 않고 Response DTO 로 변환했는가
- [ ] 페이지 조회는 `PageResponse.from(page)` 로 변환했는가

---

## 관련 문서

- [004. 아키텍처 가이드](./004-architecture.md)
- [005. JPA 가이드](./005-jpa-guidelines.md)
- [006. 팀 협업 컨벤션](./006-conventions.md)
