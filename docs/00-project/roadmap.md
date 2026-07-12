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
Phase 4. observability baseline: design and Ansible baseline prepared; runtime validation pending
Phase 5. advanced incident reports and failover: optional after core observability
```

Important boundary:

```text
Backup artifact creation and restore validation were proven separately.
The project may now claim restore-lab DB/file/API recovery validation success,
but should not overclaim production-grade HA, automated failover, or managed backup coverage.
Phase 4 observability runtime validation has not yet been executed.
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

Validated operating path:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
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

Related fix PR:

```text
PR #119 [FIX] Make restore-lab runtime validation profile-aware
```

Final Phase 3 claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
Backup artifact creation had already been validated separately in Phase 2B.
Restore was validated separately in restore-lab on 2026-07-12.
```

## Phase 4. Observability baseline — design and Ansible baseline prepared; runtime validation pending

Purpose:

```text
Collect logs and metrics that support incident diagnosis.
The goal is not a pretty dashboard; the goal is evidence for operating decisions.
```

Prepared documents and implementation baseline:

```text
docs/01-architecture/observability-evidence-baseline.md
docs/03-runbooks/observability-baseline.md
docs/03-runbooks/observability-evidence-collection-baseline.md
infra/ansible/playbooks/observability-baseline.yml
```

Prepared evidence scope:

```text
node health/resource state for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log visibility
ops-sample-service journald visibility
PostgreSQL service/log visibility
NFS export/filesystem visibility
backup/restore artifact and job-log visibility
request-path probe TSV/report
optional DB service unavailable incident report
```

Optional incident guardrail:

```text
observability_run_db_service_incident=false by default
```

This default prevents accidental PostgreSQL service stops during ordinary baseline collection.

Runtime validation is still pending. A future validation window should use the existing policy:

```text
apply once -> configure baseline -> collect observability evidence
-> run optional DB service incident -> recover -> collect evidence -> destroy once
```

A future Phase 4 runtime evidence PR may claim only the narrow result:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

It must not claim:

```text
production monitoring maturity
complete Prometheus/Loki/Grafana platform coverage
Alertmanager maturity
HA or automated failover
SLO/SLA compliance
```

Recommended next task:

```text
[VALIDATION] Run and document observability baseline evidence
```

Important boundary:

```text
Do not start with Grafana dashboard polish.
Do not expand Prometheus/Loki before basic logs and metrics are used in an incident report.
Do not turn Phase 4 into a CloudWatch-managed architecture or dashboard gallery.
```

## Phase 5. Advanced operations — optional after core observability

Possible items:

```text
PostgreSQL standby and promote
Nginx active/active or failover entrypoint
HikariCP and DB connection pool bottleneck analysis
Tomcat thread saturation
Prometheus alert rule and Alertmanager notification
Loki log query-based incident diagnosis
p95/p99 latency comparison before/after tuning
```
