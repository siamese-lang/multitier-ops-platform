# Roadmap

This roadmap separates completed runtime validation, current service-linked operating scenario hardening, and final portfolio documentation.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short version:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
```

## Direction guardrails

This repository is not being restarted as a new project.

Fixed boundaries:

```text
1. Continue from the existing AWS EC2 repository and evidence.
2. Do not migrate this project to OCI for v1.0.
3. Do not turn the project into an OpenKoda installation project.
4. Do not turn the project into a Terraform, AWS managed architecture, Kubernetes, EKS, or GitOps showcase.
5. Do not close the project as documentation-only work while service-linked operating scenarios still need design or runtime validation.
```

Allowed work:

```text
1. Extend ops-sample-service when a concrete operating scenario requires service behavior.
2. Add or adjust Nginx, WAS, HikariCP, PostgreSQL, NFS, backup, logging, metrics, or deployment behavior when tied to evidence.
3. Add open-source tools only when they help diagnose, compare, or validate a concrete operating scenario.
4. Update documentation after implementation and runtime validation boundaries are clear.
```

The project must stay evidence-led:

```text
service behavior
-> WEB/WAS/DB/Storage operating problem
-> logs, metrics, config values, HTTP responses, DB rows, or file checksums
-> diagnosis or recovery action
-> before/after validation
```

## Status summary

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
Phase 5A. ops-sample-service domain/schema: implemented
Phase 5B. server-rendered work-order web workflow: implemented
Phase 5C. evidence upload/download workflow: implemented
Phase 5D. WEB/WAS failure-lab endpoints: implemented
Phase 5E. enhanced-service runtime validation: completed as first enhanced validation pass
Phase 6. service-linked operating scenario hardening: current focus
```

## Observability expansion policy

Phase 4 already provides enough baseline observability to support the completed evidence. However, observability work is not permanently frozen.

Do not expand into dashboard-first or tool-first observability work:

```text
Grafana dashboard-first work
Alertmanager notification maturity as a standalone goal
Loki platform expansion without a request/log correlation scenario
blackbox exporter work without a concrete service-impact question
PostgreSQL HA/failover
Kubernetes/EKS/GitOps
AWS managed architecture replacement
SLO/SLA compliance work
```

Observability tools may be introduced later only when they support a concrete operating scenario. Examples:

```text
- request ID correlation across Nginx and WAS logs
- PostgreSQL connection pressure diagnosis
- service impact comparison before and after a setting change
- alert-rule evidence for a specific failure mode
```

Observability remains supporting evidence for diagnosis. It is not the project theme.

## Completed operating evidence

### Phase 0. lab-runtime smoke test

Purpose:

```text
Verify temporary EC2 lab lifecycle, bastion/Ansible control path, private node NAT egress, workload start, health check, evidence collection, and destroy.
```

### Phase 1. lab-full-min WEB/WAS/DB

Validated topology:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

Completed validations:

```text
WEB/WAS/DB integrated normal path
app-01 failure and Nginx upstream bypass
app-01/app-02 rolling restart continuity
DB-backed concurrent request observation
PostgreSQL failure and recovery isolation
Terraform cleanup after validation
```

Key evidence:

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
```

### Phase 2A. lab-full-ops storage validation

Validated topology:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
```

Completed validations:

```text
NFS mount verification
NFS write-probe verification
work order creation through Nginx
work-order evidence file creation through Nginx
PostgreSQL metadata row verification
NFS file object size and SHA-256 verification
application consistency endpoint verification
Nginx request-id access log verification
Terraform destroy after evidence collection
```

Key evidence:

```text
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
```

### Phase 2B. backup artifact creation

Validated backup path:

```text
backup-01 -> db-primary-01:5432                  # pg_dump opsdb
backup-01 -> nfs-01:/srv/ops-sample/files       # NFS inventory/checksum/restic snapshot
```

Important boundary:

```text
Phase 2B proves backup artifact creation only.
The recovery claim comes from restore-lab validation.
```

Key evidence:

```text
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
```

### Phase 3. restore-lab DB/file/API recovery validation

Validated recovery flow:

```text
preserved backup artifact
-> backup-01
-> pg_restore to db-primary-01
-> restic restore to nfs-01
-> app-01 reads restored DB metadata and NFS file object
-> nginx-01 reverse proxy validates HTTP/API consistency
```

Supported claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
Backup artifact creation had already been validated separately in Phase 2B.
Restore was validated separately in restore-lab.
```

Key evidence:

```text
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md
```

### Phase 4A. logs, service state, and request-path observability evidence

Validated evidence scope:

```text
node health/resource state for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log visibility
ops-sample-service journald visibility
PostgreSQL service/log visibility
NFS export/filesystem visibility
backup/restore artifact and job-log visibility
request-path probe TSV/report
controlled DB service unavailable incident report
```

Key evidence:

```text
docs/04-evidence/observability-baseline-validation-2026-07-12.md
```

### Phase 4B/4C. Prometheus metrics validation and DB service incident diagnosis

Diagnostic distinction:

```text
Prometheus up{instance="db-primary-01:9100"}=1
  -> DB host reachable from mon-01

/readyz 503 and /api/work-orders/summary 503
  -> application DB dependency failing

PostgreSQL service inactive and port 5432 closed
  -> DB service unavailable, not DB host unavailable
```

Key evidence:

```text
docs/04-evidence/observability-metrics-validation-2026-07-12.md
```

### Phase 4D. Prometheus DB service alert-rule evaluation

Supported claim:

```text
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

Important boundary:

```text
This is Prometheus rule evaluation evidence.
It is not Alertmanager notification maturity evidence.
```

Key evidence:

```text
docs/04-evidence/observability-alert-validation-2026-07-12.md
```

## Completed enhanced-service runtime validation

The enhanced service implementation was validated as a first enhanced runtime pass.

Completed validation scope:

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

Restore-lab recovery evidence:

```text
docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md
```

Supported enhanced-service claims:

```text
enhanced web workflow was validated through AWS/Nginx/WAS
work-order create/status-change/evidence-file workflow was validated
upload-limit behavior was validated as an operating scenario
WAS sleep vs DB sleep latency behavior was validated as an operating scenario
DB web-impact behavior was validated against the enhanced service model
restore-lab recovery was refreshed against the enhanced service model
```

## Current focus: Phase 6 service-linked operating scenario hardening

The project is not finished just because the first enhanced runtime validation completed.

Phase 6 focuses on making the WEB/WAS operations portfolio deeper by connecting service behavior to infrastructure symptoms and evidence.

Current focus:

```text
1. Reframe documentation so the project is not read as documentation-only or tool-freeze work.
2. Extend ops-sample-service where required to reproduce realistic operating behavior.
3. Implement and validate WAS thread pool / HikariCP / PostgreSQL connection pressure behavior.
4. Runtime-validate release metadata and deployment rollback against real service smoke checks.
5. Keep claim boundaries strict while avoiding unnecessary AWS runtime creation.
6. Perform final README/evidence-index/interview documentation after runtime scope is complete.
```

Recommended next PR categories:

```text
[DOCS] Reframe phase 6 as service-linked scenario hardening
[APP] Add connection pressure workload behavior
[ANSIBLE] Add connection pressure validation playbook
[VALIDATION] Collect connection pressure runtime evidence
[VALIDATION] Runtime-validate release metadata and rollback behavior
[DOCS] Finalize evidence index and incident reports after runtime validation
```

## v1.0 remaining milestones

```text
M1. Direction correction
    - fix documentation-only / expansion-freeze wording
    - keep AWS EC2 as the runtime boundary
    - mark ops-sample-service as extensible for operating scenarios

M2. Service workload hardening
    - add only service behavior required by operating scenarios
    - prioritize DB hold / connection pressure behavior
    - keep feature work subordinate to WEB/WAS/DB evidence

M3. Connection pressure validation
    - reproduce HikariCP / PostgreSQL connection pressure
    - compare HTTP responses, Nginx logs, WAS logs, DB state, and settings
    - collect before/after evidence for a bounded setting change

M4. Deployment failure and rollback validation
    - runtime-check release metadata
    - validate bad deployment detection
    - rollback and verify core work-order/evidence service functions

M5. Final portfolio documentation
    - update README, evidence-index, incident reports, portfolio summary, and interview Q&A
    - do this after runtime evidence boundaries are final
```

## v1.0 exclusion list

Do not expand v1.0 into:

```text
OCI migration
OpenKoda adoption as the main workload
Blue/Green overengineering
GitHub Actions deployment automation as the main topic
EKS/GitOps migration
ALB/RDS redesign
Grafana dashboard project
Loki/Alertmanager platform project
JMeter/k6 large-scale load-testing project
PostgreSQL HA/failover
SLO/SLA compliance work
application feature development unrelated to operating evidence
```

## Safe claims by category

Runtime evidence supports:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
The enhanced work-order/evidence-file web workflow was validated as a lab runtime scenario.
Upload-limit, latency, and DB web-impact scenarios were validated against the enhanced service model.
```

Still not claimed:

```text
production operations experience
production monitoring maturity
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
