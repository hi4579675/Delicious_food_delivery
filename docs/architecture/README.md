# 아키텍처 / 코드 구조 문서

**어떻게 코드로 표현할지** — 패키지 구조, 계층 역할, 공통 모듈, JPA 엔티티 규칙.

| 문서 | 한 줄 요약 |
|---|---|
| [패키지·계층 구조 가이드](./package-structure.md) | Layered + Package by Feature, 계층별 역할, 코드 배치 규칙 |
| [JPA 엔티티 설계 가이드](./jpa.md) | 연관관계 매핑, 순환참조/영속성 규칙, Soft Delete |
| [공통 기반 사용 가이드](./common-foundation.md) | BaseEntity, ApiResponse, 예외 던지는 법, 컨트롤러 응답 |

> 🔖 이 폴더의 `infrastructure/`는 **코드 계층 이름**입니다. 배포 환경 인프라는 [operations/infrastructure.md](../operations/infrastructure.md) 참고.
