# ADR-004: 엔티티 생성 패턴 통일 — 외부 `create()` + 내부 `private builder()`

- 상태: Accepted
- 날짜: 2026-04-23
- 결정자: @김재중, @임광조, @ㅎㄴ, @이재범, @신여원

---

## 맥락

현재 도메인마다 엔티티 생성 방식이 제각각이다.

- 일부 도메인: `Xxx.create(...)` 정적 팩토리 사용
- 일부 도메인: `Xxx.builder()...build()` 체인 사용
- 섞어 쓰는 도메인도 존재

외부(Service, Mapper 등) 코드 입장에서 같은 프로젝트인데도 "이 엔티티는 어떻게 만들어야 하지?"를 매번 확인해야 하는 상황. 생성 규칙(필수값 검증, 정규화, 도메인 불변식)의 적용 지점이 분산되어, 한 곳만 수정하면 될 것을 여러 곳 수정해야 하는 리스크가 있다.

팀에서 **엔티티 생성 패턴을 하나로 통일**하자는 안건이 올라왔고, 논의 결과 아래 결정에 만장일치.

### 고려한 질문

- **Q1**: 굳이 통일이 필요한가? (각자 편한대로 쓰면 되지 않나?)
- **Q2**: `create()`와 `builder()` 중 무엇이 더 나은가?
- **Q3**: 둘 다 두되, 외부엔 `create()`만 노출하고 `builder()`는 내부용(`PRIVATE`)으로 두면 어떨까?

---

## 결정

**외부에서는 `Xxx.create(...)` 정적 팩토리만 노출하고, 내부 필드 매핑은 `private builder()`를 사용한다.** (Q3 채택)

```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id @GeneratedValue
    private UUID storeId;

    private String name;
    private Long userId;
    // ...

    // 내부 필드 매핑 전용. 외부에서는 builder() 호출 불가
    @Builder(access = AccessLevel.PRIVATE)
    private Store(String name, Long userId, /* ... */) {
        this.name = name;
        this.userId = userId;
        // ...
    }

    // 외부 생성 경로: create()만 사용
    public static Store create(String name, Long userId, /* ... */) {
        // 필수값 검증 / 정규화 / 도메인 불변식은 이곳에 집중
        validateName(name);
        validateUserId(userId);

        return Store.builder()
                .name(name)
                .userId(userId)
                // ...
                .build();
    }
}
```

---

## 대안

| 대안 | 기각 이유 |
|---|---|
| **Q1. 통일하지 않음** (각자 편한대로) | 생성 규칙이 여러 곳에 흩어지고, 신규 팀원이 도메인마다 생성 방식을 매번 확인해야 함 |
| **Q2. `create()` 정적 팩토리만 사용** | 필드가 많은 엔티티에서 파라미터 순서 혼동·가독성 저하. 테스트·리팩터링 시 명명 파라미터의 이점을 포기해야 함 |
| **Q3. `create()` 공개 + `builder()` 내부** ✅ 채택 | 생성 경로를 한 곳에 집중시키면서 내부 매핑은 빌더의 가독성 이점을 동시에 확보 |

---

## 결과

### 👍 좋은 점

- **생성 규칙의 단일 진입점**: 필수값 검증·정규화·도메인 불변식을 `create()` 한 곳에서만 강제
- **외부의 우회 차단**: `builder()`가 `PRIVATE`이라 외부에서 검증을 건너뛰고 엔티티를 만들 수 없음
- **가독성 + 안정성 균형**: 외부는 명시적 정적 팩토리, 내부는 빌더의 명명 파라미터 이점
- **코드 리뷰 기준 명확**: "엔티티에 `public`/`default` 접근의 `builder()` 보이면 지적"

### 👎 감수한 트레이드오프

- **첫인상의 중복 느낌**: 처음 보면 `create()`와 `builder()`가 겹쳐 보일 수 있음 → 주석·네이밍으로 의도 명확화
- **파일 길이 증가**: 정적 팩토리 + 검증 메서드 + private 빌더가 한 파일에 공존 → 도메인별 필드 수(3~6개)를 고려하면 감수 가능

---

## 적용 범위

- **신규 엔티티**: 이 규칙을 **준수 필수**
- **기존 엔티티**: 점진적 마이그레이션. 해당 도메인 담당자가 다음 PR에서 정리
- **예외**: `OrderItem`처럼 같은 Aggregate 내부에서 Order가 직접 생성하는 경우는 package-private 접근자 허용 (필요 시 별도 ADR)

---

## 의견 요약 (논의 기록)

| 팀원 | 기존 방식 | 의견 |
|---|---|---|
| 김재중 | 이미 Q3 사용 중 | 생성 경로 단일화 + 내부 빌더 가독성을 함께 챙기는 쪽으로 제안 |
| 임광조 | `create()`만 사용 | Q3 동의 |
| ㅎㄴ | — | 외부 생성 경로를 `create()`로 두는 것에 동의 |
| 이재범 | `create()`만 사용 | 필드 많은 도메인에서 빌더의 가독성 이점을 인정, Q3 동의 |
| 신여원 | `builder()`만 사용 | 실무에서 섞어 쓰는 관행을 확인, Q3 동의 |

---

## 관련 문서

- [JPA 엔티티 설계 가이드](../architecture/jpa.md) — 엔티티 설계 원칙
- [패키지·계층 구조 가이드](../architecture/package-structure.md) — 도메인 엔티티 위치
