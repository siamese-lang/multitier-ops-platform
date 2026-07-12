# Next chat handoff

Use this document when continuing the project in a new ChatGPT conversation.

## Fixed project theme

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short version:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
```

Do not reinterpret the project as:

```text
OpenKoda installation
Terraform practice
AWS managed architecture showcase
Spring Boot sample app project
Kubernetes/EKS/GitOps project
Grafana dashboard-first project
```

Terraform, Ansible, Spring Boot, Nginx, PostgreSQL, NFS, restic, Prometheus, Grafana, and Loki are supporting tools only. The portfolio theme is EC2-based multi-tier operations, failure diagnosis, and recovery validation.

## Repository and local execution split

```text
repo: siamese-lang/multitier-ops-platform
local Windows/Git Bash path: /c/Project/test/multitier-ops-platform
local WSL path: /mnt/c/Project/test/multitier-ops-platform
```

Use the established execution split:

```text
Git Bash = Terraform only
WSL      = Ansible, Git work, evidence organization
AWS      = apply once -> validation -> evidence collect -> destroy once
```

Do not run Terraform from WSL in this project workflow. Do not run Ansible from Git Bash.

## Core documents to read first

Read these before creating new issues or PRs:

```text
README.md
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/next-chat-handoff.md
docs/01-architecture/restore-lab-recovery-validation.md
docs/01-architecture/observability-evidence-baseline.md
docs/03-runbooks/restore-lab-db-file-restore-baseline.md
docs/03-runbooks/restore-lab-http-api-consistency-check.md
docs/03-runbooks/observability-baseline.md
docs/03-runbooks/observability-evidence-collection-baseline.md
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md
```

## Current completed state

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. lab-full-ops backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed
Phase 4. observability baseline: design and Ansible baseline prepared; runtime validation pending
```

## Phase 0. lab-runtime smoke test

Completed.

Purpose:

```text
Verify temporary EC2 lab lifecycle, bastion/Ansible control path, private node NAT egress, workload start, health check, evidence collection, and destroy.
```

## Phase 1. lab-full-min WEB/WAS/DB

Completed.

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

## Phase 2A. lab-full-ops storage validation

Completed.

Validated reduced runtime topology:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
```

Runtime nodes used:

```text
bastion-01
nginx-01
app-01
db-primary-01
nfs-01
backup-01
NAT Gateway enabled only for the batched validation window
```

Validated scenarios:

```text
Ansible control path across public and private nodes
PostgreSQL primary configuration
NFS server export baseline
app-01 NFS mount baseline
NFS write probe
ops-sample-service deployment with evidence file root
Nginx reverse proxy
work order creation through Nginx
work-order evidence file creation through Nginx
PostgreSQL evidence metadata row verification
NFS file object size and SHA-256 verification
application consistency endpoint verification
Nginx request-id access log verification
Terraform destroy after evidence collection
```

Runtime findings fixed:

```text
NFS write failed because export root was root:root 0775 under root_squash.
App NFS mount re-run failed because the playbook tried to chown an already-mounted NFS root.
First evidence smoke failed at POST /api/work-orders/{id}/evidence-files due to a stale app artifact.
The shared systemd unit description and ReadWritePaths needed alignment with the ops runtime.
```

## Phase 2B. lab-full-ops backup artifact creation

Completed as backup-artifact evidence.

Validated backup path:

```text
backup-01 -> db-primary-01:5432                  # pg_dump opsdb
backup-01 -> nfs-01:/srv/ops-sample/files       # NFS inventory/checksum/restic snapshot
```

Key backup evidence:

```text
backup_id=lab-full-ops-backup-20260712T072623
pg_dump_size_bytes=7479
pg_dump_sha256=fe58367d5d43101461483a5054da4b8b520d2cc15e1e4c8ce2dc629082f78b0f
nfs_file_count=2
restic_snapshot_id=7f063aa1
metadata_counts=ops_work_order_evidence_files 1, ops_work_orders 6
sample_evidence_metadata includes storage_path, size, and SHA-256
restore_status=not_validated_at_backup_phase
backup-01 PLAY RECAP unreachable=0 failed=0
```

Preserved local evidence under WSL:

```text
/tmp/lab-full-ops-backup-validation
/tmp/lab-full-ops-backup-validation-lab-full-ops-backup-20260712T072623.tar.gz
```

The backup artifact archive is local evidence and is not committed to the repository.

Important boundary:

```text
Backup artifact creation was validated in Phase 2B.
Restore was validated later and separately in Phase 3.
Do not treat backup creation alone as recovery proof.
```

## Phase 3. restore-lab DB/file/API recovery validation

Completed.

Restore-lab used a separate CIDR:

```text
restore-lab VPC CIDR: 10.60.0.0/16
```

Validated recovery flow:

```text
preserved backup artifact
-> backup-01
-> pg_restore to db-primary-01
-> restic restore to nfs-01
-> app-01 reads restored DB metadata and NFS file object
-> nginx-01 reverse proxy path validates HTTP/API consistency
```

Key restore evidence:

```text
source_backup_id=lab-full-ops-backup-20260712T072623
restore_environment=restore-lab
pg_restore_status=validated
restic_restore_status=validated
actual_work_order_count=6
actual_evidence_file_count=1
sample_work_order_id=6
sample_evidence_id=1
sample_storage_path=work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
expected_sample_size_bytes=215
actual_sample_size_bytes=215
expected_sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
actual_sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
api_consistency_status=consistent
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
http_api_restore_status=validated
```

Nginx path verified:

```text
/healthz
/readyz
/api/work-orders/summary
/api/work-orders/6/evidence-files
/api/work-orders/6/evidence-files/1/consistency
```

Runtime findings fixed in PR #119:

```text
NFS export CIDR needed restore-lab 10.60 app/backup subnets.
PostgreSQL pg_hba needed restore-lab 10.60 app/backup subnets.
Restored file copy had to avoid preserving source uid/gid on root_squash NFS.
HTTP/API summary total had to read data.total instead of summing total plus buckets.
```

Final Phase 3 claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
Backup artifact creation had already been validated separately in Phase 2B.
Restore was validated separately in restore-lab on 2026-07-12.
```

AWS resources from the restore-lab validation window were destroyed after evidence collection.

## Phase 4. Observability baseline

Current status:

```text
Design completed.
Ansible baseline prepared.
Runtime validation pending.
```

Prepared files:

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

Important safety default:

```text
observability_run_db_service_incident=false
```

This prevents accidental PostgreSQL service stops during ordinary baseline collection. Enable it only during a planned runtime validation window.

Phase 4 has not yet produced runtime evidence. Do not claim observability validation success until a runtime evidence PR documents actual collected logs/metrics and an incident report.

## Workload relationship

OpenKoda:

```text
Candidate business workload and Phase 0 smoke-test workload.
Not the project theme.
Not authored by this repository.
```

ops-sample-service:

```text
Controlled workload used to reproduce operating scenarios.
It supports WEB/WAS/DB plus DB/file consistency checks.
It remains a supporting workload, not the portfolio theme.
```

Current useful capabilities:

```text
health/readiness/node endpoints
DB-backed work-order endpoints
request ID logging
work-order evidence file creation
PostgreSQL metadata + NFS file object consistency endpoint
```

Future workload decision:

```text
Use whichever workload best supports operations evidence.
If OpenKoda cannot cleanly support WEB/WAS/DB/file/backup/observability drills, keep it as Phase 0 evidence and continue with controlled workload extensions.
```

## What not to do next

Avoid:

```text
re-running the full storage or backup validation window without a new restore/observability reason
claiming Phase 4 success without runtime evidence
Grafana dashboard-first work
Prometheus/Loki platform expansion before the basic incident report exists
OpenKoda feature/UI work
Terraform-only refactoring without an incident/recovery scenario
Kubernetes/EKS/GitOps work
managed AWS architecture replacement
creating many small issues without a roadmap link
```

## Recommended next task

Next recommended task:

```text
[VALIDATION] Run and document observability baseline evidence
```

Purpose:

```text
Run one planned AWS runtime validation window for Phase 4.
Collect node, service, log, request-path, and optional DB-service-incident evidence.
Destroy AWS resources after evidence collection.
Document the result in docs/04-evidence.
```

Suggested runtime sequence:

```text
1. WSL: confirm repository and Ansible syntax checks.
2. Git Bash: terraform apply once for the chosen lab-full-ops profile.
3. WSL: populate inventories/lab-full-ops/hosts.yml from Terraform outputs.
4. WSL: run Ansible ping.
5. WSL: configure the existing WEB/WAS/DB/NFS/Backup baseline as needed.
6. WSL: run observability-baseline.yml with the DB incident disabled first.
7. WSL: if baseline evidence is healthy, optionally re-run with observability_run_db_service_incident=true.
8. WSL: preserve evidence under /tmp/observability-baseline-validation-*.
9. Git Bash: terraform destroy once.
10. WSL/Git: create docs/04-evidence/observability-baseline-validation-YYYY-MM-DD.md.
```

Narrow success claim after runtime evidence:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

Do not claim:

```text
production monitoring maturity
complete Prometheus/Grafana/Loki platform coverage
Alertmanager maturity
HA or automated failover
SLO/SLA compliance
```

## Runtime policy

```text
Do not run Terraform apply/destroy for every small PR.
Use static checks for documentation and Ansible syntax changes.
Open an AWS runtime window only when a new restore/observability/incident scenario requires evidence.
When NAT Gateway is enabled for package installation, collect evidence and destroy immediately.
```

## Response style for the next chat

When continuing in a new conversation, ask the assistant to:

```text
Use the repository documents as the source of truth.
Do not change the project theme.
Prefer fewer, larger roadmap-aligned issues over many small drifting issues.
Before implementing, check whether the work advances storage, backup/restore, observability, or incident evidence.
Keep Terraform and Ansible as supporting tools, not the portfolio theme.
Do not ask the user to run local Maven; use GitHub Actions artifacts or documented artifact checks.
Do not re-open AWS runtime validation until restore/observability/incident work needs it.
```

## Prompt to start the next chat

```text
We are continuing the GitHub project `siamese-lang/multitier-ops-platform`.

Before doing any work, read the following repository documents and treat them as the source of truth:

- README.md
- docs/00-project/project-scope.md
- docs/00-project/roadmap.md
- docs/00-project/workload-strategy.md
- docs/00-project/next-chat-handoff.md
- docs/01-architecture/restore-lab-recovery-validation.md
- docs/01-architecture/observability-evidence-baseline.md
- docs/03-runbooks/observability-baseline.md
- docs/03-runbooks/observability-evidence-collection-baseline.md
- docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
- docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
- docs/04-evidence/restore-lab-recovery-validation-2026-07-12.md

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, not a Spring Boot sample app project, not Kubernetes/EKS/GitOps work, and not a Grafana dashboard-first project. It is a VM-based operations portfolio focused on WEB/WAS/DB/storage/backup/observability tier separation, failure diagnosis, and recovery validation.

Current completed state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Phase 2A lab-full-ops storage validation completed.
- Phase 2B backup artifact creation completed as backup-artifact evidence.
- Phase 3 restore-lab DB/file/API recovery validation completed.
- Phase 4 observability design and Ansible evidence collection baseline are prepared.
- Phase 4 runtime validation has not yet been executed.

Next recommended task:
[VALIDATION] Run and document observability baseline evidence

Proceed from the roadmap and avoid creating unrelated issues or PRs.
```
