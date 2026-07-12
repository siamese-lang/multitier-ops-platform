# Evidence index

This index maps portfolio claims to the evidence documents that support them.

The project theme is fixed:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Evidence policy

Raw runtime evidence is preserved locally under `.tmp` or `/tmp` archives and is not committed to the repository.

Repository evidence documents summarize:

```text
validated topology
runtime scenario
commands or playbooks used
evidence files collected
observed outputs
supported claims
unsupported claims
cleanup status
```

## Claim map

| Claim | Supporting evidence |
|---|---|
| EC2 temporary runtime lifecycle works through Terraform, bastion, Ansible, and cleanup | `lab-runtime` evidence and README/roadmap Phase 0 |
| WEB/WAS/DB normal path was validated | `docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md` |
| WAS failure and rolling restart behavior were validated | `docs/04-evidence/lab-full-min-continuous-operations-validation.md` |
| Storage tier was integrated with DB metadata and NFS file objects | `docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md` |
| Backup artifacts were created for DB metadata and file objects | `docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md` |
| Recovery was validated in a separate restore-lab environment | `docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md` |
| Logs, service state, and request paths support diagnosis | `docs/04-evidence/observability-baseline-validation-2026-07-12.md` |
| Prometheus scraped node_exporter metrics from operating nodes | `docs/04-evidence/observability-metrics-validation-2026-07-12.md` |
| Prometheus metrics distinguished DB host reachability from DB service failure | `docs/04-evidence/observability-metrics-validation-2026-07-12.md` |
| Prometheus rule evaluation detected PostgreSQL inactivity while DB host stayed reachable | `docs/04-evidence/observability-alert-validation-2026-07-12.md` |

## Core evidence documents

### WEB/WAS/DB

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
```

Supported evidence:

```text
Nginx -> app -> PostgreSQL path
health/readiness checks
Nginx upstream behavior
app failure handling
rolling restart continuity
DB-backed request observation
PostgreSQL failure and recovery isolation
```

### Storage and DB/file consistency

```text
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
```

Supported evidence:

```text
NFS export and app mount
work-order evidence file creation
PostgreSQL metadata row
NFS file object size
NFS file object SHA-256
application consistency endpoint
Nginx request-id access log
```

### Backup artifact creation

```text
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
```

Supported evidence:

```text
pg_dump artifact
pg_dump SHA-256
NFS file inventory
restic snapshot
backup metadata counts
backup artifact archive preservation
```

Boundary:

```text
This evidence proves backup artifact creation.
It does not by itself prove recovery.
```

### Restore-lab recovery

```text
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
```

Supported evidence:

```text
separate restore-lab environment
pg_restore result
restic restore result
restored metadata row counts
sample evidence file existence
sample file size match
sample file SHA-256 match
HTTP/API consistency through Nginx
```

Supported claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
```

### Observability baseline

```text
docs/04-evidence/observability-baseline-validation-2026-07-12.md
```

Supported evidence:

```text
node health/resource state
Nginx service and logs
app service and journald logs
PostgreSQL service/log visibility
NFS export/filesystem visibility
request-path probe evidence
controlled DB service unavailable incident report
```

Supported claim:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

### Prometheus metrics validation

```text
docs/04-evidence/observability-metrics-validation-2026-07-12.md
```

Supported evidence:

```text
node_exporter on nginx-01, app-01, db-primary-01, nfs-01, backup-01
Prometheus on mon-01
node_exporter scrape targets
up query results
DB service incident with db-primary-01 host metrics still available
/readyz and summary path failures during DB service outage
post-recovery service/API checks
```

Supported claims:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
```

### Prometheus alert rule validation

```text
docs/04-evidence/observability-alert-validation-2026-07-12.md
```

Supported evidence:

```text
DB node_exporter systemd collector enabled
node_systemd_unit_state for postgresql.service
Prometheus rule loaded through /api/v1/rules
normal state had no firing alert
incident state produced ALERTS firing result
postgresql active metric became 0
DB node_exporter up metric remained 1
post-incident PostgreSQL and API recovery succeeded
Terraform destroy completed after evidence collection
```

Supported claim:

```text
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

Important boundary:

```text
This is rule evaluation evidence.
It is not Alertmanager notification maturity evidence.
```

## Evidence archives kept locally

Known local archives from the 2026-07-12 validation windows include:

```text
.tmp/observability-baseline-20260712T110244Z.tar.gz
.tmp/observability-metrics-20260712T121325Z.tar.gz
.tmp/observability-alert-20260712T131524Z.tar.gz
```

Earlier backup/restore raw evidence archives were also preserved locally and are referenced in their evidence documents.

## Claims not supported by this evidence set

Do not claim:

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

## Current evidence status

```text
Core operations evidence: sufficient for portfolio summary
Storage/backup/restore evidence: sufficient for recovery narrative
Observability evidence: sufficient for diagnosis narrative
Prometheus extension work: frozen after alert rule evaluation
AWS runtime: not needed for default next work
```
