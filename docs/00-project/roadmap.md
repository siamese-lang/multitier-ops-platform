# Roadmap

This roadmap separates completed runtime validation, current portfolio-hardening work, and optional future validation.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short version:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
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
Phase 6. portfolio hardening and interview readiness: current focus
```

## Phase 4 freeze decision

Phase 4 observability expansion is complete enough for the portfolio objective.

Do not continue expanding this project into:

```text
Grafana dashboard-first work
Alertmanager notification maturity
Loki platform expansion
blackbox exporter HTTP monitoring
PostgreSQL HA/failover
Kubernetes/EKS/GitOps
AWS managed architecture replacement
SLO/SLA compliance work
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

## Current focus: Phase 6 portfolio hardening

The project is not finished just because the first enhanced runtime validation completed.

Current focus:

```text
1. Make README and evidence-index reflect validated claims accurately.
2. Convert raw validation evidence into interview-ready incident reports.
3. Strengthen portfolio-summary and interview-explanation notes.
4. Keep claim boundaries strict.
5. Avoid unnecessary AWS runtime creation.
```

Recommended next PR categories:

```text
[DOCS] Add incident reports for enhanced operating scenarios
[DOCS] Harden evidence index claim mapping
[DOCS] Improve interview explanation notes
[VALIDATION] Add VM/systemd deployment rollback scenario only if justified
```

## Optional future runtime validation

Do not open AWS runtime by default.

If one more validation window is justified, prioritize:

```text
VM/systemd app deployment and rollback scenario
```

Reason:

```text
The current project already has strong failure, latency, DB-impact, backup, and restore evidence.
A deployment/rollback scenario would strengthen WEB/WAS operations interview readiness without changing the project theme.
```

Do not expand into:

```text
Blue/Green overengineering
GitHub Actions deployment automation as the main topic
EKS/GitOps migration
ALB/RDS redesign
Grafana dashboard work
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
