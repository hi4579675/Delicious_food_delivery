#  팀 협업 컨벤션

> 팀원 전원이 지켜야 할 협업 규칙 모음

---

## 📌 Issue

GitHub에서 이슈 생성 시 **자동으로 템플릿이 적용**됩니다.

### 📄 템플릿 파일
- [`.github/ISSUE_TEMPLATE/feature.md`](../.github/ISSUE_TEMPLATE/feature.md) — 기능 이슈
- [`.github/ISSUE_TEMPLATE/bug.md`](../.github/ISSUE_TEMPLATE/bug.md) — 버그 리포트

### 사용 방법
1. GitHub > Issues > **New issue** 클릭
2. 원하는 템플릿(Feature/Bug) 선택
3. 내용 작성 후 제출

---

## 📌 Branch

### 브랜치 전략: **Git-Flow**

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션용 브랜치 (배포 시 사용될 버전만 존재) |
| `dev` | 개발 전용 브랜치 |
| `feat/*`, `fix/*` 등 | 이슈 기반 작업 브랜치 |

### 이슈 기반 브랜치 규칙

- 생성된 이슈 번호 또는 기능명으로 브랜치 생성
- 형식: `타입/기능명` (예: `feat/login`, `feat/global-exception-handler`)

```
--------main--------
    \
     --------dev--------
         \
          --------feat/login
          \
           --------feat/global-exception-handler
```

### 브랜치 운영 규칙

- **PR이 Merge되면 해당 브랜치는 삭제**
- **머지 전 최소 2명 리뷰** 후 진행

---

## 📌 Commit

### 커밋 메시지 형식

```
<type>: <subject>
```

### 예시

- `chore: initial project setup`
- `feat: 전역 예외 처리 및 공통 응답 규격(ApiResponse) 구현`
- `docs: 아키텍처 가이드 문서 작성`

### Commit Type

| 코드 | 설명 | 원문 |
|------|------|------|
| `feat` | **새 기능** | Introduce new features. |
| `fix` | 간단한 수정 | Fix code. |
| `bugfix` | **버그 수정** | Fix a bug. |
| `hotfix` | 긴급 수정 | Critical hotfix. |
| `refactor` | 코드 리팩토링 | Refactor code. |
| `style` | 코드의 구조/형태 개선 | Improve structure / format of the code. |
| `docs` | 문서 추가/수정 | Add or update documentation. |
| `test` | **테스트 추가/수정** | Add or update tests. |
| `chore` | 구성 파일 추가/삭제 | Add or update configuration files. |
| `remove` | 코드/파일 삭제 | Remove code or files. |
| `init` | 프로젝트 시작 | Begin a project. |
| `release` | **릴리즈/버전 태그** | Release / Version tags. |

### 커밋 원칙

- **최대한 관련 커밋 tag에 맞게 커밋을 분리해서 push**
- 하나의 커밋에는 하나의 관심사만

---

## 📌 PR

GitHub에서 PR 생성 시 **자동으로 템플릿이 적용**됩니다.

### 📄 템플릿 파일
- [`.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md)

### PR 규칙

- PR 생성 시 위 템플릿 사용
- **리뷰어 최소 2명** 승인 후 머지
- 머지 방식은 **Squash and Merge** 권장 (커밋 히스토리 깔끔)
- 머지 완료 후 **작업 브랜치 삭제**

---

## 📚 관련 문서

- [아키텍처 가이드](./architecture.md) — 계층 구조 및 코드 배치 규칙
- [README](../README.md) — 프로젝트 개요 및 실행 방법
