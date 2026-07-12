# Portfolio summary

## Project title

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## One-line summary

```text
EC2 VM 기반 WEB/WAS/DB/Storage/Backup/Observability 운영환경을 직접 구성하고, 장애·복구 상황을 로그·지표·명령 결과로 검증한 운영 포트폴리오
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
7. Document supported claims without overclaiming production maturity.
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

Validated:

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

Validated:

```text
NFS export and mount
work-order evidence file creation
PostgreSQL metadata row creation
NFS file object existence
file size and SHA-256 match
application consistency endpoint
```

### 3. Backup artifact creation

Validated:

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

Validated:

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

Validated:

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

## Tools and their roles

| Tool | Role in this project | Not the project theme |
|---|---|---|
| Terraform | Create and destroy temporary AWS lab environments | Not a Terraform showcase |
| Ansible | Configure hosts and reproduce operating procedures | Not an Ansible role showcase |
| AWS EC2 | VM-style infrastructure substrate | Not an AWS managed architecture project |
| Nginx | WEB/reverse proxy tier | Not a web tuning-only project |
| Spring Boot/Tomcat | Controlled WAS workload | Not a Spring Boot feature project |
| PostgreSQL | DB tier for metadata and failure drills | Not HA database engineering |
| NFS | File storage tier for DB/file consistency checks | Not storage product evaluation |
| pg_dump/restic | Backup and restore tooling | Not backup product comparison |
| node_exporter/Prometheus | Metrics evidence for diagnosis | Not a monitoring platform project |

## Claims that are safe in an interview

```text
I built an EC2-based multi-tier operating environment with separated WEB/WAS/DB/Storage/Backup/Monitoring nodes.
I validated normal and failure paths with evidence rather than only screenshots.
I verified DB metadata and NFS file consistency with size and SHA-256 checks.
I created backup artifacts and then proved recovery in a separate restore-lab environment.
I used logs, service state, request-path responses, and Prometheus metrics to narrow a DB service incident.
I deliberately avoided overclaiming HA, production monitoring maturity, or managed cloud architecture experience.
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
```

## Project freeze boundary

Phase 4 is complete for this portfolio.

Further work should focus on:

```text
README clarity
evidence index quality
interview explanation
architecture diagrams if needed
removing ambiguity from docs
```

Further work should not focus on:

```text
more Prometheus features
Grafana dashboards
Alertmanager routing
Loki expansion
new AWS runtime windows
unrelated architecture expansion
```
