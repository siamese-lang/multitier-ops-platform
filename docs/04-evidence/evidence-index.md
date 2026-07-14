# Evidence index

This index maps portfolio claims to the evidence documents that support them.

The project theme is fixed:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Evidence policy

Raw runtime evidence is preserved locally under `.tmp`, `/tmp`, or a local evidence archive outside the repository and is not committed to the repository.

Repository evidence documents summarize:

```text
validated topology
runtime scenario
commands or playbooks used
evidence files collected
observed outputs
supported claims
unsupported claims
cleanup or runtime-retention status
```

The portfolio claim boundary is strict:

```text
A backup artifact is not recovery proof by itself.
Recovery is claimed only when restored DB/file data is validated in restore-lab through direct checks and HTTP/API consistency.
A monitoring rule evaluation is not production monitoring maturity.
A lab incident is not production operations experience.
Connection-pressure validation is bounded lab evidence, not production load testing or capacity sizing.
NFS mount failure validation is bounded lab evidence, not storage HA or automatic failover.
Nginx config rollback validation is bounded WEB-tier evidence, not production change-management maturity.
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
Phase 5E. enhanced-service runtime validation: completed
Phase 6A. bounded WEB/WAS/DB connection-pressure validation: completed
Phase 6B. bad WAS artifact deployment and rollback validation: completed
Phase 6C. app-side NFS mount failure and recovery validation: completed
Phase 6D. Nginx bad config detection and rollback validation: completed
Final cleanup. lab-full-ops AWS resources destroyed after evidence collection: completed
```

Final runtime summary:

```text
docs/04-evidence/final-runtime-validation-2026-07-13.md
```

## Representative interview scenario map

Use this table when reviewing the portfolio quickly or preparing for interviews. The interview Q&A is an explanation layer, not primary evidence. The evidence documents remain the source of truth for claims.

| Representative scenario | What it proves | Primary evidence | Interview explanation | Boundary |
|---|---|---|---|---|
| Nginx bad config detection and rollback | WEB-tier config candidates can be checked before reload, and a known-good config can be restored and validated through proxied service checks | `docs/04-evidence/final-runtime-validation-2026-07-13.md` | `docs/00-project/interview-incident-qna.md` Q8 | Not production change-management maturity, traffic safety guarantee, blue/green, or canary deployment |
| Bad WAS artifact deployment and rollback | A VM/systemd WAS deployment failure can be detected through service and health behavior, then validated after jar/env rollback | `docs/04-evidence/final-runtime-validation-2026-07-13.md` | `docs/00-project/interview-incident-qna.md` Q6 | Not production release management, zero-downtime deployment, blue/green, or canary deployment |
| Tomcat request-thread pressure vs HikariCP pool pressure | A delayed DB-backed API caused by WAS request-thread pressure can be distinguished from a failed DB-backed API caused by WAS-side DB connection-pool exhaustion | `docs/04-evidence/connection-pressure-validation-2026-07-13.md`, `docs/04-evidence/final-runtime-validation-2026-07-13.md` | `docs/00-project/interview-incident-qna.md` Q5 | Not production load testing, capacity sizing, SLO/SLA validation, or autoscaling evidence |
| App-side NFS mount failure and recovery | DB-backed work-order creation and file-storage-dependent evidence-file creation can fail differently, and recovery can be validated with DB metadata, NFS object, size, and SHA-256 | `docs/04-evidence/final-runtime-validation-2026-07-13.md` | `docs/00-project/interview-incident-qna.md` Q7 | Not production storage HA, automatic failover, NFS performance tuning, or chaos engineering |
| Backup artifact vs restore-lab recovery proof | Backup artifact creation and recovery proof are separate; recovery is claimed only after restore-lab DB/file/API consistency validation | `docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md`, `docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md` | `docs/00-project/interview-incident-qna.md` Q12 | Not production DR, RPO/RTO guarantee, continuous backup policy, or managed database recovery |

## Claim map

| Claim | Supporting evidence |
|---|---|
| EC2 temporary runtime lifecycle works through Terraform, bastion, Ansible, and cleanup | `lab-runtime` evidence, README, `docs/04-evidence/final-runtime-validation-2026-07-13.md` |
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
| Bounded WEB/WAS/DB connection pressure was validated through Nginx, embedded Tomcat, HikariCP, and PostgreSQL | `docs/04-evidence/connection-pressure-validation-2026-07-13.md`, `docs/04-evidence/final-runtime-validation-2026-07-13.md`, `docs/00-project/interview-incident-qna.md` Q5 |
| WAS request-thread pressure caused delayed but successful DB-backed API behavior | `docs/04-evidence/connection-pressure-validation-2026-07-13.md` |
| HikariCP pool pressure caused DB-backed API failure while PostgreSQL remained active | `docs/04-evidence/connection-pressure-validation-2026-07-13.md` |
| Bad WAS artifact deployment was detected and rolled back through jar/env restore | `docs/04-evidence/final-runtime-validation-2026-07-13.md`, raw local report archive, `docs/00-project/interview-incident-qna.md` Q6 |
| App-side NFS mount loss affected evidence-file creation while DB-backed work-order creation remained available | `docs/04-evidence/final-runtime-validation-2026-07-13.md`, raw local report archive, `docs/00-project/interview-incident-qna.md` Q7 |
| NFS remount restored evidence-file consistency with DB metadata, NFS object, size, and SHA-256 checks | `docs/04-evidence/final-runtime-validation-2026-07-13.md`, raw local report archive, `docs/00-project/interview-incident-qna.md` Q7 |
| Invalid Nginx config candidate was rejected by `nginx -t` before unsafe reload and restored config was reloaded successfully | `docs/04-evidence/final-runtime-validation-2026-07-13.md`, raw local report archive, `docs/00-project/interview-incident-qna.md` Q8 |
| AWS lab resources were destroyed after evidence collection | `docs/04-evidence/final-runtime-validation-2026-07-13.md`, local final-state Terraform evidence archive |

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

### Prometheus metrics validation

```text
docs/04-evidence/observability-metrics-validation-2026-07-12.md
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

### Connection pressure validation

```text
docs/04-evidence/connection-pressure-validation-2026-07-13.md
```

Supported claims:

```text
WAS request-thread pressure caused delayed but successful DB-backed summary response.
HikariCP connection-pool pressure caused DB-backed API failure while PostgreSQL remained active.
Nginx access logs, app journald logs, HikariCP pool state, PostgreSQL pg_stat_activity, and HTTP status/timing metrics supported failure-mode distinction.
```

Unsupported claims:

```text
production load testing
capacity sizing
external Tomcat/WAR operation
SLO/SLA validation
autoscaling behavior
PostgreSQL HA/failover
production incident response experience
```

### Final runtime validation and cleanup

```text
docs/04-evidence/final-runtime-validation-2026-07-13.md
```

Completed final runtime scenarios:

```text
connection pressure validation
bad WAS artifact deployment and rollback validation
app-side NFS mount failure and recovery validation
Nginx bad config detection and rollback validation
AWS lab cleanup/destroy after evidence collection
```

Supported claims:

```text
The final lab-full-ops runtime validation window was completed.
The project now has WEB, WAS, WAS/DB, Storage, Backup/Restore, and Observability evidence categories.
AWS lab resources were destroyed after evidence collection.
```

Unsupported claims:

```text
production operations experience
production change management
production load testing
production storage HA
production DR
RPO/RTO guarantee
zero-downtime release guarantee
blue-green/canary deployment
```

## Service implementation references

These documents describe the current enhanced service implementation:

```text
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
docs/00-project/ops-sample-service-completion-scope.md
```
