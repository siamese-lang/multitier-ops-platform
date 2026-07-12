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

## Repository

```text
repo: siamese-lang/multitier-ops-platform
local Windows/Git Bash path: /c/Project/test/multitier-ops-platform
local WSL path: /mnt/c/Project/test/multitier-ops-platform
```

Use Git Bash for Terraform and WSL for Ansible. Do not mix those execution environments because SSH key paths, `/tmp`, and installed tools differ.

## Core documents to read first

Read these before creating new issues or PRs:

```text
README.md
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/next-chat-handoff.md
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
```

## Current completed state

### Phase 0. lab-runtime smoke test

Completed.

Purpose:

```text
Verify temporary EC2 lab lifecycle, bastion/Ansible control path, private node NAT egress, workload start, health check, evidence collection, and destroy.
```

### Phase 1. lab-full-min WEB/WAS/DB

Completed.

Validated topology:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

Completed components:

```text
Terraform lab-full-min baseline
Ansible lab-full-min inventory/control path
PostgreSQL primary playbook
ops-sample-service systemd deployment
Nginx HTTPS reverse proxy
GitHub Actions jar artifact workflow
Nginx HTTPS ingress
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

### Phase 2A. lab-full-ops storage validation

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

Runtime findings and follow-up fixes:

```text
1. NFS write failed because export root was root:root 0775 under root_squash.
   Follow-up fixed storage defaults to nobody:nogroup 0777 for the lab export root.

2. Re-running app NFS mount failed because the playbook tried to chown an already-mounted NFS root from the app node.
   Follow-up made the mount playbook skip local ownership enforcement once the NFS path is mounted.

3. The first evidence smoke failed at POST /api/work-orders/{id}/evidence-files with upstream 404.
   Nginx and the basic work-order API were healthy, so the issue was isolated to a stale app artifact.
   Follow-up added required jar entry checks for WorkOrderEvidence classes.

4. The shared systemd unit still said lab-full-min and did not explicitly allow the evidence file root path.
   Follow-up aligned the unit description and ReadWritePaths with the runtime environment/evidence root.
```

Important cleanup status:

```text
The AWS runtime validation window was completed and resources were destroyed by the operator.
Do not recreate the lab-full-ops AWS environment just to re-check documentation or syntax-only follow-up PRs.
```

### Phase 2B. lab-full-ops backup artifact creation

Backup artifact creation evidence collected.

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
restore_status=not_validated
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
Backup artifact creation was validated.
Restore has not been validated yet.
Do not claim recovery proof until restore-lab validates DB/file/API consistency from these artifacts.
```

Cleanup status:

```text
The operator reported Terraform destroy completed after backup artifact preservation.
Future runtime windows should also capture terraform state list after destroy as an explicit evidence file.
```

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
more lab-full-min drills with the same pattern
re-running the full storage or backup validation window without a new restore/observability reason
OpenKoda feature/UI work
Terraform-only refactoring without an incident/recovery scenario
Kubernetes/EKS/GitOps work
managed AWS architecture replacement
Grafana dashboard-first work
creating many small issues without a roadmap link
```

## Recommended next task

Next recommended task:

```text
[DESIGN] Define restore-lab DB/file recovery validation path
```

Purpose:

```text
Turn the preserved backup artifacts into a recovery proof.
The next milestone must restore PostgreSQL metadata and NFS file objects in a separate restore-lab, then verify API consistency.
```

Suggested scope:

```text
Define restore-lab topology and minimum nodes.
Decide how preserved local backup artifacts are injected into restore-lab.
Define pg_restore procedure for opsdb.
Define restic restore procedure for file objects.
Verify metadata row counts, sample storage_path, sample SHA-256, and HTTP/API consistency.
Document recovery gaps and any manual steps.
```

## Recommended next implementation sequence

```text
1. [VALIDATION] Document lab-full-ops backup artifact evidence
2. [DESIGN] Define restore-lab DB/file recovery validation path
3. [TF] Add restore-lab minimal recovery profile, if a separate profile is needed
4. [ANSIBLE] Restore DB and file artifacts into restore-lab
5. [VALIDATION] restore-lab DB/file/API consistency verification
6. [OBS] Prometheus/Loki minimum observability after recovery path is proven
7. [INCIDENT] metric/log-based incident report
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
- docs/04-evidence/lab-full-ops-storage-validation-2026-07-12.md
- docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, and not a Spring Boot sample app project. It is a VM-based operations portfolio focused on WEB/WAS/DB/storage/observability/backup tier separation, failure diagnosis, and recovery validation.

Current completed state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Phase 2A lab-full-ops storage validation completed.
- Phase 2B backup artifact creation evidence collected.
- Backup validation produced pg_dump, NFS file inventory/checksum, restic snapshot, manifest, and preserved local artifact archives.
- Restore has not been validated yet.
- AWS resources from the backup validation window were destroyed.

Next recommended task:
[DESIGN] Define restore-lab DB/file recovery validation path

Proceed from the roadmap and avoid creating unrelated issues or PRs.
```
