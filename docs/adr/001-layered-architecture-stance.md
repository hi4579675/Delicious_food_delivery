# ADR-001: 아키텍처 스탠스 — Layered + Package by Feature, DDD 용어만 차용

- 상태: Accepted
- 날짜: 2026-04-23
- 결정자: @팀

---

## 맥락

프로젝트 아키텍처 스탠스를 정해야 하는데, 상충하는 요구가 있었다.

### 튜터 가이드 인용

> **"Layered Architecture: Controller, Service, Repository 계층으로 구성된 클린 아키텍처 권장"**
>
> **"헥사고날 아키텍처 등 기타 아키텍처의 경우 관심이 있는 경우 학습은 해보되 프로젝트에 적용은 하지 않도록 합니다."**

### 팀 상황

- 팀은 Clean Architecture를 지향하고 싶었으나, 가이드가 헥사고날 전면 적용을 제한
- 그런데 **튜터 예시 자체가 `presentation / application / domain / infrastructure` 4계층** — 이미 DDD/헥사고날 영향을 받은 구조
- 다음 프로젝트가 **MSA**라서 도메인 경계를 미리 연습하고 싶음
- 한편 팀원들은 Value Object, Aggregate Root 같은 DDD 전술 패턴을 아직 학습하지 않은 상태

즉 **"가이드 준수" + "다음 프로젝트(MSA) 대비" + "학습 부담 최소화"** 세 가지를 동시에 만족시킬 절충안이 필요했다.

---

## 결정

### 1) 아키텍처: **Layered Architecture + Package by Feature**

- 튜터 가이드의 "Layered Architecture 권장"을 따름
- 패키지 구조는 **Package by Feature**(도메인별 분리) — 튜터 예시와 동일
- `controller/` 최상위가 아닌 `user/`, `order/` 등 **도메인 최상위**로 그루핑

### 2) 계층 이름: **DDD 용어 차용**

4계층 이름은 DDD에서 차용한다.

```
{도메인}/
├── presentation/     → 컨트롤러 + DTO
├── application/      → 서비스
├── domain/           → JPA 엔티티, Repository 인터페이스, 도메인 예외
└── infrastructure/   → 커스텀 쿼리 구현체, 외부 API 클라이언트 (필요 시)
```

### 3) 내부 패턴: **DDD 전술 패턴은 적용하지 않음**

Aggregate Root 엄격 설계, Value Object, Domain Event 등은 **차용한 폴더 이름 외에는 적용하지 않는다.** (상세는 아래 "의도적으로 적용하지 않은 것" 참고)

---

## 대안

| 대안 | 기각 이유 |
|---|---|
| **A. 정통 Layered** (Controller / Service / Repository 3계층, 도메인 분리 X) | 다음 프로젝트(MSA) 대비 도메인 경계 연습이 안 됨 |
| **B. 정통 DDD / 헥사고날** (Aggregate Root, VO, Port/Adapter 전면 적용) | 튜터 가이드 제한, 팀 학습 비용 과다, 학습 프로젝트 범위 초과 |
| **C. Layered + Package by Feature + DDD 용어 차용** ✅ 채택 | 가이드 준수 + MSA 대비 + 학습 부담 최소화의 절충점 |

---

## 의도적으로 **적용하지 않은 것**

폴더 이름만 DDD에서 빌려왔을 뿐, 아래 전술 패턴은 본 프로젝트에서 쓰지 않는다.

| 패턴 | 왜 안 하나 | 언제 도입 고려 |
|---|---|---|
| **Aggregate Root 엄격 설계** | 튜터 가이드에서 DDD 전술 패턴 적용 제한. 현재는 "같은 도메인 내 생성/삭제 공유" 정도의 느슨한 Aggregate 개념만 ([ADR-003](./003-entity-relationship-rules.md)) | 복잡한 도메인 불변식이 늘어날 때 |
| **Value Object** (`@Embeddable` VO) | 팀 학습 비용이 크고, 프로젝트 스탠스와 불일치. `BaseEntity`도 VO 분리 없이 순수 상속 ([JPA 가이드 3-2](../architecture/jpa.md#3-2-축-1--baseentity-구현-방식-순수-상속-a)) | 값 중심 타입(Money, Email 등) 재사용이 필요할 때 |
| **Domain Event + Listener** | MSA 전환 시 본격 학습 예정. 현재는 Service 직접 호출로 충분 | MSA 분리 시점 |
| **Internal API** (모듈 간 통신) | 지금은 다른 도메인 Service 직접 주입으로 충분 | MSA 분리 시점 (이때는 HTTP/gRPC로 전환) |
| **Domain Service / Policy 폴더** | 비즈니스 로직은 `application/` 서비스에서 처리. 별도 `policy/` 폴더 X | 여러 Aggregate 걸친 복잡한 규칙이 늘어날 때 |

---

## 결과

### 👍 좋은 점

- **가이드 준수**: "헥사고날 전면 적용 X" 조건 충족
- **MSA 대비**: 도메인별 폴더 경계가 이미 잡혀 있어, 추후 서비스 분리가 자연스러움
- **학습 부담 최소**: DDD 전술 패턴을 전부 공부하지 않아도 시작 가능 — "평소 쓰던 Service 구조"로 대부분 해결
- **일관된 코드 위치**: "이 코드 어디에 두지?" 답을 빠르게 — 도메인 + 계층 2단계로 결정

### 👎 감수한 트레이드오프

- **"헥사고날인 듯 아닌 듯" 인상**: 용어만 차용하다 보니 모순되어 보일 수 있음 — 문서 전반에 스탠스를 명시해서 완화 ([패키지·계층 구조 가이드](../architecture/package-structure.md#의존성-가이드라인))
- **엄격한 DIP 부재**: `infrastructure → domain` 의존이 있지만 의존성 역전을 엄격히 적용하지 않음 — 관행상 허용으로 완화
- **용어 충돌**: `infrastructure/`가 **코드 계층 이름**과 **배포 환경**(003 문서) 두 뜻으로 쓰임 — 각 문서에 용어 주의 박스로 구분

---

## 파생 결정

본 ADR이 기반이 되어 다음 결정들이 이어진다.

- **DTO 위치**: Request/Response 모두 `{도메인}/presentation/dto/` ([팀 FAQ](../conventions/faq.md#-dto))
- **Command DTO 미사용**: Service가 Request DTO 직접 수용 (같은 근거)
- **의존성 규칙 완화**: 엄격한 allow/deny 대신 가이드라인 톤 ([패키지·계층 구조 가이드](../architecture/package-structure.md#의존성-가이드라인))
- **연관관계 규칙**: 같은 도메인 `@ManyToOne`, 다른 도메인 FK ([ADR-003](./003-entity-relationship-rules.md))
- **BaseEntity 설계**: VO 분리 없는 순수 상속 (A안) ([JPA 가이드 3-2](../architecture/jpa.md#3-2-축-1--baseentity-구현-방식-순수-상속-a))

---

## 관련 문서

- [패키지·계층 구조 가이드](../architecture/package-structure.md) — 본 결정의 구현 가이드
- [JPA 엔티티 설계 가이드](../architecture/jpa.md) — 엔티티 레벨 구현 규칙
- [ADR-003: 엔티티 연관관계 규칙](./003-entity-relationship-rules.md)
- [팀 FAQ](../conventions/faq.md) — "헥사고날 아키텍처인가요?" 답변
