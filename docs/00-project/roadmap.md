# Roadmap

This roadmap separates completed validation work from future operating-system expansion work.

The project theme is fixed:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Status summary

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. lab-full-ops backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed
Phase 4A. observability logs/service/request-path baseline evidence: completed
Phase 4B. node_exporter + Prometheus scrape metrics evidence: completed
Phase 4C. metric-based DB service incident diagnosis: completed
Phase 5. advanced operations: optional and only after roadmap-aligned evidence design
```

Important boundary:

```text
Backup artifact creation and restore validation were proven separately.
Phase 4 observability metrics validation has now been executed and documented.
The project may claim evidence-driven DB host reachability vs DB service dependency diagnosis,
but must not overclaim production-grade monitoring, HA, Alertmanager maturity, or SLO/SLA compliance.
```

## Phase 0. lab-runtime smoke test — completed

Purpose:

```text
Verify that the repository can create a temporary EC2 lab,
reach private nodes through a bastion,
pull dependencies through NAT,
run a workload,
collect evidence,
and destroy the environment.
```

Completed evidence:

```text
Terraform lab-runtime creation
Bastion-based Ansible control path
private app node NAT egress
Docker-based workload smoke test
health check evidence
Terraform destroy
```

Scope boundary:

```text
This phase does not prove the final operating architecture.
It only proves the lab execution mechanism and initial workload feasibility.
```

## Phase 1. lab-full-min WEB/WAS/DB — completed

Validated topology:

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01
- app-02

[Private DB Subnet]
- db-primary-01
```

Validated operating path:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

Completed validation scenarios:

```text
WEB/WAS/DB integrated normal path
app-01 failure and Nginx upstream bypass
app-01/app-02 rolling restart continuity
DB-backed concurrent request observation
PostgreSQL failure and recovery isolation
Terraform cleanup after validation
```

Key evidence documents:

```text
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
```

## Phase 2A. lab-full-ops storage validation — completed

Purpose:

```text
Extend the validated WEB/WAS/DB path with a storage tier and prove that
application-level DB metadata and NFS-backed file objects can be checked together.
```

Validated reduced topology:

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
```

Completed validation scenarios:

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

Runtime findings fixed:

```text
NFS export root permission mismatch under root_squash
app NFS mount playbook idempotency failure on already-mounted NFS root
stale jar artifact that lacked WorkOrderEvidence classes
systemd unit environment/path labeling mismatch
```

Key evidence document:

```text
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
```

## Phase 2B. lab-full-ops backup artifact creation — completed as backup-artifact evidence

Purpose:

```text
Create restorable backup artifacts for the already validated DB/file workload.
The goal was not to claim recovery yet; the goal was to prepare restore validation.
```

Validated backup path:

```text
backup-01 -> db-primary-01:5432                  # pg_dump opsdb
backup-01 -> nfs-01:/srv/ops-sample/files       # NFS file inventory/checksum/restic snapshot
```

Key backup identifiers:

```text
backup_id=lab-full-ops-backup-20260712T072623
pg_dump_size_bytes=7479
pg_dump_sha256=fe58367d5d43101461483a5054da4b8b520d2cc15e1e4c8ce2dc629082f78b0f
nfs_file_count=2
restic_snapshot_id=7f063aa1
metadata_counts=ops_work_order_evidence_files 1, ops_work_orders 6
restore_status=not_validated_at_backup_phase
```

Key evidence document:

```text
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
```

Important boundary:

```text
Phase 2B proves backup artifact creation only.
The recovery claim comes from Phase 3 restore-lab validation.
```

## Phase 3. restore-lab DB/file/API recovery validation — completed

Purpose:

```text
Prove that preserved backup artifacts can restore a working system in a separate environment.
```

Validated restore-lab topology:

```text
restore-lab VPC CIDR: 10.60.0.0/16

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
```

Validated recovery flow:

```text
1. Use preserved local backup artifact from Phase 2B.
2. Create a minimal restore-lab environment.
3. Copy backup artifacts to backup-01.
4. Restore PostgreSQL metadata with pg_restore.
5. Restore NFS-backed file objects from restic snapshot.
6. Start app and Nginx path against restored DB/file tiers.
7. Validate metadata row counts.
8. Validate sample evidence storage_path.
9. Validate sample file SHA-256.
10. Verify application consistency endpoint returns true.
11. Verify HTTP path through Nginx.
12. Preserve evidence bundle.
13. Destroy restore-lab resources after evidence collection.
```

Key restore identifiers:

```text
source_backup_id=lab-full-ops-backup-20260712T072623
restore_environment=restore-lab
pg_restore_status=validated
restic_restore_status=validated
actual_work_order_count=6
actual_evidence_file_count=1
api_consistency_status=consistent
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
actual_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
http_api_restore_status=validated
```

Runtime findings fixed after validation:

```text
NFS export CIDR needed restore-lab 10.60 app/backup subnets
PostgreSQL pg_hba needed restore-lab 10.60 app/backup subnets
restored file copy had to avoid preserving source uid/gid on root_squash NFS
HTTP/API summary total had to read data.total instead of summing total plus buckets
```

Key evidence document:

```text
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
```

Final Phase 3 claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
Backup artifact creation had already been validated separately in Phase 2B.
Restore was validated separately in restore-lab on 2026-07-12.
```

## Phase 4. Observability baseline and metrics evidence — completed

Purpose:

```text
Collect logs and metrics that support incident diagnosis.
The goal is not a pretty dashboard; the goal is evidence for operating decisions.
```

### Phase 4A. Logs, service state, and request-path evidence — completed

Implementation and runbooks:

```text
docs/01-architecture/observability-evidence-baseline.md
docs/03-runbooks/observability-baseline.md
docs/03-runbooks/observability-evidence-collection-baseline.md
infra/ansible/playbooks/observability-baseline.yml
```

Runtime evidence document:

```text
docs/04-evidence/observability-baseline-validation-2026-07-12.md
```

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

Supported Phase 4A claim:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

### Phase 4B. node_exporter and Prometheus scrape evidence — completed

Implementation and runbooks:

```text
infra/ansible/playbooks/observability-node-exporter-baseline.yml
infra/ansible/playbooks/observability-prometheus-scrape-baseline.yml
docs/03-runbooks/observability-node-exporter-baseline.md
docs/03-runbooks/observability-prometheus-scrape-baseline.md
docs/03-runbooks/observability-metrics-runtime-validation.md
```

Runtime evidence document:

```text
docs/04-evidence/observability-metrics-validation-2026-07-12.md
```

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

Terraform profile flags:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=true
nat_gateway=true
storage_node=true
```

Validated Prometheus scrape result:

```text
mon-01 Prometheus scraped:
- nginx-01:9100
- app-01:9100
- db-primary-01:9100
- nfs-01:9100
- backup-01:9100

Final post-fix evidence:
active_targets=5
healthy_targets=5
up_results=5
healthy_up_results=5
```

Supported Phase 4B claim:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

### Phase 4C. Metric-based DB service incident diagnosis — completed

Scenario:

```text
PostgreSQL service was stopped on db-primary-01 while node_exporter and Prometheus remained active.
```

Diagnostic distinction:

```text
Prometheus up{instance="db-primary-01:9100"}=1
  -> DB host reachable from mon-01

/readyz 503 and /api/work-orders/summary 503
  -> application DB dependency failing

PostgreSQL service inactive and port 5432 closed
  -> DB service unavailable, not DB host unavailable
```

Recovery evidence:

```text
PostgreSQL service active
port 5432 LISTEN
/readyz 200
/api/work-orders/summary 200
```

Supported Phase 4C claim:

```text
Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
```

### Phase 4 runtime findings fixed

```text
PR #126 defined observability_db_target default for controlled DB incidents.
PR #130 strengthened Prometheus scrape validation so evidence is collected only after target registration and job-specific up query stability.
```

Important Prometheus finding:

```text
Prometheus /-/ready only proves the server is ready.
It does not prove scrape targets are registered or healthy.
```

Local raw evidence archives:

```text
.tmp/observability-baseline-20260712T110244Z.tar.gz
.tmp/observability-metrics-20260712T121325Z.tar.gz
```

Raw evidence remains local and is not committed.

## Phase 5. Advanced operations — optional after core observability

Possible items:

```text
Prometheus alert rule evaluation for DB dependency symptoms
Loki/log query-based incident correlation
PostgreSQL standby and promote
Nginx active/active or failover entrypoint
HikariCP and DB connection pool bottleneck analysis
Tomcat thread saturation
p95/p99 latency comparison before/after tuning
```

Recommended next task:

```text
[ALERT] Add minimal Prometheus alert rule for DB service dependency evidence
```

Scope for the next task:

```text
Add one or two Prometheus rule files and validate rule evaluation evidence.
Do not claim Alertmanager notification maturity unless notification routing is separately configured and validated.
Do not start with Grafana dashboard polish.
```

Still not claimed:

```text
production monitoring maturity
Grafana dashboard readiness
Alertmanager notification maturity
PostgreSQL HA
automatic failover
SLO/SLA compliance
```

## Current handoff addendum

The most recent continuation addendum is:

```text
docs/00-project/current-state-after-observability-metrics.md
```

Read it with `docs/00-project/next-chat-handoff.md` when continuing this project in a new conversation.