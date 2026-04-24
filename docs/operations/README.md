# 운영 / 인프라 / 배포 문서

**어디에 어떻게 올라가고, 어떻게 배포되는지** — 서버, 네트워크, CI/CD, 외부 API.

| 문서 | 한 줄 요약 |
|---|---|
| [인프라 명세](./infrastructure.md) | EC2 스펙, 네트워크, 보안그룹, 컨테이너 구성, 도메인 |
| [배포 파이프라인](./deployment.md) | GitHub Actions, Dockerfile, Secrets, 롤백, 외부 API 운영 동작 |

> 🔖 이 폴더의 "인프라"는 **배포 환경**입니다. 코드 계층 이름 `infrastructure/`는 [architecture/package-structure.md](../architecture/package-structure.md) 참고.
