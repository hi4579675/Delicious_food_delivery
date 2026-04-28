# ADR-005: 문자열 필드 공백 처리 — `nullable=false`는 거부, `nullable=true`는 null 정규화

- 상태: Accepted
- 날짜: 2026-04-23
- 결정자: @이재범 (제안), @ㅎㄴ, @김재중, @김대영

---

## 맥락

문자열 필드에 **공백 문자만(`"   "`)** 들어왔을 때 어떻게 처리할지 규칙이 없었다. 선택지는 크게 세 가지.

| 선택지 | 의미 |
|---|---|
| **그대로 허용** | `"   "` 그대로 DB에 저장 |
| **정규화** | `"   "` → `null` 또는 `""`로 치환 후 저장 |
| **거부** | `"   "`는 잘못된 입력으로 간주, 검증 실패 |

### 왜 문제인가

`nullable = true` 필드에 공백 문자열이 저장되면 다음 문제가 생긴다.

```sql
-- 의도: "값이 없는 행"을 찾기
SELECT * FROM p_product WHERE description IS NULL;
-- 결과: "   " 저장된 행은 빠짐 → "값 없음"인데 잡히지 않음
```

즉, **`IS NULL` 질의의 신뢰도가 깨지고**, 애플리케이션 전반에서 "값 없음"을 일관되게 표현할 수 없다.

---

## 결정

**DTO 레벨에서 문자열 필드의 공백을 검증·정규화한다.** 필드의 nullable 여부에 따라 동작이 달라진다.

| 필드 종류 | 입력 | 처리 |
|---|---|---|
| `nullable = false` (필수) | `null` 또는 `"   "` | **검증 실패** — `@NotBlank`로 거부 |
| `nullable = true` (선택) | `"   "` | **`null`로 정규화** 후 저장 |
| `nullable = true` (선택) | `null` | 그대로 `null` 유지 |
| 모든 필드 | 값이 있음 (`"치킨 세트"`) | 앞뒤 공백은 trim 권장 |

### 구현 방식 (권장)

**Jackson 커스텀 Deserializer 전역 등록** — 역직렬화 단계에서 자동 처리.

```java
@JsonComponent
public class BlankStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;  // 공백만 → null
    }
}
```

등록 후:
- 모든 `String` 필드가 **자동 trim + blank → null** 처리됨
- `nullable = false` 필드는 `@NotBlank`가 null을 잡아내어 검증 실패

### `@NotBlank` 사용

필수 필드에는 항상 `@NotBlank`를 붙인다.

```java
public record SignupRequest(
    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 8, max = 15)
    String password,

    @NotBlank @Size(min = 2, max = 20)
    String name,

    String phone    // nullable=true, 공백이면 자동 null 정규화
) {}
```

---

## 대안

| 대안 | 기각 이유 |
|---|---|
| **A. 그대로 허용** | `IS NULL` 질의 신뢰도 붕괴, 데이터 오염, "값 없음" 표현 불일치 |
| **B. 전부 거부** (`@NotBlank` 만 사용, `nullable=true`에도 `@NotBlank`) | nullable=true의 의미가 사라짐. "값 없어도 OK"인데 공백 거부하면 과도 |
| **C. 서비스 레이어에서 개별 정규화** | 각 Service 메서드마다 `blankToNull()` 호출 → 누락 위험, 보일러플레이트 |
| **D. Jackson Deserializer 전역 + `@NotBlank`** ✅ 채택 | 한 번 등록하면 모든 필드가 자동 적용, 누락 위험 X, DTO에는 검증 어노테이션만 |

---

## 결과

### 👍 좋은 점

- **`IS NULL` 쿼리 신뢰 확보**: 공백만 있는 행이 없으므로 `WHERE col IS NULL`로 "값 없음" 정확히 탐지
- **"값 없음" 의미 통일**: 애플리케이션 전체에서 `null` 하나로 표현
- **DTO가 깔끔해짐**: `@NotBlank`만 붙이면 끝, 커스텀 검증 불필요
- **입력 앞뒤 공백 자동 정리**: `" 치킨 "` → `"치킨"` 부수 효과

### 👎 감수한 트레이드오프

- **"공백도 유효한 값"인 특수 케이스 대응 필요**: 비밀번호나 특정 토큰 등 공백을 의미 있게 쓰는 필드가 생기면 예외 처리 필요 (현재는 없음)
- **Deserializer 전역 영향**: 외부 API 응답을 DTO로 역직렬화할 때도 적용됨 → 의도치 않은 정규화 주의

---

## 적용 범위

### 신규 DTO

- 필수 필드: `@NotBlank`
- 선택 필드: 어노테이션 없이 (deserializer가 자동 null 정규화)

### 기존 DTO 마이그레이션

- 현재 DTO에 `@NotBlank`가 일관되게 붙어있지 않다면 각 도메인 담당자가 점검
- 선택 필드는 추가 작업 없이 deserializer 도입만으로 자동 반영

### 배치 적용할 것

- [ ] `common/config/` 아래 `BlankStringDeserializer` 추가 (`@JsonComponent`)
- [ ] 각 도메인 Request DTO의 필수 필드에 `@NotBlank` 일괄 점검
- [ ] (선택) 예외 응답에서 `@NotBlank` 검증 실패를 `COMMON-001` 로 내려주는 기존 [GlobalExceptionHandler](../architecture/common-foundation.md#4-예외-던지는-방법) 흐름 확인

---

## 의견 요약 (논의 기록)

| 팀원 | 의견 |
|---|---|
| 이재범 | DTO 레벨에서 whitespace-only 검증 필요. `nullable=false` 거부, `nullable=true`는 null로 간주 |
| ㅎㄴ | 정규화 추천. "null 허용 = 값 없어도 OK"인데 공백 통과하면 `IS NULL` 깨짐. 재범 의견 지지 |
| 김재중 | 동의 |
| 김대영 | null 목적에 따라 다르지만 보통 정규화 채택 |

---

## 관련 문서

- [공통 기반 사용 가이드 #4. 예외 던지는 방법](../architecture/common-foundation.md#4-예외-던지는-방법) — `@Valid` 검증 실패 자동 처리 흐름
- [예외 처리 전략](../conventions/exception.md) — `COMMON-001` 공통 에러 코드
