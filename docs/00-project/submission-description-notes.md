# Submission description notes

Use this document when submitting the repository URL in an application form, resume, portfolio page, or interview-preparation material.

This is not a new project scope. It is a wording reference for the current portfolio state.

## Project title

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## URL field description

When an application form asks for a short description of the submitted URL, use one of the following depending on the character limit.

### Very short version

```text
EC2 기반 WEB/WAS/DB/Storage/Backup 운영환경에서 경량 웹 업무서비스의 장애·복구를 검증한 운영 포트폴리오
```

### 100-character version

```text
EC2 기반 WEB/WAS/DB/Storage/Backup 운영환경을 구성하고, 작업 요청·증빙 파일 서비스의 장애·복구를 검증한 프로젝트입니다.
```

### 300-character version

```text
AWS EC2를 VM 환경처럼 사용해 Nginx WEB, Spring Boot WAS, PostgreSQL DB, NFS 파일저장소, backup, Prometheus 계층을 분리 구성한 운영 포트폴리오입니다. 운영 대상은 작업 요청·증빙 파일 관리형 경량 웹 서비스이며, 기존 runtime evidence로 정상 경로, 장애, DB/file consistency, backup artifact, restore-lab 복구, DB service incident 진단을 검증했습니다.
```

### 500-character version

```text
AWS EC2 기반으로 Nginx WEB, Spring Boot WAS, PostgreSQL DB, NFS 파일저장소, backup node, Prometheus monitoring node를 분리 구성한 운영 포트폴리오입니다. 운영 대상은 작업 요청 등록, 상태 변경, 조치 메모, 증빙 파일 업로드·다운로드를 제공하는 경량 웹 업무 서비스입니다. 기존 runtime evidence로 WEB/WAS/DB 정상 경로, WAS 장애, PostgreSQL 장애, DB metadata와 NFS file object consistency, pg_dump/restic backup artifact 생성, 별도 restore-lab 복구, logs/service/request-path/Prometheus metric 기반 DB service incident diagnosis를 문서화했습니다.
```

## Resume project bullet

Use this when a resume or portfolio table has room for one concise bullet.

```text
AWS EC2 기반 WEB/WAS/DB/Storage/Backup/Observability 운영환경을 직접 구성하고, 작업 요청·증빙 파일 관리형 경량 웹 서비스의 장애·복구 시나리오를 로그·지표·명령 결과로 검증한 운영 포트폴리오
```

## Resume achievement bullets

```text
- EC2 VM 기반으로 Nginx, Spring Boot WAS, PostgreSQL, NFS, backup node, Prometheus monitoring node를 계층 분리 구성했습니다.
- 운영 대상 서비스는 작업 요청·증빙 파일 관리형 경량 웹 서비스로, 웹 화면·상태 이력·파일 업로드/다운로드·failure-lab 경로를 포함하도록 보강했습니다.
- WEB/WAS/DB 정상 경로, WAS 장애, PostgreSQL 장애, DB metadata와 NFS file consistency를 runtime evidence로 검증했습니다.
- pg_dump/restic backup artifact를 생성하고 별도 restore-lab에서 DB/file/API 복구와 SHA-256 checksum 일치를 검증했습니다.
- logs, service state, request path, node_exporter, Prometheus metric을 사용해 DB host reachability와 PostgreSQL service failure를 구분했습니다.
```

## GitHub repository description

GitHub repository description field can use this shorter line:

```text
EC2 VM 기반 WEB/WAS/DB/Storage/Backup 운영환경과 경량 웹 업무서비스 장애·복구 evidence 검증 포트폴리오
```

## Portfolio page summary

```text
이 프로젝트는 EC2를 VM 환경처럼 사용해 운영자가 직접 확인해야 하는 계층별 상태를 드러내는 데 초점을 두었습니다. 운영 대상은 작업 요청·증빙 파일 관리형 경량 웹 서비스이며, Nginx, WAS, PostgreSQL, NFS, backup node, Prometheus node를 나누어 구성했습니다. 정상 경로와 장애 경로를 evidence로 검증했고, 백업은 artifact 생성으로 끝내지 않고 별도 restore-lab에서 복구를 확인했습니다. DB 장애는 HTTP 응답, systemd 상태, 포트, Prometheus metric을 함께 확인해 DB host 장애와 PostgreSQL service failure를 구분했습니다.
```

## Interview opening answer

```text
이 프로젝트는 EC2를 VM 환경처럼 사용해서 WEB/WAS/DB/Storage/Backup/Observability 계층을 직접 나누고, 장애와 복구 상황을 evidence로 검증한 운영 포트폴리오입니다. 운영 대상은 작업 요청·증빙 파일 관리형 경량 웹 서비스입니다. 서비스 기능을 크게 만드는 것보다 운영 관점에서 어떤 계층이 실패했는지 확인하는 데 초점을 두었고, 백업 artifact 생성과 별도 restore-lab 복구, DB service incident의 logs/metrics 기반 진단까지 문서화했습니다.
```

## Strongest evidence to mention

If only three evidence points can be mentioned, use these:

```text
1. restore-lab DB/file/API recovery validation
2. DB metadata + NFS file object consistency validation
3. Prometheus metric/rule-based distinction between DB host reachability and PostgreSQL service failure
```

If the interviewer asks what service was operated, use this:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스입니다. 작업 요청 등록, 상태 변경, 조치 메모, 증빙 파일 업로드·다운로드를 제공하고, PostgreSQL에는 metadata를, NFS에는 실제 파일 object를 저장합니다.
```

## Safe claims

Runtime evidence claims:

```text
EC2 기반 다계층 운영환경을 직접 구성했다.
WEB/WAS/DB/Storage/Backup/Observability 계층을 분리했다.
장애와 복구를 로그·지표·명령 결과로 검증했다.
DB metadata와 NFS file object consistency를 검증했다.
backup artifact 생성과 별도 restore-lab 복구를 구분해 검증했다.
Prometheus metric으로 DB host reachability와 PostgreSQL service failure를 구분했다.
```

Implementation claims:

```text
작업 요청·증빙 파일 관리형 경량 웹 서비스 구현 baseline을 추가했다.
웹 화면, 상태 이력, 감사 로그, 증빙 파일 업로드/다운로드, failure-lab endpoints가 있다.
```

Careful boundary:

```text
보강된 웹 서비스 흐름에 대한 AWS runtime evidence는 다음 검증 단계에서 갱신해야 한다.
```

## Claims to avoid

Do not use these phrases in resumes or interviews:

```text
production 운영 경험
상용 수준 모니터링 완성
Grafana dashboard 구축 완료
Alertmanager 알림 체계 완성
온콜/장애 대응 체계 구축
PostgreSQL HA 구현
자동 failover 구현
SLO/SLA 검증
Kubernetes/EKS/GitOps 운영 프로젝트
AWS managed architecture 운영 프로젝트
상용 ITSM 구현
보강된 웹 서비스 runtime 검증 완료
```

## Reader path to include near the URL

If the application form allows additional notes, include this path:

```text
README.md → apps/ops-sample-service/README.md → apps/ops-sample-service/FAILURE_LAB.md → docs/00-project/portfolio-summary.md → docs/04-evidence/evidence-index.md → docs/00-project/interview-explanation-notes.md
```
