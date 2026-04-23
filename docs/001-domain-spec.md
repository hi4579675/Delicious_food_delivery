# 001. 도메인 명세

> **이 문서를 보면**: 어떤 도메인들이 있고, 누가 뭘 할 수 있고, 주문이 어떤 상태를 거치는지 파악 가능.
>
> **언제 다시 보나요**: 기능 구현 전, 권한 체크 로직 작성 시, 상태 전이 로직 작성 시.

---

## 한 줄 요약

11개 도메인으로 구성된 배달 플랫폼. 권한은 `CUSTOMER/OWNER/MANAGER/MASTER` 4단계. 주문은 `PENDING → ACCEPTED → COOKING → DELIVERING → DELIVERED → COMPLETED` 순서로 전이.

---

## 목차

- [1. 도메인 목록](#1-도메인-목록)
- [2. 도메인 관계 요약](#2-도메인-관계-요약)
- [3. 역할(액터)](#3-역할액터)
- [4. 권한별 접근 범위](#4-권한별-접근-범위)
- [5. 주문 상태 흐름](#5-주문-상태-흐름)
- [6. 도메인 주요 규칙](#6-도메인-주요-규칙)
- [7. 공통 규약](#7-공통-규약)

---

프로젝트의 주요 도메인 규칙, 권한 체계, 상태 흐름을 정의합니다.

---

## 1. 도메인 목록

| 도메인 | 주요 책임 | 대응 테이블 | 관련 요구사항 |
|--------|----------|------------|---------------|
| `common` | 공통 모듈 (설정, 응답 포맷, 예외 처리, BaseEntity) | - | 전 영역 |
| `auth` | 로그인, JWT 토큰 발급/검증 | - | 사용자 인증 |
| `user` | 회원가입, 사용자 조회/수정, 권한 관리 | `p_user` | 사용자 인증, 권한 체계 |
| `address` | 사용자 배송지 관리 | `p_address` | 주문 기능 (배송지) |
| `region` | 배달 가능 지역 관리 | `p_region` | 가게 지역 기반 검색 |
| `store` | 가게 등록/조회/검색, 카테고리 관리 | `p_store`, `p_store_category` | 가게 CRUD + Search |
| `product` | 가게의 메뉴/상품 관리 | `p_product` | 메뉴 CRUD |
| `order` | 주문 생성 및 상태 전이, 주문 아이템 관리 | `p_order`, `p_order_items` | 주문 CRUD, 상태 플로우 |
| `payment` | 주문 결제 처리 | `p_payment` | 주문 완료 처리 |
| `review` | 리뷰/평점 작성, 가게 평점 집계 | `p_review` | 리뷰 및 평점 기능 |
| `ai` | Gemini API 연동, 프롬프트 가공, 요청 이력 | `p_llms`, `p_llm_calls` | AI API 연동 |

### 요구사항 매핑

| 요구사항 | 담당 도메인 |
|----------|------------|
| 1. 모든 도메인 CRUD + Search | `user`, `store`, `product`, `order`, `review`, `address`, `region`, `payment` |
| 2. 사용자 인증 (회원가입/로그인) | `user`, `auth` |
| 2. 권한 체계 (CUSTOMER/OWNER/MANAGER/MASTER) | `user`, `common` (Security) |
| 2. 주문 상태 플로우 | `order` |
| 3. AI API 연동 (Gemini) | `ai` |
| 4. 클라우드 배포 | (인프라) |
| 5. 리뷰 및 평점 | `review`, `store` (평균 평점 집계) |
| 6. API 문서화 (Swagger) | `common` (설정), 각 도메인 (컨트롤러) |
| 7. 테스트 코드 | 각 도메인 |

---

## 2. 도메인 관계 요약

> ERD 기준. 자세한 컬럼/제약은 [002. 데이터 명세](./002-data-spec.md) 참고.

| 관계 | 카디널리티 | 설명 |
|------|:---------:|------|
| `user` ↔ `store` | 1:N | OWNER 한 명이 여러 가게 소유 |
| `user` ↔ `order` | 1:N | CUSTOMER 한 명이 여러 주문 |
| `user` ↔ `address` | 1:N | 사용자별 여러 배송지 |
| `user` ↔ `ai` (요청 로그) | 1:N | 사용자별 여러 AI 요청 |
| `region` ↔ `store` | 1:N | 지역별 여러 가게 |
| `store` ↔ `product` | 1:N | 가게별 여러 메뉴 |
| `store` ↔ `order` | 1:N | 가게별 여러 주문 |
| `store` ↔ `review` | 1:N | 가게별 여러 리뷰 (역정규화 필드 존재) |
| `order` ↔ `order_item` | 1:N | 주문별 여러 주문 상품 |
| `order` ↔ `review` | 1:1 | 주문당 리뷰 1개 (UNIQUE) |
| `order` ↔ `payment` | 1:1 | 주문당 결제 1건 (UNIQUE) |
| `product` ↔ `order_item` | 1:N | 메뉴별 여러 주문 상품 |

---

## 3. 역할(액터)

| 역할 | 코드 | 설명 |
|------|------|------|
| 고객 | `CUSTOMER` | 주문 생성, 본인 주문 조회, 리뷰 작성 |
| 가게 주인 | `OWNER` | 본인 가게/메뉴/주문 관리, 주문 상태 변경 |
| 서비스 담당자 | `MANAGER` | 모든 가게/주문 관리 권한 |
| 최종 관리자 | `MASTER` | 전체 권한 + MANAGER 관리(생성/삭제) |

---

## 4. 권한별 접근 범위

| 기능 | CUSTOMER | OWNER | MANAGER | MASTER |
|------|:---:|:---:|:---:|:---:|
| 가게 등록 | X | O | X | O |
| 가게 수정/삭제 | X | O (본인) | O | O |
| 메뉴 등록/수정/삭제 | X | O (본인 가게) | O | O |
| 주문 생성 | O | X | X | X |
| 주문 상태 변경 | X | O (본인 가게) | O | O |
| 주문 취소 (5분 이내) | O (본인) | X | X | O |
| 리뷰 작성 | O (본인 주문) | X | X | X |
| 사용자 조회/관리 | X | X | O | O |
| MANAGER 생성/삭제 | X | X | X | O |
| 카테고리/지역 관리 | X | X | O | O |

---

## 5. 주문 상태 흐름

### 상태 전이도

```
PENDING (주문요청, CUSTOMER)
  ├─→ CANCELED (CUSTOMER 5분 이내 / MASTER)
  └─→ ACCEPTED (주문수락, OWNER)
        ├─→ CANCELED (MASTER)
        └─→ COOKING (조리완료, OWNER)
              → DELIVERING (배송수령, OWNER)
                → DELIVERED (배송완료, OWNER)
                  → COMPLETED (주문완료, OWNER)
```

### 상태별 정의

| 상태 | 의미 | 변경 주체 |
|------|------|----------|
| `PENDING` | 주문 요청 접수 | CUSTOMER (생성) |
| `ACCEPTED` | 가게가 주문 수락 | OWNER |
| `COOKING` | 조리 완료 | OWNER |
| `DELIVERING` | 배송 수령/진행 중 | OWNER |
| `DELIVERED` | 배송 완료 | OWNER |
| `COMPLETED` | 주문 완료 | OWNER |
| `CANCELED` | 취소됨 | CUSTOMER (5분 이내), MASTER |

### 전이 규칙

- `CUSTOMER`는 주문 생성(`PENDING`) 및 취소(5분 이내)만 가능
- `OWNER`는 순서대로만 상태 변경 가능 (역방향 불가)
- `MANAGER`, `MASTER`는 모든 상태 변경 가능
- `CANCELED` 상태는 `PENDING` 또는 `ACCEPTED`에서만 전이 가능

---

## 6. 도메인 주요 규칙

### 6-1. 사용자

- `email`: 이메일 형식, 로그인 식별자
- `name`: 2자 이상 20자 이하 (중복 허용 닉네임)
- `MANAGER`는 `MASTER`만 생성 가능 (권한 매트릭스 참고)

### 6-2. 주문

- 주문 취소 가능 시간: 생성 후 **5분 이내**
- 주문 상태 변경은 역방향 불가

### 6-3. 리뷰

- 주문 및 배송이 완료된 이후 작성 가능
- 본인이 주문한 건에 대해서만 작성 가능
- 평점 범위: 1 ~ 5점
- 가게 평균 평점은 `p_store.avg_rating`에 역정규화 저장 (갱신 방식은 구현 시 확정)

### 6-4. 가게 / 메뉴

- 가게는 `OWNER` 한 명이 소유
- 메뉴는 가게에 속함
- 가게는 지역(`region`), 카테고리(`store_category`)에 속함

### 6-5. AI 연동

- 입력 텍스트는 100자로 제한
- 요청 시 "답변을 최대한 간결하게 50자 이하로" 프롬프트 자동 삽입
- AI 요청 이력은 `p_llm_calls` 테이블에 저장

---

## 7. 공통 규약

### 7-1. 엔티티 공통

- 엔티티는 `BaseEntity`를 상속하여 감사 필드(`created_at/by`, `updated_at/by`, `deleted_at/by`) 자동 관리, 단 로그성 테이블은 제외
- 삭제는 Soft Delete 방식 (`deleted_at` 기록)
- 숨김(`is_hidden`)과 삭제(`deleted_at`)는 별개 필드

### 7-2. 인증/인가

- 매 요청 시 JWT 검증 + DB 권한 재검증
- 엔드포인트별 접근 권한은 `3. 역할(액터)` 및 `4. 권한별 접근 범위` 참고

### 7-3. Search / 페이징 규칙

모든 도메인의 Search API는 아래 규칙을 따릅니다.

- **페이지 크기**: `10`, `30`, `50` 중 하나만 허용
- 위 3가지가 아닌 값은 **기본 10건**으로 고정
- **정렬 기본값**: 생성일(`createdAt`) 내림차순
- 조회 방식: Page 또는 Slice, 쿼리 파라미터(`@RequestParam`) 사용

---

## 관련 문서

- [000. 프로젝트 개요](./000-overview.md)
- [002. 데이터 명세](./002-data-spec.md)
- [004. 아키텍처 가이드](./004-architecture.md)
- [005. JPA 엔티티 설계 가이드](./005-jpa-guidelines.md)
- [006. 팀 협업 컨벤션](./006-conventions.md)
