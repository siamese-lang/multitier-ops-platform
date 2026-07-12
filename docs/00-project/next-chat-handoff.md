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
Grafana dashboard project
```

## Repository

```text
repo: siamese-lang/multitier-ops-platform
local Windows/Git Bash path: /c/Project/test/multitier-ops-platform
local WSL path: /mnt/c/Project/test/multitier-ops-platform
```

Do not use old D drive paths.

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
It is not the final product, but it now supports WEB/WAS/DB plus DB/file consistency checks.
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
re-running the full storage validation window without a new backup/restore/observability reason
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
[ANSIBLE] Add lab-full-ops pg_dump and file backup baseline
```

Purpose:

```text
Move beyond storage validation by preparing backup artifacts for PostgreSQL metadata and NFS file objects.
This should lead directly to restore-lab verification, not stop at backup creation.
```

Suggested scope:

```text
Define backup boundaries for opsdb and /srv/ops-sample/files.
Add pg_dump baseline for db-primary-01.
Add file backup baseline for nfs-01, preferably restic or an explicitly documented archive workflow.
Store or stage artifacts through backup-01 or a clearly documented backup target.
Generate a work-order evidence dataset before backup.
Capture artifact inventory and checksum evidence.
Do not call the phase complete until restore-lab validates recovery.
```

## Recommended next implementation sequence

```text
1. [ANSIBLE] lab-full-ops pg_dump and file backup baseline
2. [VALIDATION] backup artifact inventory and checksum evidence
3. [TF] restore-lab minimal node profile, if not already available
4. [ANSIBLE] restore DB and file artifacts into restore-lab
5. [VALIDATION] restore-lab DB/file/API consistency verification
6. [OBS] Prometheus/Loki minimum observability
7. [INCIDENT] metric/log-based incident report
```

## Runtime policy

```text
Do not run Terraform apply/destroy for every small PR.
Use static checks for documentation and Ansible syntax changes.
Open an AWS runtime window only when a new scenario requires evidence.
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
Do not re-open AWS runtime validation until backup/restore/observability work needs it.
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

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, and not a Spring Boot sample app project. It is a VM-based operations portfolio focused on WEB/WAS/DB/storage/observability/backup tier separation, failure diagnosis, and recovery validation.

Current completed state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Phase 2A lab-full-ops storage validation completed.
- Storage validation proved Nginx -> app -> PostgreSQL metadata plus NFS file object consistency.
- Runtime findings from the storage validation were fixed in the follow-up PR.
- AWS resources from the validation window were destroyed.

Next recommended task:
[ANSIBLE] Add lab-full-ops pg_dump and file backup baseline

Proceed from the roadmap and avoid creating unrelated issues or PRs.
```
