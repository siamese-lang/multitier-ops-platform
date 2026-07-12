# Roadmap

This roadmap separates completed runtime validation, completed service implementation baseline, and remaining enhanced-service validation work.

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
Phase 5E. enhanced-service validation and evidence refresh: current focus
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

The next work should be enhanced-service validation, not another observability feature.

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
The recovery claim comes from Phase 3 restore-lab validation.
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
Restore was validated separately in restore-lab on 2026-07-12.
```

Key evidence:

```text
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
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

Supported claim:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

Key evidence:

```text
docs/04-evidence/observability-baseline-validation-2026-07-12.md
```

### Phase 4B. node_exporter and Prometheus scrape metrics evidence

Validated monitoring-enabled topology:

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

Supported claim:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

Key evidence:

```text
docs/04-evidence/observability-metrics-validation-2026-07-12.md
```

### Phase 4C. metric-based DB service incident diagnosis

Diagnostic distinction:

```text
Prometheus up{instance="db-primary-01:9100"}=1
  -> DB host reachable from mon-01

/readyz 503 and /api/work-orders/summary 503
  -> application DB dependency failing

PostgreSQL service inactive and port 5432 closed
  -> DB service unavailable, not DB host unavailable
```

Supported claim:

```text
Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
```

Key evidence:

```text
docs/04-evidence/observability-metrics-validation-2026-07-12.md
```

### Phase 4D. Prometheus DB service alert-rule evaluation

Validated rule condition:

```text
DB host is reachable from Prometheus.
PostgreSQL service is inactive on db-primary-01.
Prometheus ALERTS query returns firing result.
```

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

## Completed service implementation baseline

`ops-sample-service` is now implemented as:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

Implemented service capabilities:

```text
work-order domain/schema
server-rendered web UI
work-order list/detail/create/status-change workflow
status history and operation audit logs
evidence upload/download workflow
PostgreSQL metadata + file storage object consistency path
WEB/WAS failure-lab page and APIs
```

Relevant app docs:

```text
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
docs/00-project/ops-sample-service-completion-scope.md
```

## Current focus: Phase 5E enhanced-service validation

The next work should not create AWS resources by default.

Current focus:

```text
Ansible validation prep for enhanced web workflow
static syntax and playbook checks
one planned runtime validation window only when ready
collect enhanced-service evidence
destroy once
refresh evidence docs
```

Recommended next PR categories:

```text
[ANSIBLE] Add enhanced service workflow validation
[VALIDATION] Document enhanced web workflow evidence
[VALIDATION] Refresh backup/restore evidence after service completion
[DOCS] Update evidence index after enhanced-service validation
```

## Runtime policy

Do not run Terraform apply/destroy for every small PR.

```text
Documentation cleanup -> no AWS runtime
Application implementation -> local/static checks first
Ansible syntax or static prep -> no AWS runtime unless a scenario needs evidence
Enhanced service validation -> one planned runtime window only
Runtime window -> collect evidence -> destroy once
```

## Safe claims by category

Runtime evidence already supports:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

Repository implementation supports:

```text
ops-sample-service now includes a lightweight web work-order/evidence-file workflow.
The service includes web upload/download and DB/file consistency paths.
The service includes failure-lab endpoints for slow request, DB sleep, file storage, and upload-limit inspection.
```

Still not claimed until enhanced runtime evidence exists:

```text
completed AWS runtime validation of the enhanced web workflow
completed AWS runtime validation of evidence upload/download through Nginx/WAS/NFS/PostgreSQL
completed service-level Nginx timeout, thread, connection-pool, or slow-query validation
refreshed restore-lab validation against the enhanced service model
```

## Still not claimed

```text
production monitoring maturity
Grafana dashboard readiness
Alertmanager notification maturity
paging or on-call workflow
PostgreSQL HA
automatic failover
SLO/SLA compliance
Kubernetes/EKS/GitOps operation
AWS managed architecture operation
production service operation
commercial ITSM implementation
```
