# Interview explanation notes

Use this document to explain the project in interviews without drifting into a Terraform, Prometheus, dashboard-first, or application-feature-first narrative.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This document is an interview explanation layer. The source of truth for evidence remains:

```text
docs/04-evidence/evidence-index.md
docs/04-evidence/final-runtime-validation-2026-07-13.md
docs/04-evidence/connection-pressure-validation-2026-07-13.md
```

## One-line answer

```text
AWS EC2를 VM 환경처럼 사용해 작업 요청·증빙 파일 관리형 경량 웹 서비스를 WEB/WAS/DB/Storage/Backup/Observability 계층에 배치하고, 장애·성능·복구 상황을 HTTP 응답·로그·systemd 상태·DB row·pg_stat_activity·파일 checksum으로 검증한 운영 포트폴리오입니다.
```

Shorter version:

```text
VM 기반 다계층 운영환경을 만들고, 서비스 장애가 났을 때 어느 계층을 확인해야 하는지 evidence로 설명하는 프로젝트입니다.
```

## What service did you operate?

Use this answer:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며 조치 메모와 증빙 파일을 남길 수 있습니다. PostgreSQL에는 작업 요청, 상태 이력, 감사 로그, 파일 메타데이터를 저장하고, NFS에는 실제 첨부파일을 저장하도록 분리했습니다.
```

Boundary:

```text
상용 ITSM이나 ERP처럼 큰 업무시스템을 만든 것은 아닙니다. WEB/WAS/DB/File 운영 문제를 설명 가능하게 재현하기 위한 경량 업무 서비스입니다.
```

## 30-second version

```text
EC2를 VM 환경처럼 사용해 Nginx, Spring Boot embedded Tomcat, PostgreSQL, NFS, backup, Prometheus 계층을 분리 구성했습니다. 운영 대상은 작업 요청과 증빙 파일을 관리하는 경량 웹 서비스입니다. 이 서비스의 정상 흐름과 함께 Nginx 설정 오류, WAS artifact 배포 실패, Tomcat/HikariCP pressure, NFS mount 장애, 백업·복구를 검증했고, HTTP 상태, 로그, systemd 상태, DB row, pg_stat_activity, 파일 checksum으로 원인과 복구 여부를 확인했습니다.
```

## 2-minute interview script

```text
이 프로젝트는 AWS EC2를 VM 환경처럼 사용해 WEB, WAS, DB, 파일저장소, 백업, 관측성 계층을 분리 구성하고 운영 상황을 검증한 포트폴리오입니다.

운영 대상 서비스는 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스입니다. 사용자는 작업 요청을 등록하고, 운영자는 상태 변경과 조치 메모를 남기며, 증빙 파일을 업로드하거나 다운로드할 수 있습니다. PostgreSQL에는 작업 요청, 상태 이력, 감사 로그, 파일 메타데이터를 저장하고, NFS에는 실제 파일 object를 저장하도록 분리했습니다.

단순히 서버를 띄우는 데서 끝내지 않고, Nginx에서 Spring Boot embedded Tomcat 기반 WAS process로 요청이 전달되고, WAS가 HikariCP를 통해 PostgreSQL을 사용하며, 증빙 파일은 NFS file storage에 저장되는 구조를 만들었습니다. 정상 업무 흐름과 DB/file consistency를 먼저 검증한 뒤, 장애와 복구 상황을 계층별 evidence로 확인했습니다.

대표적으로 Nginx bad config는 nginx -t로 reload 전에 차단하고 known-good config 복구 후 proxy path를 확인했습니다. 잘못된 WAS artifact 배포는 systemd와 health 실패로 확인한 뒤 이전 jar/env를 복구하고 health, version, readiness, DB-backed summary까지 확인했습니다. Tomcat request-thread pressure와 HikariCP connection-pool pressure는 HTTP status/timing, Nginx log, app journald, HikariCP state, PostgreSQL pg_stat_activity로 구분했습니다. NFS mount 장애에서는 DB-backed 작업 요청은 성공하지만 file-storage 의존 증빙 파일 생성은 실패하는 것을 확인하고, remount 후 DB metadata와 NFS object, size, SHA-256 일치를 검증했습니다.

백업은 pg_dump와 restic artifact 생성만으로 끝내지 않았습니다. 별도 restore-lab에서 DB metadata와 NFS 파일을 복구한 뒤 HTTP/API consistency와 checksum으로 복구 결과를 검증했습니다. 이 프로젝트는 상용 운영 경험이라고 주장하기 위한 것이 아니라, 운영 직무에서 필요한 계층별 확인 절차와 근거 기반 판단을 EC2 lab에서 정리한 포트폴리오입니다.
```

## Representative five-scenario sequence

Use this sequence when an interviewer asks for concrete troubleshooting stories:

```text
1. Nginx bad config detection and rollback
   - bad candidate를 nginx -t로 차단
   - known-good config 복구 후 reload와 proxied health/version/summary 확인

2. Bad WAS artifact deployment and rollback
   - 잘못된 jar 배포로 systemd/health 실패 관찰
   - 이전 jar/env 복구 후 health, version, readiness, DB-backed summary 확인

3. Tomcat request-thread pressure vs HikariCP pool pressure
   - request-thread pressure는 DB-backed API 지연으로 나타남
   - HikariCP pool exhaustion은 PostgreSQL active 상태에서도 DB-backed API 503으로 나타남

4. App-side NFS mount failure and recovery
   - DB-backed work-order 생성은 성공
   - file-storage-dependent evidence-file 생성은 storage-not-ready로 실패
   - remount 후 DB metadata/NFS object/size/SHA-256 consistency 확인

5. Backup artifact vs restore-lab recovery proof
   - pg_dump/restic artifact 생성과 recovery proof를 구분
   - restore-lab에서 DB/file/API consistency와 checksum 확인
```

## What I built

| Area | Explanation |
|---|---|
| Operated service | Lightweight work-order/evidence-file web service |
| WEB | Nginx reverse proxy, request-path validation, config test and rollback target |
| WAS | Spring Boot embedded Tomcat process with health/readiness/API/failure-lab paths |
| DB pool | HikariCP connection-pool pressure observation |
| DB | PostgreSQL work-order, event, audit, and file metadata; pg_stat_activity checks |
| Storage | NFS-backed file object storage separated from DB metadata |
| Backup | pg_dump and restic-based backup artifact creation |
| Restore | separate restore-lab validation of DB/file/API consistency |
| Observability | logs, service state, node_exporter metrics, Prometheus scrape, DB service rule evaluation |
| IaC/config | Terraform for lab lifecycle, Ansible for server configuration and evidence collection |

## Main evidence chain

```text
1. WEB/WAS/DB/Storage normal business path works.
2. DB metadata and NFS file object consistency can be checked together.
3. Backup artifacts are created with inventory and checksum evidence.
4. Restore-lab proves DB/file/API consistency in a separate environment.
5. Logs and metrics narrow a DB incident from host failure to PostgreSQL service failure.
6. Nginx bad config is rejected before unsafe reload and restored with a known-good config.
7. Bad WAS artifact deployment is detected and rolled back through jar/env restore and post-rollback checks.
8. Tomcat request-thread pressure and HikariCP connection-pool pressure are distinguishable.
9. App-side NFS mount failure affects file-storage-dependent paths while DB-backed paths can remain available.
10. AWS lab resources were destroyed after final evidence collection.
```

## Strongest interview points

### 1. The project is operations-focused, not app-feature-focused

Good answer:

```text
서비스 기능을 크게 만드는 것보다 운영자가 장애 상황에서 어떤 계층을 먼저 확인해야 하는지를 검증하는 데 초점을 두었습니다. 다만 “무엇을 운영했는가”를 설명할 수 있도록 작업 요청과 증빙 파일을 다루는 경량 웹 서비스를 구현했습니다. 이 서비스는 DB metadata와 NFS file object를 분리하고, health/readiness, request log, upload/download, failure-lab endpoint를 제공해 WEB/WAS 운영 시나리오를 만들 수 있게 합니다.
```

Avoid:

```text
Spring Boot 서비스를 만들었습니다.
```

That answer hides the operating purpose.

### 2. The workload exists to make failures explainable

Good answer:

```text
작업 요청 등록, 상태 변경, 조치 메모, 증빙 파일 업로드·다운로드 흐름이 있어야 장애가 업무 흐름에 어떤 영향을 주는지 설명할 수 있다고 봤습니다. 그래서 DB-backed 업무 경로와 NFS file-storage-dependent 경로를 나누어 만들고, 장애 상황에서 두 경로가 어떻게 다르게 반응하는지 확인했습니다.
```

Avoid:

```text
웹 화면을 추가했습니다.
```

The point is not the UI itself, but the operating workload it creates.

### 3. The connection-pressure evidence is stronger than a generic latency story

Good answer:

```text
단순히 서버가 느렸다고 설명하지 않고 Tomcat request-thread pressure와 HikariCP connection-pool pressure를 나누어 검증했습니다. 전자는 DB-backed API가 지연되지만 성공했고, 후자는 PostgreSQL은 active인 상태에서도 WAS-side connection pool 고갈로 DB-backed API가 503 실패했습니다. 이 차이를 HTTP timing/status, Nginx log, app journald, HikariCP state, pg_stat_activity로 확인했습니다.
```

Avoid:

```text
부하 테스트를 했습니다.
```

This is bounded lab evidence, not production load testing or capacity sizing.

### 4. The restore validation is stronger than backup creation

Good answer:

```text
백업 파일을 만들었다는 것만으로는 복구 가능성을 증명할 수 없다고 봤습니다. 그래서 lab-full-ops에서 만든 backup artifact를 별도 restore-lab에 옮기고, pg_restore와 restic restore 후 HTTP/API consistency와 SHA-256 checksum으로 복구 결과를 검증했습니다.
```

Avoid:

```text
pg_dump와 restic으로 백업했습니다.
```

Backup creation alone is not the core claim.

### 5. The observability work supports diagnosis, not dashboard polish

Good answer:

```text
Prometheus를 도입한 목적은 대시보드를 꾸미기 위해서가 아니라 장애 원인을 좁히기 위해서였습니다. DB 장애 때 DB host의 node_exporter는 up=1이었고 PostgreSQL service active metric은 0이었기 때문에, DB host 장애가 아니라 PostgreSQL service dependency failure라고 판단할 수 있었습니다.
```

Avoid:

```text
Prometheus 모니터링을 구축했습니다.
```

That is too generic.

## Likely interview questions and answers

### Q1. 왜 EC2로 직접 구성했나요?

```text
관리형 서비스만 사용하면 운영자가 직접 확인해야 하는 계층별 설정, 로그, 포트, systemd 상태, 파일 권한, 백업 artifact의 경계를 보기 어렵다고 판단했습니다. 이 프로젝트에서는 EC2를 VM 환경처럼 사용해서 WEB/WAS/DB/Storage/Backup/Observability 계층을 직접 나누고, 각 계층에서 장애가 났을 때 어떤 evidence를 확인해야 하는지 검증했습니다.
```

### Q2. Terraform이 핵심인가요?

```text
아닙니다. Terraform은 실험 환경을 반복 가능하게 만들고 비용 리스크 때문에 검증 후 destroy하기 위한 도구입니다. 프로젝트의 핵심은 Terraform 코드 자체가 아니라, 생성된 VM 기반 운영환경에서 장애·복구를 어떻게 검증했는지입니다.
```

### Q3. Ansible이 핵심인가요?

```text
Ansible도 보조 도구입니다. 다만 서버 설정과 evidence 수집 절차를 사람이 임의로 수행하지 않고 재현 가능하게 만들기 위해 사용했습니다. 예를 들어 PostgreSQL, NFS, application service, Nginx, node_exporter, Prometheus 설정과 evidence 수집을 playbook으로 남겼습니다.
```

### Q4. 이 서비스가 실제 업무시스템인가요?

```text
상용 업무시스템 수준은 아닙니다. 대신 운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스입니다. 작업 요청 등록, 상태 변경, 조치 메모, 증빙 파일 업로드·다운로드, 상태 이력, 감사 로그, DB/file consistency를 포함해 WEB/WAS/DB/File 운영 문제를 설명할 수 있는 정도로 구성했습니다.
```

### Q5. 가장 의미 있었던 장애 검증은 무엇인가요?

```text
면접 상황에 따라 connection pressure나 NFS mount failure를 선택해 설명하겠습니다. connection pressure는 Tomcat request-thread pressure와 HikariCP connection-pool pressure를 HTTP status/timing, 로그, HikariCP state, pg_stat_activity로 구분했다는 점에서 WAS/DB 운영 이해를 보여주기 좋습니다. NFS mount failure는 DB-backed 업무 생성은 가능하지만 file-storage-dependent 기능은 실패할 수 있다는 점을 DB row, NFS object, checksum으로 확인했다는 점에서 계층 분리 설명에 적합합니다.
```

### Q6. 업로드 장애는 어떻게 접근했나요?

```text
업로드 실패를 단순 애플리케이션 오류로 보지 않고 Nginx 제한, WAS multipart 처리, PostgreSQL metadata 생성, NFS file object 생성 여부로 나누어 확인했습니다. HTTP status, Nginx log, app log, DB metadata row, NFS 파일 존재 여부, consistency check를 함께 보면서 어느 계층에서 실패했는지 분리했습니다.
```

### Q7. 지연 문제는 어떻게 구분했나요?

```text
단순 지연 시나리오에서는 WAS sleep과 DB sleep 경로를 비교했고, 최종 connection-pressure 검증에서는 Tomcat request-thread pressure와 HikariCP connection-pool pressure를 구분했습니다. 특히 HikariCP pool pressure에서는 PostgreSQL 자체는 active였지만 DB-backed API가 503 실패했기 때문에, DB 서버 장애가 아니라 WAS-side DB connection pool 고갈로 판단했습니다.
```

### Q8. 백업과 복구는 어떻게 검증했나요?

```text
lab-full-ops에서 PostgreSQL metadata와 NFS file object를 가진 데이터를 만든 뒤, backup-01에서 pg_dump와 restic snapshot을 생성했습니다. 이후 별도의 restore-lab 환경을 만들고, 해당 artifact를 복구한 뒤 work-order row count, evidence metadata, NFS file size, SHA-256 checksum, HTTP/API consistency endpoint를 통해 복구 결과가 일치하는지 검증했습니다.
```

### Q9. Prometheus alert까지 했으면 운영 모니터링이 완성된 건가요?

```text
그렇게 주장하지 않습니다. 이 프로젝트에서 검증한 것은 Alertmanager나 온콜 알림 체계가 아니라 Prometheus rule evaluation입니다. 즉 DB host는 reachable한데 PostgreSQL service가 inactive인 상태를 rule이 firing으로 평가하는지까지 검증했습니다. production monitoring maturity나 paging workflow는 별도 범위입니다.
```

### Q10. 왜 OpenKoda가 아니라 ops-sample-service를 사용했나요?

```text
이 프로젝트의 주제는 특정 솔루션 설치가 아니라 WEB/WAS/DB 운영환경을 직접 구성하고 장애·복구를 검증하는 것입니다. OpenKoda는 초기 workload 후보였지만, 포트폴리오에서는 운영 시나리오를 명확히 만들 수 있는 경량 서비스가 더 적합하다고 판단했습니다. 그래서 작업 요청, 상태 이력, 파일 업로드, DB/file consistency, failure-lab endpoint를 직접 통제할 수 있는 ops-sample-service를 운영 대상으로 삼았습니다.
```

### Q11. 다음으로 보강한다면 무엇을 할 건가요?

```text
현재 v1.0 기준으로는 새 runtime이나 새 오픈소스를 추가하기보다 제출·면접용 정리를 우선하겠습니다. 이미 주요 장애·복구 시나리오는 검증했기 때문에, 대표 시나리오별 claim, evidence, boundary를 더 짧고 명확하게 설명하고 지원서나 포트폴리오 URL에 맞는 문구로 정리하는 것이 더 중요하다고 봅니다.
```

## Claims to avoid

Do not say:

```text
운영 실무 경험이 있습니다.
프로덕션 장애 대응 경험이 있습니다.
프로덕션 수준 DR을 구현했습니다.
HA와 자동 failover를 구현했습니다.
SLO/SLA를 검증했습니다.
RPO/RTO를 보장했습니다.
대규모 부하 테스트를 수행했습니다.
capacity sizing을 완료했습니다.
상용 ITSM을 만들었습니다.
Kubernetes/GitOps 운영을 했습니다.
외부 Tomcat/WAR 운영을 했습니다.
blue-green/canary 배포를 구현했습니다.
zero-downtime release를 보장했습니다.
```

Use instead:

```text
상용 운영 경험은 아니지만, 운영 직무에서 필요한 계층별 확인 절차와 장애·복구 검증 방식을 EC2 기반 lab에서 evidence로 정리했습니다.
```

## Documents to review before interviews

```text
README.md
docs/00-project/portfolio-review-guide.md
docs/00-project/portfolio-summary.md
docs/04-evidence/evidence-index.md
docs/04-evidence/final-runtime-validation-2026-07-13.md
docs/04-evidence/connection-pressure-validation-2026-07-13.md
docs/00-project/interview-incident-qna.md
docs/00-project/submission-description-notes.md
```
