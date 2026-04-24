# ADR-002: 예외 처리 전략 — 도메인별 ErrorCode + 개별 Exception 클래스

- 상태: Accepted
- 날짜: 2026-04-23
- 결정자: @팀

---

## 맥락

프로젝트 초기에는 예외 처리 방식이 단순했지만, 도메인이 늘어나면서 아래 문제가 차례로 드러났다.

### 진화 단계 (상세: [예외 처리 전략](../conventions/exception.md))

**0단계 — 중앙 `ErrorCode` enum 1개 + `BusinessException` 1개**
```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
```
- 파일 비대화, Git 충돌, 도메인 경계 없음, MSA 분리 불가

**1단계 — 도메인별 ErrorCode + 단일 도메인 Exception**
```java
throw new UserException(UserErrorCode.USER_NOT_FOUND);
```
- 도메인 분리는 해결됐지만, 특정 예외를 `catch`할 수 없고 로그 식별이 어려움

**2단계 — 도메인별 ErrorCode + 개별 Exception 클래스** ← 현재
```java
throw new UserNotFoundException();
```

각 단계의 남은 문제가 다음 단계의 동기가 되었고, 어떤 전략을 **공식 채택**할지 결정이 필요했다.

---

## 결정

**2단계 방식을 채택한다.** ErrorCode enum은 도메인별로 유지하되, 예외를 던질 때는 **ErrorCode 하나당 개별 Exception 클래스 하나**를 만든다.

```java
// 도메인별 ErrorCode — 유지
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 이메일입니다."),
}

// 개별 Exception 클래스
public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}

// 서비스에서 사용
throw new UserNotFoundException();
// 또는
userRepository.findById(id).orElseThrow(UserNotFoundException::new);
```

### 동작 흐름

```
throw new UserNotFoundException()
   ↓
UserNotFoundException 이 BaseException 상속 + UserErrorCode.USER_NOT_FOUND 보유
   ↓
GlobalExceptionHandler 가 BaseException 다형성으로 catch
   ↓
ErrorCode 에서 status(404) / code("USER-001") / message 를 꺼냄
   ↓
ApiResponse.error(ec) 로 통일된 JSON 응답 자동 생성
```

새 예외 추가 시 `GlobalExceptionHandler` **수정 불필요**. `BaseException` 상속만 하면 자동 처리.

---

## 대안

| 대안 | 기각 이유 |
|---|---|
| **0단계. 중앙 `ErrorCode` + `BusinessException` 1개** | 파일 비대화, Git 충돌, 도메인 경계 없음, MSA 분리 시 전부 끌고 가야 함 |
| **1단계. 도메인별 `ErrorCode` + 도메인별 Exception 1개** | 특정 예외 `catch` 불가능 (if문으로 재분기), 로그에서 메시지 읽어야 식별 |
| **2단계. 도메인별 `ErrorCode` + 개별 Exception 클래스** ✅ 채택 | 가독성·catch 세분화·로그 식별·MSA 대응 모두 개선 |

---

## 결과

### 👍 좋은 점

- **서비스 코드 가독성**: `throw new UserNotFoundException()` — 클래스명이 곧 의미
- **특정 예외만 `catch` 가능**: MSA에서 다른 서비스 호출 시 에러별로 다른 행동 가능
  ```java
  try { userClient.getUser(id); }
  catch (UserNotFoundException e) { return GuestUser.create(); }
  catch (TokenExpiredException e) { tokenService.refresh(); }
  ```
- **로그/모니터링 식별**: `UserNotFoundException` 클래스명으로 즉시 분류, Grafana/Sentry 알림 규칙 편함
- **메서드 레퍼런스 사용**: `.orElseThrow(UserNotFoundException::new)` — 람다보다 간결
- **Spring 프레임워크와 동일 패턴**: Spring Security도 `BadCredentialsException`, `UsernameNotFoundException` 등 개별 클래스 방식

### 👎 감수한 트레이드오프

- **파일 수 증가**: ErrorCode 1개 + Exception N개. 도메인당 3~6개 수준이면 감수 가능
- **"예외 클래스 과한가" 의문 여지**: 단순 validation은 공통 `COMMON-001`로 처리해서 증가 속도 조절

### 예외 클래스 증식 방지 원칙

> **"비즈니스 의미가 다른 것만 쪼갠다."**

| 쪼갠다 | 쪼개지 않는다 |
|---|---|
| `UserNotFoundException` — 사용자 없음 | 단순 validation (`@NotBlank`, `@Size`) |
| `DuplicateEmailException` — 이메일 중복 | → 공통 `COMMON-001`로 처리 |
| `CancelTimeExceededException` — 취소 시간 초과 | 범용 입력값 오류 |

**기준**: `catch`해서 다른 행동을 해야 하거나, 로그에서 별도 추적이 필요하면 쪼갠다. 수십 개가 되고 있다면 도메인을 더 분리해야 한다는 신호.

---

## 적용 범위

- [ ] 모든 도메인 예외는 `{도메인}/domain/exception/` 하위에 개별 Exception 클래스로 작성
- [ ] `{Domain}ErrorCode` enum 유지 (클라이언트 코드 분기 / 메시지 관리용)
- [ ] 모든 예외 클래스는 `common/exception/BaseException` 상속
- [ ] `GlobalExceptionHandler`는 `BaseException`만 catch (도메인 추가 시 수정 불필요)
- [ ] 공통 예외(인증/인가/404 등)는 `common/exception/CommonException` + `CommonErrorCode` 사용, 도메인마다 새로 만들지 않음

---

## 상세 문서

이 ADR은 결정 기록 버전입니다. **구체적 구현 방법·진화 과정·코드 예시는 아래 문서에 상세히 정리됨.**

- [예외 처리 전략](../conventions/exception.md) — 3단계 진화 과정, 방식 A vs B 상세 비교, 새 예외 추가 체크리스트
- [공통 기반 사용 가이드 #4. 예외 던지는 방법](../architecture/common-foundation.md#4-예외-던지는-방법) — 실전 사용 예시 (ErrorCode 작성 → 개별 예외 → 서비스에서 throw)
- [팀 FAQ](../conventions/faq.md#예외-처리) — "왜 단일 Exception 대신 개별 클래스?" 한 줄 답변

## 관련 문서

- [패키지·계층 구조 가이드](../architecture/package-structure.md) — `{도메인}/domain/exception/` 위치 규칙
- [ADR-001: 아키텍처 스탠스](./001-layered-architecture-stance.md) — Layered 기반 예외 레이어 세분화
