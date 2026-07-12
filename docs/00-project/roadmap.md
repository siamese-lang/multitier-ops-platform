# Roadmap

This roadmap separates completed validation work from service-completion work and optional future expansion.

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
Phase 5. ops-sample-service completion: current focus
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

The next work should be service completion, not another observability feature.

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

## Current focus: Phase 5 service completion

The next work should not create AWS resources by default.

Current focus:

```text
ops-sample-service를 작업 요청·증빙 파일 관리형 경량 웹 업무 서비스로 보강
service domain/schema
server-rendered web UI
status transition and event history
evidence upload/download workflow
operations dashboard and failure lab
validation playbooks for enhanced workflow
```

Source-of-truth design document:

```text
docs/00-project/ops-sample-service-completion-scope.md
```

Recommended next PR categories:

```text
[APP] Add work order domain and schema
[APP] Add basic server-rendered web UI
[APP] Add evidence upload/download workflow
[APP] Add status transition and event history
[APP] Add operations dashboard and failure lab
[ANSIBLE] Add validation for enhanced service workflow
[VALIDATION] Refresh restore-lab evidence after service completion
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
completed web business service workflow
completed evidence upload/download workflow through enhanced web pages
service-level WEB/WAS timeout, thread, connection-pool, or slow-query validation
```
