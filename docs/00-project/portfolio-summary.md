# Portfolio summary

## Project title

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## One-line summary

```text
작업 요청·증빙 파일 관리형 경량 웹 서비스를 EC2 VM 기반 WEB/WAS/DB/Storage/Backup/Observability 계층 위에 구성하고, 장애·복구 상황을 로그·지표·명령 결과로 검증하는 운영 포트폴리오
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
Failure-lab endpoints는 WEB/WAS 지연, DB 지연, file storage 상태, upload limit을 관찰하게 한다.
```

Boundary:

```text
This is a lightweight operations service, not a commercial ITSM clone or production service.
The enhanced service implementation exists in the repository, but AWS runtime evidence for the enhanced workflow must still be refreshed.
```

## What this project is meant to prove

This project is designed to prove operating capability, not feature development capability.

It demonstrates that the operator can:

```text
1. Separate an application environment into WEB/WAS/DB/Storage/Backup/Observability tiers.
2. Configure the tiers with repeatable Terraform and Ansible workflows.
3. Validate normal request paths through Nginx, WAS, DB, and NFS-backed file storage.
4. Reproduce failures and identify which tier should be inspected first.
5. Use logs, service state, metrics, checksums, and API responses as evidence.
6. Create backup artifacts and prove recovery in a separate restore-lab environment.
7. Keep application features small while making the operated service explainable.
8. Document supported claims without overclaiming production maturity.
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
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432
backup-01 -> nfs-01:/srv/ops-sample/files
mon-01    -> node_exporter on WEB/WAS/DB/Storage/Backup nodes
```

## Key validation outcomes

### 1. WEB/WAS/DB operating path

Validated with existing runtime evidence:

```text
Nginx reverse proxy
Spring Boot/Tomcat app service
PostgreSQL DB connection
health/readiness distinction
Nginx access log and upstream evidence
WAS failure and upstream bypass
rolling restart continuity
DB-backed concurrent request observation
```

### 2. DB metadata and NFS file consistency

Validated with existing runtime evidence:

```text
NFS export and mount
work-order evidence file creation
PostgreSQL metadata row creation
NFS file object existence
file size and SHA-256 match
application consistency endpoint
```

### 3. Backup artifact creation

Validated with existing runtime evidence:

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

### 4. Restore-lab recovery

Validated with existing runtime evidence:

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

### 5. Observability and incident diagnosis

Validated with existing runtime evidence:

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

### 6. Enhanced service implementation baseline

Implemented in repository code:

```text
work-order domain/schema
server-rendered work-order web workflow
evidence upload/download workflow
status history and audit logs
WEB/WAS failure-lab page and APIs
```

Pending runtime evidence:

```text
enhanced web workflow through Nginx
upload/download through Nginx -> WAS -> file storage -> PostgreSQL metadata
failure-lab slow request and DB-sleep behavior
refreshed restore-lab validation against enhanced service model
```

## Representative evidence documents

```text
docs/04-evidence/evidence-index.md
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
docs/04-evidence/observability-baseline-validation-2026-07-12.md
docs/04-evidence/observability-metrics-validation-2026-07-12.md
docs/04-evidence/observability-alert-validation-2026-07-12.md
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
| Spring Boot/Tomcat | Runs the lightweight operated service in the WAS tier | Not a Spring Boot feature project |
| PostgreSQL | DB tier for work-order/event/audit/file metadata | Not HA database engineering |
| NFS | File storage tier for evidence file objects | Not storage product evaluation |
| pg_dump/restic | Backup and restore tooling | Not backup product comparison |
| node_exporter/Prometheus | Metrics evidence for diagnosis | Not a monitoring platform project |

## Claims that are safe in an interview

Runtime evidence claims:

```text
I built an EC2-based multi-tier operating environment with separated WEB/WAS/DB/Storage/Backup/Monitoring nodes.
I validated normal and failure paths with evidence rather than only screenshots.
I verified DB metadata and NFS file consistency with size and SHA-256 checks.
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
Enhanced web workflow runtime evidence is the next validation target.
Do not present the enhanced workflow as already runtime-validated until the evidence docs are refreshed.
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
```

## Project freeze boundary

Phase 4 observability expansion is complete for this portfolio.

Further work should focus on:

```text
enhanced-service validation playbooks
one planned runtime validation window
evidence refresh
evidence index quality
interview explanation after validation
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
