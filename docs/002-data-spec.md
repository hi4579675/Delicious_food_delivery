# 002. 데이터 명세

> **이 문서를 보면**: ERD 어디서 보는지, 테이블 규약이 뭔지, 감사 필드는 어떻게 쓰는지 파악 가능.
>
> **언제 다시 보나요**: 엔티티 만들 때, 테이블 설계 변경 시, 인덱스 추가할 때.

---

## 한 줄 요약

모든 테이블은 `p_` 접두사, PK는 `UUID` (User만 `BIGINT`), 대부분의 엔티티는 `BaseEntity` 상속으로 감사 필드 자동 관리(로그성 테이블 예외), Soft Delete 기본.

---

## 목차

- [1. ERD](#1-erd)
- [2. 테이블 목록](#2-테이블-목록)
- [3. 데이터 규약](#3-데이터-규약)
- [4. 스냅샷 필드](#4-스냅샷-필드)

---

ERD, 테이블 명세, 데이터 규약을 정의합니다.

---

## 1. ERD

**dbdiagram.io 링크**: https://dbdiagram.io/d/69e05b518089629684ac0737

---

## 2. 테이블 목록

| 번호 | 테이블 | 도메인 | PK 타입 | BaseEntity 상속 |
|:---:|--------|--------|--------|:--------------:|
| 1 | `p_user` | user | `BIGINT` | O |
| 2 | `p_region` | region | `UUID` | O |
| 3 | `p_store_category` | store | `UUID` | O |
| 4 | `p_store` | store | `UUID` | O |
| 5 | `p_address` | address | `UUID` | O |
| 6 | `p_product` | product | `UUID` | O |
| 7 | `p_order` | order | `UUID` | O |
| 8 | `p_order_items` | order | `UUID` | **X** |
| 9 | `p_payment` | payment | `UUID` | O |
| 10 | `p_review` | review | `UUID` | O |
| 11 | `p_llms` | ai | `UUID` | O |
| 12 | `p_llm_calls` | ai | `UUID` | **X** |

> **BaseEntity 상속 예외**: `p_order_items`, `p_llm_calls`는 수정/삭제가 필요 없는 로그성 테이블이므로 BaseEntity를 상속하지 않고 `created_at`, `created_by`만 별도 필드로 추가한다. (참고 문서 기준)

---

## 3. 데이터 규약

### 3-1. 테이블 이름
- 모든 테이블은 `p_` 접두사 (예: `p_user`, `p_store`)

### 3-2. PK 전략
- 기본: `UUID`
- 예외: `p_user`만 `BIGINT` (`user_id`)

### 3-3. 공통 감사 필드 (BaseEntity)

모든 도메인 엔티티는 `BaseEntity`를 상속하여 아래 필드를 자동 관리합니다.

| 필드 | DB 타입 | Java 타입 | NN | 설명 |
|------|--------|----------|:--:|------|
| `created_at` | `TIMESTAMP` | `LocalDateTime` | O | 레코드 생성 시간 |
| `created_by` | `BIGINT` | `Long` | O | 레코드 생성자 **user_id 값** |
| `updated_at` | `TIMESTAMP` | `LocalDateTime` | - | 레코드 수정 시간 |
| `updated_by` | `BIGINT` | `Long` | - | 레코드 수정자 **user_id 값** |
| `deleted_at` | `TIMESTAMP` | `LocalDateTime` | - | 레코드 삭제 시간 (Soft Delete) |
| `deleted_by` | `BIGINT` | `Long` | - | 레코드 삭제자 **user_id 값** |

> **주의**: `_by` 필드는 Java에서 **`Long` 타입**으로, **user_id 값만 저장**합니다.  
> JPA 엔티티에서 `@ManyToOne User` 관계 매핑은 **금지** — 도메인 강결합 방지 및 MSA 전환 대비.  
> `BaseEntity`는 `@MappedSuperclass` 단순 상속 구조. 자세한 정책은 [005. JPA 엔티티 설계 가이드](./005-jpa-guidelines.md#3-공통-엔티티-정책) 참고.

### 3-4. Soft Delete

- 삭제 시 물리 DELETE 대신 `deleted_at`, `deleted_by`만 기록 (서비스에서 `entity.softDelete(currentUserId)` **명시적 호출**)
- 조회 시 `deleted_at IS NULL` 조건 자동 적용 — `@SQLRestriction`만 사용
- `@SQLDelete`는 **사용하지 않음** (DELETE SQL을 자동 override하면 `deleted_by`를 기록할 수 없음)

### 3-5. 숨김 vs 삭제

- **숨김 (`is_hidden`)**: 사용자에게 안 보이지만 데이터 보존 (상품 품절/일시 중단 등)
- **삭제 (`deleted_at`)**: Soft Delete, 다시 복구 가능
- 두 필드는 **독립적**으로 동작

### 3-6. 제약 조건

| 관계 | 제약 |
|------|------|
| `p_order` ↔ `p_payment` | UNIQUE (`order_id`) - 주문당 결제 1건 |
| `p_order` ↔ `p_review` | UNIQUE (`order_id`) - 주문당 리뷰 1개 |
| `p_user` ↔ `p_user.email` | UNIQUE |

---

## 4. 스냅샷 필드

요청/주문 시점의 데이터 보존용 스냅샷 필드.

| 테이블 | 필드 | 보존 시점 |
|--------|------|----------|
| `p_order` | `delivery_address_snapshot` | 주문 시점 배송지 전체 주소 |
| `p_order_items` | `unit_price`, `line_total_price`, `product_name_snapshot` | 주문 시점 단가/합계/상품명 |
| `p_llm_calls` | `input_snapshot` (`jsonb`) | AI 요청 시점 원문 |

---

## 관련 문서

- [000. 프로젝트 개요](./000-overview.md)
- [001. 도메인 명세](./001-domain-spec.md)
- [004. 아키텍처 가이드](./004-architecture.md)
- [005. JPA 엔티티 설계 가이드](./005-jpa-guidelines.md)
