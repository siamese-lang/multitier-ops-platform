# Portfolio summary

## Project title

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## One-line summary

```text
작업 요청·증빙 파일 관리형 경량 웹 서비스를 EC2 VM 기반 WEB/WAS/DB/Storage/Backup/Observability 계층 위에 구성하고, 장애·성능·복구 상황을 로그·지표·HTTP 응답·DB row·파일 checksum으로 검증하는 운영 포트폴리오
```

## Operated service

The operated service is `ops-sample-service`.

Use this description:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

What the service does:

```text
사용자는 작업 요청을 등록한다.
운영자는 작업 요청 상세 화면에서 상태를 변경하고 조치 메모를 남긴다.
운영자는 증빙 파일을 업로드하고 다운로드할 수 있다.
PostgreSQL은 작업 요청, 상태 이력, 감사 로그, 파일 metadata를 저장한다.
NFS/file storage는 실제 증빙 파일 object를 저장한다.
Consistency API는 DB metadata와 file object의 크기·SHA-256 일치를 확인한다.
Failure-lab endpoints는 WEB/WAS 지연, DB 지연, DB connection hold, HikariCP pool state, WAS runtime, file storage 상태, upload limit을 관찰하게 한다.
```

Boundary:

```text
This is a lightweight operations service, not a commercial ITSM clone or production service.
The service exists to make WEB/WAS/DB/Storage/Backup operations scenarios concrete and explainable.
```

## What this project is meant to prove

This project is designed to prove operating capability, not feature development capability.

It demonstrates that the operator can:

```text
1. Separate an application environment into WEB/WAS/DB/Storage/Backup/Observability tiers.
2. Configure the tiers with repeatable Terraform and Ansible workflows.
3. Validate normal request paths through Nginx, WAS, DB, and NFS-backed file storage.
4. Reproduce failures and identify which tier should be inspected first.
5. Use logs, service state, metrics, checksums, HTTP status, timing, and API responses as evidence.
6. Distinguish embedded Tomcat request-thread pressure from HikariCP DB connection-pool pressure.
7. Create backup artifacts and prove recovery in a separate restore-lab environment.
8. Keep application features small while making the operated service explainable.
9. Document supported claims without overclaiming production maturity.
```

## Main runtime topology

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
- mon-01
```

Main request and operations path:

```text
operator -> nginx-01:443 -> app-01 embedded Tomcat -> HikariCP -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432
backup-01 -> nfs-01:/srv/ops-sample/files
mon-01    -> node_exporter on WEB/WAS/DB/Storage/Backup nodes
```

## Key validation outcomes

### 1. WEB/WAS/DB operating path

Validated with runtime evidence:

```text
Nginx reverse proxy
Spring Boot embedded Tomcat app service
PostgreSQL DB connection
health/readiness distinction
Nginx access log and upstream evidence
WAS failure and upstream bypass
rolling restart continuity
DB-backed concurrent request observation
```

### 2. DB metadata and NFS file consistency

Validated with runtime evidence:

```text
NFS export and app mount
work-order evidence file creation
PostgreSQL metadata row creation
NFS file object existence
file size and SHA-256 match
application consistency endpoint
```

### 3. Enhanced web workflow and upload/download path

Validated with enhanced runtime evidence:

```text
work-order list/detail/create/status-change workflow through Nginx/WAS
status history and audit log visibility
evidence upload through multipart web form
evidence download through web endpoint
DB metadata and NFS file object consistency
request ID correlation through Nginx and app logs
```

### 4. Upload-limit, latency, and DB web-impact scenarios

Validated with enhanced runtime evidence:

```text
upload-limit incident validation
WAS sleep vs DB sleep latency scenario validation
DB web-impact incident validation
health vs readiness distinction during DB service impact
service recovery verification after controlled incident
```

### 5. Connection pressure validation

Validated with bounded lab runtime evidence:

```text
embedded Tomcat request-thread pressure caused delayed but successful DB-backed summary response
HikariCP connection-pool pressure caused DB-backed request failure while PostgreSQL stayed active
Nginx access log, app journald, HikariCP state, PostgreSQL pg_stat_activity, and HTTP timing/status were correlated
```

Representative evidence:

```text
baseline_tomcat_max_threads=4
baseline_hikari_max_pool_size=2
was_summary_during_http_code=200
was_summary_during_time_total=9.071659
db_summary_during_http_code=503
db_pool_active_connections=2
db_pool_idle_connections=0
PostgreSQL state=active, wait_event=PgSleep, query=select pg_sleep($1)
```

Supported interpretation:

```text
The project can distinguish a delayed API caused by WAS request-thread pressure from a failed DB-backed API caused by WAS-side DB connection-pool exhaustion.
```

Boundary:

```text
This is bounded lab evidence. It is not production load testing, capacity sizing, or external Tomcat/WAR operation.
```

### 6. Backup artifact creation

Validated with runtime evidence:

```text
pg_dump artifact for PostgreSQL metadata
NFS file inventory and checksum evidence
restic snapshot for file objects
backup metadata and raw artifact preservation
```

Important boundary:

```text
Backup artifact creation alone is not a recovery claim.
Recovery was proven separately in restore-lab.
```

### 7. Restore-lab recovery

Validated with runtime evidence:

```text
separate restore-lab VPC
pg_restore into restore DB node
restic restore into restore NFS node
application reads restored DB metadata and file object
HTTP/API consistency through Nginx
sample file size and SHA-256 match
```

Supported claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
```

### 8. Observability and incident diagnosis

Validated with runtime evidence:

```text
service state evidence
Nginx request-path evidence
application readiness and DB dependency evidence
PostgreSQL service/port evidence
node_exporter host metrics
Prometheus scrape evidence
Prometheus rule evaluation evidence
```

Supported diagnostic claims:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Interview-ready incident reports

The incident report layer converts raw validation output into operations narratives.

```text
docs/05-incident-reports/README.md
docs/05-incident-reports/enhanced-service-workflow-baseline-report.md
docs/05-incident-reports/upload-limit-incident-report.md
docs/05-incident-reports/latency-diagnosis-incident-report.md
docs/05-incident-reports/db-web-impact-incident-report.md
docs/05-incident-reports/restore-lab-recovery-incident-report.md
docs/05-incident-reports/connection-pressure-incident-report.md
```

Recommended interview sequence:

```text
1. Start with the enhanced service workflow baseline.
2. Explain upload-limit diagnosis as WEB/WAS/DB/NFS separation.
3. Explain latency diagnosis as WAS-side delay vs DB-backed delay.
4. Explain DB web-impact as health vs readiness and dependency failure.
5. Explain connection pressure as embedded Tomcat thread pressure vs HikariCP pool exhaustion.
6. Explain restore-lab as the difference between backup artifact creation and recovery proof.
```

Scenario-specific Q&A:

```text
docs/00-project/interview-incident-qna.md
```

Portfolio reading guide:

```text
docs/00-project/portfolio-review-guide.md
```

## Representative evidence documents

```text
docs/04-evidence/evidence-index.md
docs/00-project/current-state-after-enhanced-runtime-validation.md
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md
docs/04-evidence/observability-baseline-validation-2026-07-12.md
docs/04-evidence/observability-metrics-validation-2026-07-12.md
docs/04-evidence/observability-alert-validation-2026-07-12.md
docs/04-evidence/connection-pressure-validation-2026-07-13.md
```

Service implementation references:

```text
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
docs/00-project/ops-sample-service-completion-scope.md
```

## Tools and their roles

| Tool | Role in this project | Not the project theme |
|---|---|---|
| Terraform | Create and destroy temporary AWS lab environments | Not a Terraform showcase |
| Ansible | Configure hosts and reproduce operating procedures | Not an Ansible role showcase |
| AWS EC2 | VM-style infrastructure substrate | Not an AWS managed architecture project |
| Nginx | WEB/reverse proxy tier | Not a web tuning-only project |
| Spring Boot/embedded Tomcat | Runs the lightweight operated service in the WAS tier; exposes request-thread pressure evidence | Not external Tomcat/WAR administration |
| HikariCP | Exposes WAS-side DB connection-pool pressure evidence | Not production capacity sizing |
| PostgreSQL | DB tier for work-order/event/audit/file metadata and pg_stat_activity checks | Not HA database engineering |
| NFS | File storage tier for evidence file objects | Not storage product evaluation |
| pg_dump/restic | Backup and restore tooling | Not backup product comparison |
| node_exporter/Prometheus | Metrics evidence for diagnosis | Not a monitoring platform project |

## Claims that are safe in an interview

Runtime evidence claims:

```text
I built an EC2-based multi-tier operating environment with separated WEB/WAS/DB/Storage/Backup/Monitoring nodes.
I validated normal and failure paths with evidence rather than only screenshots.
I verified DB metadata and NFS file consistency with size and SHA-256 checks.
I validated work-order and evidence-file web workflows through Nginx, WAS, PostgreSQL, and NFS.
I tested upload-limit, latency, DB-impact, and connection-pressure scenarios as WEB/WAS operating incidents.
I distinguished embedded Tomcat thread pressure from HikariCP pool exhaustion using HTTP timing/status, Nginx logs, app journald, HikariCP state, and PostgreSQL pg_stat_activity.
I created backup artifacts and then proved recovery in a separate restore-lab environment.
I used logs, service state, request-path responses, and Prometheus metrics to narrow a DB service incident.
```

Service implementation claims:

```text
I implemented the operated service as a lightweight work-order and evidence-file web service.
The service includes work-order pages, status history, audit logs, evidence upload/download, and failure-lab endpoints.
```

Careful boundary:

```text
This is lab runtime validation for an operations portfolio.
It is not production operations experience, production load testing, production DR, HA, automatic failover, SLO/SLA, capacity sizing, external Tomcat administration, or RPO/RTO proof.
```

## Claims that must not be made

```text
production operations experience
production-grade monitoring maturity
Grafana dashboard readiness
Alertmanager notification maturity
paging or on-call workflow
PostgreSQL HA
automatic failover
SLO/SLA compliance
Kubernetes/EKS/GitOps operation
AWS managed architecture operation
commercial ITSM implementation
production disaster recovery
production load testing
capacity sizing
external Tomcat/WAR operation
RPO/RTO guarantee
```

## Project hardening focus

This project is not complete just because runtime validation succeeded.

Further work should focus on:

```text
evidence-index quality
incident report layer
interview explanation notes
portfolio submission wording
one optional VM/systemd deployment rollback scenario if justified
```

Further work should not focus on:

```text
more Prometheus features
Grafana dashboards
Alertmanager routing
Loki expansion
unplanned AWS runtime windows
unrelated architecture expansion
```
