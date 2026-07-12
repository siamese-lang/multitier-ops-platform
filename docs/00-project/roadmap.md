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
Phase 2B. lab-full-ops backup/restore baseline: next
Phase 2C. lab-full-ops observability baseline: next after backup path
Phase 3. restore-lab recovery validation: future, required
Phase 4. advanced incident reports and failover: optional after core recovery
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

Completed build items:

```text
Terraform lab-full-min baseline
Ansible inventory/control path
PostgreSQL primary configuration
ops-sample-service systemd deployment
Nginx HTTPS reverse proxy
GitHub Actions jar artifact workflow
Nginx HTTPS ingress rule
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

Important boundary:

```text
Do not keep adding similar lab-full-min drills unless they clearly unlock Phase 2.
The minimum WEB/WAS/DB operating story is already strong enough.
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

Temporary validation support:
- NAT Gateway enabled only for package installation during the batched runtime window
```

Validated operating path:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
```

Completed build items:

```text
Terraform reduced lab-full-ops profile
lab-full-ops inventory/control path
PostgreSQL primary wrapper for lab-full-ops
nfs-01 export baseline
app-01 NFS client mount baseline
ops-sample-service lab-full-ops deployment
Nginx reverse proxy wrapper for lab-full-ops
work-order evidence smoke playbook
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

Runtime findings that were fixed:

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

Important boundary:

```text
Do not repeat the full AWS runtime window for small follow-up PRs.
Use syntax/static checks until a new backup, restore, observability, or incident scenario requires runtime evidence.
```

## Phase 2B. lab-full-ops backup baseline — next

Purpose:

```text
Create a backup path for the already validated DB/file workload.
The goal is not merely backup creation; the goal is to prepare restore validation.
```

Recommended next implementation order:

```text
1. Define backup artifact boundaries for PostgreSQL metadata and NFS file objects.
2. Add pg_dump baseline for db-primary-01.
3. Add restic or equivalent file backup baseline for nfs-01.
4. Store backup artifacts on backup-01 or an explicitly documented backup target.
5. Generate a new evidence dataset through the work-order evidence flow.
6. Capture backup command output and artifact inventory.
7. Do not call the backup phase complete until restore-lab can verify recovery.
```

Minimum required evidence:

```text
pg_dump command output
NFS file backup command output
backup artifact inventory
metadata/file checksum mapping
cleanup/destroy policy
```

## Phase 2C. lab-full-ops observability baseline — next after backup path

Purpose:

```text
Collect logs and metrics that support incident diagnosis.
The goal is not a pretty dashboard; the goal is evidence for operating decisions.
```

Recommended scope:

```text
node metrics for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log visibility
ops-sample-service journald/request-id log visibility
PostgreSQL service/log visibility
NFS/storage host metrics
one incident report that uses logs or metrics to narrow the failure class
```

## Phase 3. restore-lab — required future milestone

Purpose:

```text
Prove that backup artifacts can restore a working system in a separate environment.
```

Target flow:

```text
Create lab-full-ops data
Create PostgreSQL dump
Create file backup
Destroy lab-full-ops resources
Create restore-lab resources
Restore DB and file backup
Validate DB rows, file checksum, and HTTP/API consistency
Document recovery time and recovery gaps
```

Required evidence:

```text
pg_dump command output
restic or file-backup snapshot output
Terraform destroy and new apply
restore command output
checksum comparison
HTTP/API endpoint verification
incident/recovery report
```

This phase is important because it proves recovery rather than merely backup creation.

## Phase 4. Advanced operations — optional after core recovery

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

These should be added only after Phase 2 backup/observability and Phase 3 restore evidence exist.

## Work that should stop now

Avoid the following unless a clear evidence gap is identified:

```text
more lab-full-min drills with the same pattern
repeating the storage validation runtime window without a new scenario
OpenKoda UI or feature work
Terraform-only refactoring without an operating scenario
GitHub issue/PR churn without validation value
dashboard-first monitoring work
managed AWS replacement of VM-based tiers
```

## Definition of done for the portfolio

The project reaches a strong portfolio state when the repository includes:

```text
architecture documents
Terraform environment lifecycle
Ansible configuration/runbooks
WEB/WAS/DB/file/observability/backup tier descriptions
at least four incident or recovery scenarios
logs/metrics/command evidence
restore-lab recovery proof
portfolio summary for reviewers
```
