# 006. 팀 협업 컨벤션

> **이 문서를 보면**: 이슈/브랜치/커밋/PR을 어떤 규칙으로 만들어야 하는지 파악 가능.
>
> **언제 다시 보나요**: 이슈 만들기 전, PR 올리기 전, 커밋 메시지 작성 시.

---

## 한 줄 요약

`main` / `dev` / `feat|fix/*` 브랜치 전략(Git-Flow 단순화 버전) + Conventional Commits 기반 커밋 규칙 + GitHub 이슈/PR 템플릿 자동 적용 + 리뷰어 2명 이상 승인 후 머지.

---

팀원 전원이 지켜야 할 협업 규칙 모음.

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

### 브랜치 전략

`main` / `dev` + 기능 브랜치로 나누는 **Git-Flow 단순화 버전**. release/hotfix 브랜치는 사용하지 않음.

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션용 브랜치 (배포 시 사용될 버전만 존재) |
| `dev` | 개발 전용 브랜치 |
| `feat/*`, `fix/*` 등 | 기능 단위 작업 브랜치 |

### 브랜치 네이밍 규칙

- 형식: `타입/기능명` (예: `feat/login`, `feat/global-exception-handler`, `fix/jwt-expiration`)

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

- **커밋 타입별로 커밋을 분리**해서 푸시 (예: 기능 추가와 문서 수정은 별도 커밋)
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

---

## 📚 관련 문서

- [004. 아키텍처 가이드](./004-architecture.md) — 계층 구조 및 코드 배치 규칙
- [README](../README.md) — 프로젝트 개요 및 실행 방법
