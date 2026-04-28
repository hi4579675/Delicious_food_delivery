# ADR (Architecture Decision Records)

이 폴더는 프로젝트의 주요 기술/설계 의사결정을 기록합니다.
"무엇을 정했는가"는 설계 문서에, **"왜 그렇게 정했는가"**는 여기에 남깁니다.

---

## 목록

| 번호 | 제목 | 상태 | 날짜 |
|:---:|------|:----:|------|
| 001 | [아키텍처 스탠스 — Layered + Package by Feature, DDD 용어만 차용](./001-layered-architecture-stance.md) | Accepted | 2026-04-23 |
| 002 | [예외 처리 전략 — 도메인별 ErrorCode + 개별 Exception 클래스](./002-exception-handling-strategy.md) | Accepted | 2026-04-23 |
| 003 | [엔티티 연관관계 규칙 — 같은 도메인 `@ManyToOne`, 다른 도메인 FK](./003-entity-relationship-rules.md) | Proposed | 2026-04-23 |
| 004 | [엔티티 생성 패턴 — 외부 `create()` + 내부 `private builder()`](./004-entity-creation-pattern.md) | Accepted | 2026-04-23 |
| 005 | [문자열 필드 공백 처리 — `nullable=false` 거부 / `nullable=true` null 정규화](./005-blank-string-normalization.md) | Accepted | 2026-04-23 |
| 006 | [AI 상품 설명 — "설정"과 "상태" 필드 분리](./006-ai-description-field-design.md) | Proposed | 2026-04-23 |

---

## 작성 규칙

- 파일명: `{번호}-{kebab-case-제목}.md` (예: `002-redis-caching.md`)
- 번호는 3자리, 순차 증가
- 새 ADR 추가 시 위 표에 한 줄 추가
- 폐기된 결정은 **삭제하지 말고** 상태만 `Deprecated`로 변경하고, 대체 ADR 링크 추가
- 상태 종류: `Proposed` / `Accepted` / `Deprecated` / `Superseded by ADR-XXX`

---

## 템플릿

새 ADR 작성 시 아래를 복사해서 사용하세요.

```markdown
# ADR-XXX: 결정 제목

- 상태: Proposed
- 날짜: YYYY-MM-DD
- 결정자: @팀원1, 

## 맥락
어떤 상황에서 어떤 결정을 해야 했는지

## 결정
무엇을 선택했는지

## 대안
고려했지만 버린 선택지와 그 이유

## 결과
좋은 점 / 감수한 트레이드오프
```