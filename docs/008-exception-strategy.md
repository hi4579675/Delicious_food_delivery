# 008. 예외 처리 전략 비교 및 선택 근거

> **이 문서를 보면**: 왜 개별 예외 클래스 방식을 선택했는지, 기존 방식과 뭐가 다른지, MSA에서 어떤 이점이 있는지 파악 가능.
>
> **언제 다시 보나요**: 새 도메인 예외 설계 시, 팀원 온보딩 시, 면접 준비 시.

---

## 한 줄 요약

**ErrorCode enum은 그대로 유지하되, 예외를 던질 때는 개별 Exception 클래스를 사용한다.** 가독성·catch 세분화·MSA 대응 모두 개선.

---

## 0. 예외 처리의 진화 과정

이 프로젝트의 예외 처리는 3단계를 거쳐 발전했다. 각 단계의 문제점이 다음 단계의 설계 동기가 된다.

### 0단계: 중앙 Enum 하나로 모든 에러 관리

가장 단순한 접근. 프로젝트 전체에 `ErrorCode` enum 1개, `BusinessException` 1개만 존재.

```java
// 프로젝트 전체에서 쓰는 단일 enum
public enum ErrorCode {
    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    CANCEL_TIME_EXCEEDED(HttpStatus.BAD_REQUEST, "ORDER-002", "취소 가능 시간이 지났습니다."),
    // 가게
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-001", "가게를 찾을 수 없습니다."),
    // ... 모든 도메인의 에러가 한 파일에 ...
}

// 예외 클래스도 1개
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}

// 서비스에서 사용
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
```

**문제점:**

| 문제 | 설명 |
|------|------|
| **파일 비대화** | 도메인이 10개면 에러 코드가 수십 개 → enum 파일이 수백 줄 |
| **Git 충돌** | 여러 명이 동시에 같은 enum 파일을 수정 → merge conflict 빈발 |
| **도메인 경계 없음** | User 개발자가 Order 에러 코드를 실수로 던져도 컴파일 에러 없음 |
| **MSA 분리 불가** | 하나의 enum에 모든 도메인이 묶여 있어 서비스 분리 시 전부 끌고 가야 함 |

이 문제를 해결하기 위해 → **도메인별 ErrorCode enum 분리** (1단계)

---

### 1단계: 도메인별 ErrorCode + 단일 Exception (이전 방식)

ErrorCode를 도메인별로 분리하고, `ErrorCode` 인터페이스로 다형성 확보.

```java
// 공통 인터페이스
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}

// 도메인별 분리
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
    // ...
}

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    // ...
}

// 도메인별 예외 (1개)
public class UserException extends BaseException {
    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}

// 서비스에서 사용
throw new UserException(UserErrorCode.USER_NOT_FOUND);
```

**0단계 대비 개선된 점:**
- 도메인별 파일 분리 → Git 충돌 감소
- 타 도메인 ErrorCode 사용 방지 (UserException은 UserErrorCode만 받음)
- MSA 분리 시 도메인별 enum만 가져가면 됨

**남은 문제점:**

| 문제 | 설명 |
|------|------|
| **catch 불가** | `catch (UserException e)` — 어떤 에러인지 if문으로 재분기 필요 |
| **로그 식별 어려움** | 전부 `UserException`으로 찍힘 → 메시지까지 읽어야 구분 |
| **코드 장황** | `() -> new UserException(UserErrorCode.USER_NOT_FOUND)` — 길다 |

이 문제를 해결하기 위해 → **개별 예외 클래스** (2단계, 현재)

---

### 2단계: 도메인별 ErrorCode + 개별 Exception (현재 채택)

ErrorCode enum은 유지하면서, 예외를 던질 때는 개별 클래스 사용.

```java
// ErrorCode — 1단계와 동일하게 유지
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
}

// 개별 예외 클래스
public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}

// 서비스에서 사용
throw new UserNotFoundException();
```

**1단계 대비 개선된 점:**
- 특정 예외만 catch 가능
- 로그에서 클래스명으로 즉시 식별
- 메서드 레퍼런스 사용 가능 (`UserNotFoundException::new`)

---

### 진화 요약

```
0단계: 중앙 ErrorCode 1개 + BusinessException 1개
  → 문제: 파일 비대화, Git 충돌, 도메인 경계 없음
  
1단계: 도메인별 ErrorCode + 도메인별 Exception 1개
  → 개선: 도메인 분리, MSA 대비
  → 문제: catch 불가, 로그 식별 어려움

2단계: 도메인별 ErrorCode + 개별 Exception 클래스 (현재)
  → 개선: catch 세분화, 가독성, 모니터링
```

---

## 1. 두 가지 방식 비교

### 방식 A: 단일 Exception + ErrorCode enum (기존)

```java
// 예외 클래스 1개
public class UserException extends BaseException {
    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}

// 서비스에서 사용
throw new UserException(UserErrorCode.USER_NOT_FOUND);
throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
throw new UserException(UserErrorCode.INVALID_PASSWORD);
```

### 방식 B: 개별 Exception 클래스 (현재 채택)

```java
// 예외 클래스 N개 — 각각 하나의 ErrorCode에 매핑
public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}

public class DuplicateEmailException extends BaseException {
    public DuplicateEmailException() {
        super(UserErrorCode.DUPLICATE_EMAIL);
    }
}

// 서비스에서 사용
throw new UserNotFoundException();
throw new DuplicateEmailException();
```

---

## 2. 상세 비교

| 항목 | 방식 A (단일 Exception) | 방식 B (개별 Exception) |
|------|------------------------|------------------------|
| **파일 수** | ErrorCode 1개 + Exception 1개 = 2개 | ErrorCode 1개 + Exception N개 = N+1개 |
| **서비스 코드** | `throw new UserException(UserErrorCode.USER_NOT_FOUND)` | `throw new UserNotFoundException()` |
| **메서드 레퍼런스** | `() -> new UserException(UserErrorCode.USER_NOT_FOUND)` | `UserNotFoundException::new` |
| **가독성** | ErrorCode까지 읽어야 의미 파악 | 클래스명만으로 즉시 파악 |
| **특정 예외 catch** | 불가능 (전부 UserException) | 가능 (`catch (UserNotFoundException e)`) |
| **로그 식별** | `UserException` — 뭔 에러인지 모름 | `UserNotFoundException` — 바로 식별 |
| **GlobalExceptionHandler** | 동일 (BaseException으로 처리) | 동일 (BaseException으로 처리) |
| **ErrorCode enum** | 사용 | 동일하게 사용 |

---

## 3. 방식 B를 선택한 이유

### 3-1. 서비스 코드 가독성

```java
// 방식 A — 읽을 때 ErrorCode까지 눈이 가야 함
User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

// 방식 B — 클래스명이 곧 의미
User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);
```

### 3-2. 특정 예외만 catch 가능

MSA 환경에서 다른 서비스를 호출할 때, 특정 에러에만 대응해야 하는 경우가 많다.

```java
// 방식 B — 특정 예외만 잡을 수 있음
try {
    userClient.getUser(userId);
} catch (UserNotFoundException e) {
    // 사용자 없으면 게스트로 처리
    return GuestUser.create();
} catch (TokenExpiredException e) {
    // 토큰 만료면 재발급 시도
    tokenService.refresh();
}

// 방식 A — if문으로 다시 분기해야 함
try {
    userClient.getUser(userId);
} catch (UserException e) {
    if (e.getErrorCode() == UserErrorCode.USER_NOT_FOUND) {
        return GuestUser.create();
    }
    throw e;  // 나머지는 다시 던짐
}
```

### 3-3. 로그/모니터링 식별

운영 환경에서 에러 로그를 볼 때:

```
// 방식 A — 로그만 봐선 뭔 에러인지 모름, 메시지까지 읽어야 함
ERROR UserException: 사용자를 찾을 수 없습니다.
ERROR UserException: 이미 사용 중인 이메일입니다.
ERROR UserException: 비밀번호가 올바르지 않습니다.

// 방식 B — 클래스명만으로 즉시 분류 가능
ERROR UserNotFoundException: 사용자를 찾을 수 없습니다.
ERROR DuplicateEmailException: 이미 사용 중인 이메일입니다.
ERROR InvalidPasswordException: 비밀번호가 올바르지 않습니다.
```

Grafana, Sentry 같은 모니터링 도구에서 예외 클래스명 기준으로 알림을 설정할 수 있다.

### 3-4. Spring 프레임워크와 동일한 패턴

Spring Security도 개별 예외 클래스 방식을 사용한다:

```
AuthenticationException (추상)
├── BadCredentialsException        // 비밀번호 틀림
├── UsernameNotFoundException      // 사용자 없음
├── AccountExpiredException        // 계정 만료
├── LockedException                // 계정 잠김
└── DisabledException              // 계정 비활성화
```

우리 구조도 동일한 패턴:

```
BaseException (추상)
├── UserNotFoundException
├── DuplicateEmailException
├── InvalidPasswordException
├── InvalidRoleException
├── ForbiddenRoleChangeException
├── InvalidCredentialsException    // (auth)
├── TokenExpiredException          // (auth)
└── ...
```

---

## 4. ErrorCode enum을 유지하는 이유

개별 예외 클래스를 쓰더라도 ErrorCode enum은 **삭제하지 않는다.** 이유:

1. **클라이언트(프론트엔드) 식별용**: 프론트는 HTTP 상태 코드만으로 에러를 구분할 수 없음. `"USER-001"` 같은 코드로 에러별 UI 분기 처리
2. **에러 코드 한눈에 관리**: enum 파일 하나에 해당 도메인의 모든 에러가 정리됨
3. **GlobalExceptionHandler 호환**: `BaseException.getErrorCode()`로 응답을 자동 생성

```
ErrorCode enum     → 에러 코드/메시지/상태 정의 (한 곳에서 관리)
개별 Exception     → 서비스에서 던지고 catch하는 용도 (가독성/세분화)
GlobalExceptionHandler → BaseException 다형성으로 자동 처리 (건드릴 일 없음)
```

---

## 5. 새 예외 추가 시 체크리스트

1. `{Domain}ErrorCode` enum에 새 코드 추가
2. 개별 예외 클래스 생성 (`{도메인}/domain/exception/`)
3. 서비스에서 `.orElseThrow(XxxException::new)` 또는 `throw new XxxException()` 사용
4. 끝. GlobalExceptionHandler가 자동 처리.

### 예시: 주문 도메인에 예외 추가

```java
// 1. ErrorCode
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    CANCEL_TIME_EXCEEDED(HttpStatus.BAD_REQUEST, "ORDER-002", "주문 취소 가능 시간(5분)이 지났습니다."),
    ;
    // ...
}

// 2. 개별 예외
public class OrderNotFoundException extends BaseException {
    public OrderNotFoundException() {
        super(OrderErrorCode.ORDER_NOT_FOUND);
    }
}

public class CancelTimeExceededException extends BaseException {
    public CancelTimeExceededException() {
        super(OrderErrorCode.CANCEL_TIME_EXCEEDED);
    }
}

// 3. 서비스
Order order = orderRepository.findById(orderId)
        .orElseThrow(OrderNotFoundException::new);

if (order.isOverCancelDeadline()) {
    throw new CancelTimeExceededException();
}
```

---

## 관련 문서

- [007. 공통 기반 가이드](./007-common-foundation-guide.md) — 예외 사용법 상세
- [004. 아키텍처 가이드](./004-architecture.md) — 계층별 역할
