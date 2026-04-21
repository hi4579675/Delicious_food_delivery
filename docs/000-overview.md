# 000. 프로젝트 개요

> **이 문서를 보면**: 프로젝트가 뭘 만드는지, 어떤 기술을 쓰는지, 핵심 규약이 뭔지 파악 가능.

---

## 한 줄 요약

**광화문 지역 배달 주문 관리 플랫폼**

---

## 1. 프로젝트 정보

| 항목 | 내용 |
|------|------|
| 주제 | 배달 주문 관리 플랫폼 (00의 민족 유사 서비스) |
| 목표 | 광화문 근처 음식점의 배달 주문, 결제, 주문 내역 관리 |
| 작성일 | 2026-04-16 |
| 아키텍처 | Monolithic · Layered (Controller → Service → Repository) |

---

## 2. 서비스 범위

| 항목 | 정책 |
|------|------|
| 운영 지역 | 초기 광화문 근처 한정, 향후 지역 확장 가능하도록 설계 |
| 주문 유형 | 온라인 주문만 가능 |
| 결제 방식 | 카드 결제만 가능 (PG 실제 연동 없이 DB 저장만) |
| 데이터 보존 | 모든 데이터 Soft Delete (`deleted_at` 기록) |
| 숨김 처리 | 상품(메뉴) 숨김과 삭제는 별도 필드 (`is_hidden` ≠ `deleted_at`) |
| AI 연동 | Google Gemini API로 상품 설명 자동 생성 |

---

## 3. 기술 스택 요약

| 구분 | 기술 |
|------|------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (jjwt) |
| ORM | JPA / Hibernate |
| DB | PostgreSQL 15+ (Docker) |
| Cache / Session | Redis 7+ (Docker) |
| Build | Gradle 8.x |
| API 문서 | springdoc-openapi (Swagger) |
| AI | Google Gemini 1.5 Flash |
| 컨테이너 | Docker + docker-compose |
| CI/CD | GitHub Actions |
| 서버 | AWS EC2 t2.micro (Ubuntu 22.04) |

> 상세 버전/설정: [003. 인프라 명세](./003-infrastructure.md)

---

## 4. 핵심 규약 (반드시 숙지)

### 데이터

- 모든 테이블 `p_` 접두사, PK는 `UUID` (`p_user`만 예외 - `BIGINT`)
- 대부분의 엔티티는 `BaseEntity` 상속 — `createdAt`, `updatedAt`, `deletedAt` + `_by` 필드
  - 단 수정/삭제 없는 로그성 테이블은 예외
- 감사 필드 `_by`는 Java `Long` (user_id 값만 저장)
- **`@ManyToOne User` 금지** — 도메인 강결합 방지

### 삭제 / 숨김

- 삭제는 **Soft Delete** (`deleted_at` 기록)
- 숨김(`is_hidden`)과 삭제(`deleted_at`)는 **별개 필드**

### 보안

- 매 요청 시 **JWT + DB 권한 재검증**
- 비밀번호는 **BCrypt** 해시 저장

### 비즈니스 규칙

- 주문 취소는 생성 후 **5분 이내**만 허용
- 결제는 **CARD 단일**, PG 미연동 (DB 저장만)
- AI 요청 텍스트 **100자 제한**, 응답 **50자 이하** 프롬프트 자동 삽입

---

## 다음 읽기

- 권한/도메인 규칙 → [001. 도메인 명세](./001-domain-spec.md)
- ERD / 테이블 상세 → [002. 데이터 명세](./002-data-spec.md)
- 배포 / 환경변수 → [003. 인프라 명세](./003-infrastructure.md)
