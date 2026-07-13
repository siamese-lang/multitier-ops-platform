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

The portfolio claim boundary is strict:

```text
A backup artifact is not recovery proof by itself.
Recovery is claimed only when restored DB/file data is validated in restore-lab through direct checks and HTTP/API consistency.
A monitoring rule evaluation is not production monitoring maturity.
A lab incident is not production operations experience.
```

## Current validation state

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. lab-full-ops backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed
Phase 4A. observability logs/service/request-path evidence: completed
Phase 4B. node_exporter + Prometheus scrape metrics evidence: completed
Phase 4C. metric-based DB service incident diagnosis: completed
Phase 4D. Prometheus DB service alert-rule evaluation evidence: completed
Phase 5E. enhanced-service runtime validation: completed as first enhanced validation pass
```

Enhanced validation scope completed:

```text
S1 enhanced service workflow validation: completed
S2 upload-limit incident validation: completed
S3 latency scenario validation: completed
S4 DB web-impact incident validation: completed
Backup baseline: completed
Restore-lab DB/file restore baseline: completed
Restore-lab HTTP/API consistency validation: completed
Source lab destroy: completed
Restore lab destroy: completed
```

Current-state document:

```text
docs/00-project/current-state-after-enhanced-runtime-validation.md
```

## Claim map

| Claim | Supporting evidence |
|---|---|
| EC2 temporary runtime lifecycle works through Terraform, bastion, Ansible, and cleanup | `lab-runtime` evidence and README/roadmap Phase 0 |
| WEB/WAS/DB normal path was validated | `docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md` |
| WAS failure and rolling restart behavior were validated | `docs/04-evidence/lab-full-min-continuous-operations-validation.md` |
| Storage tier was integrated with DB metadata and NFS file objects | `docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md` |
| Backup artifacts were created for DB metadata and file objects | `docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md` |
| Recovery was validated in a separate restore-lab environment | `docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md`, `docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md` |
| Logs, service state, and request paths support diagnosis | `docs/04-evidence/observability-baseline-validation-2026-07-12.md` |
| Prometheus scraped node_exporter metrics from operating nodes | `docs/04-evidence/observability-metrics-validation-2026-07-12.md` |
| Prometheus metrics distinguished DB host reachability from DB service failure | `docs/04-evidence/observability-metrics-validation-2026-07-12.md` |
| Prometheus rule evaluation detected PostgreSQL inactivity while DB host stayed reachable | `docs/04-evidence/observability-alert-validation-2026-07-12.md` |
| `ops-sample-service` includes a lightweight work-order/evidence-file web workflow | repository implementation: `apps/ops-sample-service/README.md`, `apps/ops-sample-service/FAILURE_LAB.md` |
| Enhanced service workflow was validated as a runtime operating scenario | `docs/00-project/current-state-after-enhanced-runtime-validation.md` |
| Upload-limit incident was validated as a WEB/WAS operating scenario | `docs/00-project/current-state-after-enhanced-runtime-validation.md` |
| WAS sleep vs DB sleep latency behavior was validated | `docs/00-project/current-state-after-enhanced-runtime-validation.md` |
| DB web-impact incident was validated against the enhanced service model | `docs/00-project/current-state-after-enhanced-runtime-validation.md` |
| Restore-lab recovery was refreshed against the enhanced service model | `docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md` |

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
docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md
```

2026-07-13 restore-lab recovery used:

```text
source_backup_id=lab-full-ops-backup-20260712T182247
pg_restore_status=validated
restic_restore_status=validated
actual_work_order_count=13
actual_evidence_file_count=3
restored_work_order_count=13
api_consistency_status=consistent
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
http_api_restore_status=validated
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

Unsupported claims:

```text
production disaster recovery
RPO/RTO guarantee
Multi-AZ high availability
automatic failover
continuous backup policy
managed database recovery
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

### Enhanced service runtime validation

```text
docs/00-project/current-state-after-enhanced-runtime-validation.md
```

Completed validation scope:

```text
S1 enhanced service workflow validation
S2 upload-limit incident validation
S3 latency scenario validation
S4 DB web-impact incident validation
backup baseline
restore-lab DB/file restore baseline
restore-lab HTTP/API consistency validation
source lab destroy
restore lab destroy
```

Supported evidence categories:

```text
work-order page smoke
work-order create/status-change workflow
status history and audit log visibility
evidence upload/download path
DB metadata and NFS file object consistency
request ID based WEB/WAS log correlation
upload-limit behavior
WAS sleep vs DB sleep latency behavior
DB web-impact behavior
restore-lab recovery after enhanced service model
```

## Service implementation references

These documents describe the current enhanced service implementation:

```text
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
docs/00-project/ops-sample-service-completion-scope.md
```

## Evidence archives kept locally

Known local archives from validation windows include `.tmp/` or `/tmp` evidence bundles. They are intentionally not committed.

Examples referenced by evidence documents include:

```text
.tmp/observability-baseline-20260712T110244Z.tar.gz
.tmp/observability-metrics-20260712T121325Z.tar.gz
.tmp/observability-alert-20260712T131524Z.tar.gz
.tmp/restore-lab-runtime-20260713T091446
```

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
commercial ITSM implementation
production disaster recovery
RPO/RTO guarantee
```

## Current portfolio-hardening work

The next project phase is not additional feature work by default.

Current focus:

```text
incident report layer
interview explanation notes
portfolio summary quality
claim-to-evidence readability
optional VM/systemd deployment rollback validation if justified
```

Recommended incident report documents:

```text
docs/05-incident-reports/upload-limit-incident-report.md
docs/05-incident-reports/latency-diagnosis-incident-report.md
docs/05-incident-reports/db-web-impact-incident-report.md
docs/05-incident-reports/restore-lab-recovery-incident-report.md
```
