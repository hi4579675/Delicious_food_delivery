# ADR-003: 엔티티 연관관계 규칙 — 같은 도메인은 `@ManyToOne`, 다른 도메인은 FK

- 상태: Proposed
- 날짜: 2026-04-23
- 결정자: @팀 (논의 대기)

---

## 맥락

도메인이 여러 개인 상황에서 엔티티 간 연관관계를 JPA의 `@ManyToOne`으로 매핑할지, 단순 `Long`/`UUID` FK 필드로만 둘지 일관된 규칙이 필요하다.

무분별한 `@ManyToOne` 매핑은 다음과 같은 문제를 일으킨다.

- **순환참조**: A ↔ B 양방향이 얽히면 `@ToString`, JSON 직렬화에서 무한 재귀
- **N+1 / LAZY 초기화 이슈**: 도메인 경계를 넘나드는 연관관계 로딩 시 성능 문제
- **도메인 강결합**: User 엔티티가 수정되면 다른 모든 도메인이 영향받음
- **MSA 분리 불가**: 서비스를 분리하려 해도 JPA 연관관계가 실제 객체 참조라 HTTP 호출로 바꿀 수 없음

반대로 **모든 관계를 FK 필드로만** 두면 Aggregate 내부의 자연스러운 객체 모델링이 불편해진다 (예: `Order`가 자신의 `OrderItem` 컬렉션을 갖지 못하게 됨).

따라서 **도메인 경계를 기준**으로 둘을 분리하는 규칙이 필요하다.

---

## 결정

### 같은 도메인(같은 폴더) 안 — `@ManyToOne` 사용 가능

```
order/        OrderItem → Order        (같은 Aggregate, 함께 생성/삭제)
store/        Store → StoreCategory
ai/           LlmCall → Llm
```

> ⚠️ **주의**: "같은 폴더면 다 `@ManyToOne`"이 아니다.
> 실제로 **하나의 Aggregate로 함께 생성·삭제되는 관계**인지 먼저 판단하고 적용한다.

### 다른 도메인(다른 폴더) 간 — `Long`/`UUID` FK 필드만

```java
public class Order {
    private Long userId;      // Order → User    (다른 도메인)
    private UUID storeId;     // Order → Store   (다른 도메인)
    private UUID addressId;   // Order → Address (다른 도메인)
}

public class Store {
    private Long ownerId;     // Store → User    (다른 도메인)
    private UUID regionId;    // Store → Region  (다른 도메인)
}
```

- 도메인 간 관계는 **엔티티 연관관계/DB 조인이 아니라**, 서비스 레이어에서 **Repository 조회 또는 다른 도메인 Service 호출**로 풀어낸다.
- `@ManyToOne User`, `@ManyToOne Store` 같은 크로스 도메인 매핑은 **전면 금지**.

---

## 대안

| 대안 | 기각 이유 |
|---|---|
| **A. 모든 관계에 `@ManyToOne` (JPA 표준)** | 순환참조/N+1/JSON 무한 재귀 위험, 도메인 강결합, MSA 분리 불가 |
| **B. 전부 `Long`/`UUID` FK 필드만** | Aggregate 내부의 자연스러운 객체 모델링 불가 (`Order.items` 컬렉션을 못 쓰는 등) |
| **C. 같은 도메인 한정 `@ManyToOne` + 다른 도메인 FK** ✅ 채택 | Aggregate 이점은 살리고, 도메인 경계는 분명히 유지 |

---

## 결과

### 👍 좋은 점

- **순환참조 원천 차단**: 다른 도메인 사이에 `@ManyToOne`이 없으므로 양방향 얽힘이 구조적으로 불가능
- **JPA 이슈 범위 한정**: N+1, LAZY 초기화 문제는 한 도메인 내부로만 한정됨 — 디버깅·튜닝이 쉬워짐
- **`@ToString` / JSON 무한 재귀 위험 감소**: 도메인 간 객체 참조가 없으니 직렬화 사고 위험이 근본적으로 줄어듦
- **MSA 분리 대비**: User처럼 나중에 별도 서비스로 분리하고 싶은 도메인은, 지금부터 `Long userId`만 들고 가면 나중에 `findById`를 HTTP 호출로 교체하기만 하면 됨
- **도메인 경계 자동 강제**: "다른 도메인 엔티티 import"가 생기면 리뷰에서 바로 보임

### 👎 감수한 트레이드오프

- **Repository 쿼리 증가**: 다른 도메인 정보가 필요하면 해당 도메인 Service/Repository를 거쳐야 함 → 코드 조금 장황해짐
- **N+1 주의 지점 이동**: 예전엔 LAZY 로딩에서 발생하던 N+1이, 이제는 서비스에서 `for`문 안에 Repository를 호출하는 형태로 나타날 수 있음 → `findAllByXxxIdIn(...)` 같은 배치 조회 패턴으로 대응

---

## 적용 범위

### `@ManyToOne` 허용 케이스 (예시)

| From → To | 도메인 | 근거 |
|---|---|---|
| `OrderItem` → `Order` | order | 같은 Aggregate, 함께 생성·삭제 |
| `Store` → `StoreCategory` | store | 같은 도메인 폴더, 카테고리와 함께 관리 |
| `LlmCall` → `Llm` | ai | 같은 도메인 폴더, AI 요청 모델 참조 |

> 위는 예시이며, 신규 `@ManyToOne` 도입 시 "같은 도메인 폴더인가?" + "함께 생성·삭제되는 관계인가?" 두 조건을 팀 논의로 확인.

### FK 필드 강제 케이스

| From → To | FK 필드 | 도메인 |
|---|---|---|
| `Order.userId` | `Long` | order → user |
| `Order.storeId` | `UUID` | order → store |
| `Store.ownerId` | `Long` | store → user |
| `Store.regionId` | `UUID` | store → region |
| `Address.userId` | `Long` | address → user |
| `Review.orderId`, `Review.storeId` | `UUID` | review → order, store |

---

## 관련 문서

- [JPA 엔티티 설계 가이드 #2. 연관관계 매핑](../architecture/jpa.md#2-연관관계-매핑) — 세부 규칙 및 담당 도메인별 매핑표
- [패키지·계층 구조 가이드](../architecture/package-structure.md) — 도메인 폴더 경계
- [ADR-004: 엔티티 생성 패턴](./004-entity-creation-pattern.md) — 엔티티 생성 통일 규칙

