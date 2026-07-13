# Interview explanation notes

Use this document to explain the project in interviews without drifting into a Terraform, Prometheus, dashboard-first, or application-feature-first narrative.

## One-line answer

```text
AWS EC2 기반으로 작업 요청·증빙 파일 관리형 경량 웹 서비스를 WEB/WAS/DB/파일저장소/백업/관측성 계층에 배치하고, 장애·성능·복구 상황을 로그·지표·HTTP 응답·DB row·파일 checksum으로 검증한 운영 포트폴리오입니다.
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

## 2-minute interview script

```text
이 프로젝트는 AWS EC2를 VM 환경처럼 사용해 WEB, WAS, DB, 파일저장소, 백업, 관측성 계층을 분리 구성하고 운영 상황을 검증한 프로젝트입니다.

운영 대상 서비스는 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스입니다. 사용자는 작업 요청을 등록하고, 운영자는 상태 변경과 조치 메모를 남기며, 증빙 파일을 업로드하거나 다운로드할 수 있습니다. PostgreSQL에는 작업 요청, 상태 이력, 감사 로그, 파일 메타데이터를 저장하고, NFS에는 실제 파일 object를 저장하도록 분리했습니다.

단순히 서버를 띄우는 데서 끝내지 않고, Nginx에서 Spring Boot WAS로 요청이 전달되고, WAS가 PostgreSQL과 NFS 파일저장소를 함께 사용하는 구조를 만들었습니다. 이후 정상 요청, WAS 장애, DB 장애, 파일 메타데이터와 실제 파일의 일관성, 백업 artifact 생성, 별도 restore-lab 복구까지 단계적으로 검증했습니다.

서비스 보강 이후에는 작업 요청 웹 화면, 상태 변경, 조치 메모, 증빙 파일 업로드·다운로드, request ID 기반 로그 추적, upload-limit, WAS sleep, DB sleep, DB web-impact 시나리오를 AWS runtime에서 1차 검증했습니다. 이 검증은 상용 운영 경험이 아니라 controlled lab evidence이지만, WEB/WAS/DB/NFS 계층을 어떤 순서로 확인해야 하는지 설명할 수 있게 만들었습니다.

가장 중요하게 본 부분은 장애 상황에서 추측하지 않고 evidence로 원인을 좁히는 것이었습니다. 예를 들어 DB 장애 상황에서는 /healthz는 200이지만 /readyz와 DB-dependent API는 503이 되었고, Prometheus에서는 DB host의 node_exporter는 up=1로 살아 있는데 PostgreSQL service active metric은 0으로 떨어지는 것을 확인했습니다. 이를 통해 DB 서버 자체가 죽은 것이 아니라 PostgreSQL 서비스 의존성이 실패했다는 결론을 낼 수 있었습니다.

또한 백업은 단순히 pg_dump와 restic snapshot을 만든 것으로 끝내지 않고, 별도 restore-lab 환경에서 DB metadata와 NFS 파일을 복구한 뒤 HTTP/API consistency와 SHA-256 checksum으로 검증했습니다.
```

## 30-second version

```text
EC2 기반으로 Nginx, Spring Boot WAS, PostgreSQL, NFS, backup node, Prometheus monitoring node를 분리 구성했습니다. 운영 대상 서비스는 작업 요청과 증빙 파일을 관리하는 경량 웹 서비스로, 작업 요청 등록·상태 변경·증빙 파일 업로드·다운로드·DB/file consistency 확인 흐름을 갖고 있습니다. WEB/WAS/DB 정상 경로, WAS 장애, DB 서비스 장애, DB metadata와 NFS 파일 일관성, 백업 artifact 생성, 별도 restore-lab 복구를 검증했고, 서비스 보강 후에는 upload-limit, latency, DB web-impact까지 1차 runtime evidence로 검증했습니다.
```

## What I built

| Area | Explanation |
|---|---|
| Operated service | Lightweight work-order/evidence-file web service |
| WEB | Nginx reverse proxy, request-path validation, upload-limit and timeout observation |
| WAS | Spring Boot/Tomcat service with health/readiness/web/API/failure-lab paths |
| DB | PostgreSQL work-order, event, audit, and file metadata |
| Storage | NFS-backed file object storage separated from DB metadata |
| Backup | pg_dump and restic-based backup artifact creation |
| Restore | separate restore-lab validation of DB/file/API consistency |
| Observability | logs, service state, node_exporter metrics, Prometheus scrape, DB service alert-rule evaluation |
| IaC/config | Terraform for lab lifecycle, Ansible for server configuration and evidence collection |

## Main evidence chain

```text
1. WEB/WAS/DB normal path works.
2. WAS failure can be isolated through Nginx upstream behavior.
3. DB-dependent readiness/API paths fail when PostgreSQL is stopped.
4. DB metadata and NFS file object consistency can be checked together.
5. Backup artifacts are created with inventory and checksum evidence.
6. Restore-lab proves the backup artifacts can restore DB/file/API consistency in a separate environment.
7. Logs and metrics narrow the DB incident from host failure to PostgreSQL service failure.
8. Prometheus rule evaluation detects PostgreSQL service inactivity while DB host remains reachable.
9. Enhanced work-order web workflow works through Nginx/WAS/PostgreSQL/NFS in the first enhanced runtime validation pass.
10. Upload-limit, latency, and DB web-impact scenarios were validated as controlled WEB/WAS operating incidents.
```

## Incident reports to use in interviews

Use these reports when the interviewer asks for concrete troubleshooting stories:

```text
docs/05-incident-reports/enhanced-service-workflow-baseline-report.md
docs/05-incident-reports/upload-limit-incident-report.md
docs/05-incident-reports/latency-diagnosis-incident-report.md
docs/05-incident-reports/db-web-impact-incident-report.md
docs/05-incident-reports/restore-lab-recovery-incident-report.md
```

Use this Q&A document for shorter spoken answers:

```text
docs/00-project/interview-incident-qna.md
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

### 2. Enhanced workflow validation makes the workload concrete

Good answer:

```text
처음에는 단순 API 수준의 smoke test로도 운영 시나리오를 만들 수 있었지만, 면접에서 “무엇을 운영했는가”를 설명하기에는 부족했습니다. 그래서 작업 요청 등록, 상태 변경, 조치 메모, 증빙 파일 업로드·다운로드가 있는 경량 웹 업무 서비스를 만들고, 이 흐름이 Nginx, WAS, PostgreSQL, NFS를 실제로 거치는지 검증했습니다.
```

Avoid:

```text
웹 화면을 추가했습니다.
```

The point is not the UI itself, but the operating workload it creates.

### 3. The restore validation is stronger than backup creation

Good answer:

```text
백업 파일을 만들었다는 것만으로는 복구 가능성을 증명할 수 없다고 봤습니다. 그래서 lab-full-ops에서 만든 backup artifact를 별도 restore-lab에 옮기고, pg_restore와 restic restore 후 HTTP/API consistency와 SHA-256 checksum으로 복구 결과를 검증했습니다.
```

Avoid:

```text
pg_dump와 restic으로 백업했습니다.
```

Backup creation alone is not the core claim.

### 4. The observability work supports diagnosis, not dashboard polish

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
관리형 서비스만 사용하면 운영자가 직접 확인해야 하는 계층별 설정, 로그, 포트, systemd 상태, 파일 권한, 백업 artifact의 경계를 보기 어렵다고 판단했습니다. 이 프로젝트에서는 EC2를 VM 환경처럼 사용해서 WEB/WAS/DB/Storage/Backup/Monitoring 계층을 직접 나누고, 각 계층에서 장애가 났을 때 어떤 evidence를 확인해야 하는지 검증했습니다.
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
DB web-impact 검증입니다. PostgreSQL 장애 상황에서 /healthz는 200으로 살아 있었지만 /readyz와 DB-dependent API는 503을 반환했습니다. 이 조합을 통해 애플리케이션 프로세스가 살아 있는 것과 업무 요청을 처리할 준비가 된 것은 다르다는 점을 확인했습니다. 이전 observability 검증에서는 DB host의 node_exporter up metric은 1이고 PostgreSQL service active metric은 0인 상태도 확인해, DB host outage가 아니라 DB service dependency failure라고 판단할 수 있었습니다.
```

### Q6. 업로드 장애는 어떻게 접근했나요?

```text
업로드 실패를 단순 애플리케이션 오류로 보지 않고 Nginx 제한, WAS multipart 처리, PostgreSQL metadata 생성, NFS file object 생성 여부로 나누어 확인했습니다. HTTP status, Nginx log, app log, DB metadata row, NFS 파일 존재 여부, consistency check를 함께 보면서 어느 계층에서 실패했는지 분리했습니다.
```

### Q7. 지연 문제는 어떻게 구분했나요?

```text
WAS sleep과 DB sleep 경로를 나누어 검증했습니다. WAS sleep은 애플리케이션 내부 처리 지연을 만들고, DB sleep은 DB 의존 경로의 지연을 만듭니다. 두 요청의 응답 시간, Nginx upstream response time, app request duration, DB 의존 endpoint 상태를 비교해 지연이 WAS 처리 문제인지 DB-backed path 문제인지 구분했습니다.
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
새로운 플랫폼 기능을 추가하기보다 VM/systemd 기반 배포·롤백 시나리오를 보강하겠습니다. app jar 교체, systemd restart, health/readiness 확인, 문제가 있는 배포의 rollback, rollback 후 업무 흐름과 DB/file consistency 확인까지 검증하면 WEB/WAS 운영 포트폴리오로 더 설득력이 높아질 것입니다.
```

## Claims to avoid

Do not say:

```text
운영 실무 경험이 있습니다.
프로덕션 수준 DR을 구현했습니다.
HA와 자동 failover를 구현했습니다.
SLO/SLA를 검증했습니다.
상용 ITSM을 만들었습니다.
Kubernetes/GitOps 운영을 했습니다.
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
docs/05-incident-reports/README.md
docs/00-project/interview-incident-qna.md
docs/00-project/deployment-rollback-scenario-plan.md
```
