# 설계 문서 인덱스

> 이 폴더는 배달 플랫폼 프로젝트의 **모든 설계·운영 문서**를 모아둔 곳입니다.
> 주제별 폴더 구조로 정리되어 있고, 각 폴더 안에 카테고리 인덱스가 별도로 있습니다.

---

## 빠르게 찾기

| 상황 | 읽을 문서 |
|---|---|
| 처음 봐요 | [프로젝트 개요](./overview.md) → [도메인 명세](./design/domain.md) → [패키지·계층 구조](./architecture/package-structure.md) |
| **"DTO 어디 두지? Command는?"** 자주 묻는 것 | **[팀 FAQ](./conventions/faq.md)** |
| 도메인/권한/주문 상태 | [도메인 명세](./design/domain.md) |
| ERD, 테이블 규약 | [데이터 명세](./design/data.md) |
| "어디에 어떤 코드 놓지?" | [패키지·계층 구조 가이드](./architecture/package-structure.md) |
| 엔티티 만들 때 규칙 | [JPA 엔티티 설계 가이드](./architecture/jpa.md) |
| BaseEntity / ApiResponse / 예외 사용법 | [공통 기반 사용 가이드](./architecture/common-foundation.md) |
| 예외 클래스 설계 근거 | [예외 처리 전략](./conventions/exception.md) |
| EC2/네트워크/보안그룹 | [인프라 명세](./operations/infrastructure.md) |
| CI/CD / Dockerfile / 롤백 | [배포 파이프라인](./operations/deployment.md) |
| PR/커밋/브랜치 규칙 | [팀 협업 컨벤션](./conventions/team.md) |

---

## 문서 구조

```
docs/
├── overview.md              # 프로젝트 개요
├── design/                  # 📐 설계 (What)
├── architecture/            # 🏛 코드 구조 (How to code)
├── operations/              # ⚙️ 운영·배포 (How to deploy)
├── conventions/             # 📋 팀 규칙
├── adr/                     # 의사결정 기록
└── troubleshooting/         # 이슈 해결 기록
```

### 📐 design/ — 설계 (What / Why)
서비스가 **무엇을** 하는지, **어떤 데이터**를 다루는지.

- [도메인 명세](./design/domain.md)
- [데이터 명세](./design/data.md)

### 🏛 architecture/ — 코드 구조 (How to code)
**어떻게 코드로 표현할지** — 패키지·계층·공통 모듈.

- [패키지·계층 구조 가이드](./architecture/package-structure.md)
- [JPA 엔티티 설계 가이드](./architecture/jpa.md)
- [공통 기반 사용 가이드](./architecture/common-foundation.md)

### ⚙️ operations/ — 운영·배포 (How to deploy/run)
**어디에 어떻게 올라가고, 어떻게 배포되는지**.

- [인프라 명세](./operations/infrastructure.md)
- [배포 파이프라인](./operations/deployment.md)

### 📋 conventions/ — 협업 규칙
팀이 함께 지키는 규칙, 자주 묻는 Q&A.

- [팀 협업 컨벤션](./conventions/team.md)
- [예외 처리 전략](./conventions/exception.md)
- [팀 FAQ](./conventions/faq.md)

### 📚 누적 기록

- [`adr/`](./adr/) — 의사결정 기록 (Architecture Decision Records)
- [`troubleshooting/`](./troubleshooting/) — 이슈 해결 기록

---

## ⚠️ 용어 주의 — `infrastructure`는 두 가지 의미

| 위치 | 의미 |
|---|---|
| `architecture/` 문서 내 `infrastructure/` | **코드 계층 이름** (커스텀 쿼리 구현체, 외부 API 클라이언트 위치) |
| `operations/infrastructure.md` | **배포 환경** (EC2, Docker, 네트워크) |

이름만 같고 완전히 다른 개념입니다.

---

## 업데이트 규칙

- 모든 설계 변경은 **해당 문서에 반영**
- 문서 변경은 PR 설명에 링크 포함 (예: `docs/design/domain.md#3-권한별-접근-범위`)
- 새 문서 추가 시 **해당 폴더 README + 이 인덱스**에 한 줄 요약 추가
