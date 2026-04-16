# 005. JPA 엔티티 설계 가이드

> **이 문서를 보면**: 엔티티 만들 때 어떤 규칙을 지켜야 하는지, 순환참조/영속성 문제 어떻게 피하는지 파악 가능.
>
> **언제 다시 보나요**: 엔티티 신규 작성/수정 시, 연관관계 추가 시, 리뷰에서 지적받았을 때.

---

## 한 줄 요약

**ERD는 풍부하게, JPA는 가난하게.** 단방향 `@ManyToOne` 위주, 컬렉션은 Aggregate 내부만, `BaseEntity` 순수 상속, 연관관계는 `Long` FK만.

---

## 목차

- [1. 핵심 원칙](#1-핵심-원칙)
- [2. 연관관계 매핑](#2-연관관계-매핑)
- [3. 공통 엔티티 정책](#3-공통-엔티티-정책)
- [4. 영속성 처리](#4-영속성-처리)
- [5. 실무 규칙](#5-실무-규칙)
- [6. 엔티티 작성 체크리스트](#6-엔티티-작성-체크리스트)

---

엔티티 만들 때 이 문서의 규칙을 지키면 순환참조와 영속성 문제를 대부분 예방할 수 있습니다.

---

## 담당 도메인별 확인 사항

**공통**: 모든 담당자는 섹션 `1. 핵심 원칙`, `3. 감사 필드`, `4. 영속성 처리`, `5.  규칙`, `6. 체크리스트`를 반드시 확인.

**도메인별 추가 확인 사항** (섹션 2-1, 2-2, 2-3 매핑):

| 담당 도메인 | 엔티티 연관관계 | 특이사항 |
|------------|----------------|---------|
| user | User는 다른 엔티티에서 Long(user_id)로만 참조됨 | User 엔티티 자체는 독립. 다른 도메인이 `@ManyToOne User` 쓰지 못하게 차단 |
| auth | 엔티티 관계 거의 없음 (JWT/로그인 위주) | TokenPair 등 VO만 존재. 영속성 X |
| address | Address → User (`@ManyToOne`) | user_id로 저장. User 엔티티 직접 참조 금지 |
| region | Region → ParentRegion (자기참조) | 자식 컬렉션(`children`) 열지 않음. 쿼리로 조회 |
| store_category | StoreCategory → ParentCategory (자기참조) | region과 동일 패턴 |
| store | Store → Region, StoreCategory (`@ManyToOne`) | `reviews`, `products` 컬렉션 열지 않음 |
| product | Product → Store (`@ManyToOne`) | `orderItems` 컬렉션 열지 않음 |
| order | Order → Store (`@ManyToOne`), Order ↔ OrderItem (Aggregate) | **예외적으로 `Order.items` 컬렉션 허용** (Aggregate 내부) |
| payment | Payment → Order (`@ManyToOne`) | Order가 `payments` 컬렉션 갖지 않음 |
| review | Review → Order, Store (`@ManyToOne`) | Store가 `reviews` 컬렉션 갖지 않음 |
| ai | LlmCall → Llm, Product (`@ManyToOne`) | 별도 Aggregate 없음 |

---

## 1. 핵심 원칙

> "ERD는 풍부하게, JPA 객체 모델은 가난하게."

- DB의 FK 수와 JPA 연관관계 수를 같게 만들 필요 없음
- 단방향 ManyToOne 위주로 설계 (양방향은 최소화)
- 컬렉션(`@OneToMany`)은 Aggregate 내부에서만 사용
- 조회는 연관관계 탐색 대신 Repository 쿼리로 해결

---

## 2. 연관관계 매핑

### 2-1. 엔티티로 매핑할 관계 (ManyToOne 단방향)

| From → To | 이유 |
|-----------|------|
| OrderItem → Order, Product | 항목은 주문/상품 없이 존재 불가 |
| Payment → Order | 결제는 주문 종속 |
| Review → Order, Store | 리뷰 작성 가능 여부 확인 |
| Address → User | 주소는 유저 소유 |
| Store → Region, StoreCategory | 가게 지역/카테고리 |
| Product → Store | 상품은 가게 소유 |
| Region → ParentRegion (자기참조) | 트리 구조, 부모만 |
| StoreCategory → ParentCategory (자기참조) | 트리 구조, 부모만 |
| LlmCall → Llm, Product | AI 호출 이력 |

### 2-2. 열지 않을 컬렉션 (Repository 쿼리로 조회)

| 엔티티 | 안 여는 필드 | 대신 쓸 메서드 |
|--------|-------------|---------------|
| Order | items | `OrderItemRepository.findByOrderId()` |
| Store | reviews | `ReviewRepository.findByStoreId()` |
| Store | products | `ProductRepository.findByStoreId()` |
| User | addresses | `AddressRepository.findByUserId()` |
| User | orders | `OrderRepository.findByUserId()` |
| Region | children | `RegionRepository.findByParentId()` |
| StoreCategory | children | `StoreCategoryRepository.findByParentId()` |

### 2-3. 예외: Aggregate 내부는 컬렉션 허용

`Order`와 `OrderItem`은 같은 Aggregate로, 함께 생성/삭제되므로 컬렉션을 예외적으로 허용합니다.

```java
@Entity
public class Order extends BaseEntity {

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
```

---

## 3. 공통 엔티티 정책

본 정책은 **서로 독립된 두 결정**으로 구성된다. 한쪽이 다른 쪽을 강제하지 않으며, 각각 별도로 선택 가능하다.

### 3-1. 두 축의 독립성

| 축 | 무엇을 정하는가 | 우리 선택 |
|----|----------------|----------|
| **축 1. BaseEntity 구현 방식** | 엔티티 공통 필드를 어떻게 구성할지 | **순수 상속 (A)** |
| **축 2. 연관관계 매핑 방식** | 엔티티 간 관계를 어떻게 표현할지 | **`Long` FK만 저장** |

두 축은 서로 간섭하지 않는다. 이론상 4가지 조합이 모두 가능하지만, 우리는 **A + Long FK** 를 채택한다.

- 축 1이 B(하이브리드)여도 축 2는 Long FK일 수 있음
- 축 1이 A(순수 상속)여도 축 2는 `@ManyToOne` 일 수 있음
- 혼동 주의: "VO 캡슐화(축 1 B)"는 감사 필드(`createdAt`, `deletedAt` 등)를 값 객체로 묶는 것일 뿐, 연관관계(축 2) 얘기가 아님

### 3-2. 축 1 — BaseEntity 구현 방식: 순수 상속 (A)

**규칙**

- 모든 도메인 엔티티는 `common/model/BaseEntity`를 상속한다
- `BaseEntity`는 `createdAt`, `updatedAt`, `deletedAt` 및 `createdBy`, `updatedBy`, `deletedBy`를 **직접 필드로 보유**한다
- `@Embeddable` VO 분리는 하지 않는다

**대안 비교**

| 옵션 | 특징 | 채택 여부 |
|------|------|:--------:|
| **A. 순수 상속** | `BaseEntity`에 필드 직접 선언. 가장 단순, 한국 실무 표준 | 채택 |
| B. 하이브리드 | `BaseEntity` + `@Embeddable` VO (CreateAudit / UpdateAudit / DeleteAudit) | 미채택 |
| C. 순수 VO | 상속 없이 엔티티마다 VO 조합 | 미채택 |

**채택 이유**

- 문서 전반의 스탠스("정통 DDD/헥사고날 적용 X", "Value Object 학습 X")와 일관
- 팀 온보딩 문서의 "평소 쓰던 구조" 원칙과 일관
- 한국 실무/레퍼런스 자료가 가장 많아 팀원이 막혔을 때 해결 속도가 빠름
- 코드 리뷰 기준이 명확함 (BaseEntity를 상속했는지 여부만 확인)

### 3-3. 축 2 — 연관관계 매핑 방식: `Long` FK

**규칙**

- 엔티티 간 연관관계는 **`Long` FK 필드로만 저장**한다
- `@ManyToOne User`, `@ManyToOne Store` 같은 **JPA 연관관계 매핑은 금지**한다
- 감사 필드의 `_by` 역시 `Long` 타입 (user_id 값만 저장)

**채택 이유**

- 도메인 간 강결합 방지 — User 엔티티 수정이 모든 엔티티에 영향 주지 않음
- MSA 전환 시 도메인 단위 분리 용이
- 연관 엔티티 로딩으로 인한 N+1, 순환참조, 무한 재귀(toString/JSON) 문제 원천 차단

### 3-4. 필수 설정

- `@SpringBootApplication` 혹은 별도 `@Configuration` 클래스에 `@EnableJpaAuditing` 선언
- `AuditorAware<Long>` 빈 등록 — 현재 로그인 사용자 ID를 제공하여 `@CreatedBy` / `@LastModifiedBy`를 자동 채움
- 권장 위치: `common/config/jpa/JpaAuditingConfig.java`

### 3-5. 코드 예시

```java
// BaseEntity (축 1: 순수 상속)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;

    private Long deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

// 도메인 엔티티 (축 1: 상속 + 축 2: Long FK)
@Entity
public class Order extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID orderId;

    private Long userId;    // FK Long만 (@ManyToOne 금지)
    private UUID storeId;   // FK Long/UUID만

    private OrderStatus status;
    // ...
}
```

---

## 4. 영속성 처리

### 4-1. LAZY 기본

모든 `@ManyToOne`, `@OneToOne`, `@OneToMany`는 반드시 `LAZY`로 설정합니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
private Order order;
```

EAGER은 N+1 문제와 무한 조인을 유발하므로 사용 금지.

### 4-2. Cascade는 Aggregate 내부만

```java
// 허용: Order-OrderItem (같은 Aggregate)
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)

// 금지: Store-Review, User-Address 등 서로 다른 도메인
```

### 4-3. 명시적 저장 권장

Cascade에 의존하여 자동 저장을 기대하기보다, 서비스 계층에서 명시적으로 저장하는 것이 예측 가능합니다.

```java
// 지양: Cascade로 자동 저장 기대
orderRepository.save(order);

// 권장: 명시적으로 저장
Order saved = orderRepository.save(order);
orderItemRepository.saveAll(buildItems(saved));
```

### 4-4. Soft Delete

삭제는 DB FK Cascade가 아닌 Soft Delete로 처리합니다.

```java
@Entity
@Table(name = "p_store")
@SQLDelete(sql = "UPDATE p_store SET deleted_at = now() WHERE store_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Store extends BaseEntity { }
```

또는 서비스에서 명시적으로 처리:

```java
@Transactional
public void deleteStore(UUID storeId) {
    Store store = storeRepository.findById(storeId).orElseThrow();
    store.softDelete(currentUserId);
}
```

---

## 5.  규칙

### 5-1. 엔티티를 API 응답으로 직접 반환 금지

```java
// 권장: DTO 변환
return StoreResponse.from(store);
```

Jackson 순환참조 이슈를 원천 차단합니다.

### 5-2. `@ToString`은 연관 엔티티 제외

```java
@ToString(exclude = {"store", "user"})
public class Order { }
```

Lombok의 무한 재귀를 방지합니다.

### 5-3. `equals` / `hashCode`는 ID 기반

연관 엔티티를 비교 로직에 포함하지 않습니다.

---

## 6. 엔티티 작성 체크리스트

새 엔티티를 만들 때 아래 항목을 모두 확인합니다.

- [ ] `BaseEntity`를 상속했는가
- [ ] 모든 연관관계가 `LAZY`인가
- [ ] 양방향 대신 단방향 `@ManyToOne`으로 설계했는가
- [ ] 컬렉션은 Aggregate 내부만 사용했는가 (Order.items 외 금지)
- [ ] 다른 도메인의 엔티티를 import하지 않았는가 (감사 필드는 Long)
- [ ] `@ToString`에 연관 엔티티를 exclude했는가
- [ ] Soft Delete를 적용했는가
- [ ] API 응답은 DTO로 변환했는가

---

## 관련 문서

- [아키텍처 가이드](./architecture.md)
- [팀 협업 컨벤션](./conventions.md)
