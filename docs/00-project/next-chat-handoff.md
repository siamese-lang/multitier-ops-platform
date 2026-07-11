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
docs/04-evidence/lab-full-min-web-was-db-integrated-validation.md
docs/04-evidence/lab-full-min-continuous-operations-validation.md
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

Important note:

```text
The final cleanup after the continuous validation session was operator-reported.
The final `terraform state list` output was not pasted into the issue thread.
Do not claim copied terminal evidence for state-empty unless separately provided.
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
Controlled workload used in lab-full-min to reproduce operating scenarios.
Provides health/readiness/node/DB-backed endpoints and request ID logs.
Not the final product.
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
OpenKoda feature/UI work
Terraform-only refactoring without an incident/recovery scenario
Kubernetes/EKS/GitOps work
managed AWS architecture replacement
Grafana dashboard-first work
creating many small issues without a roadmap link
```

## Recommended next issue

Create this next:

```text
[DESIGN] lab-full-ops 파일저장소·백업·관측성 확장 설계
```

Purpose:

```text
Move beyond the completed lab-full-min WEB/WAS/DB validation and design the next operating tiers: nfs-01, backup-01, mon-01, log-01, and loadgen-01.
```

Suggested scope:

```text
Define target topology.
Define whether files are on NFS or local filesystem abstraction.
Define DB metadata + file object consistency checks.
Define backup boundaries: PostgreSQL dump + file backup.
Define restore-lab target flow.
Define observability minimum: Prometheus node/app/PostgreSQL/Nginx metrics and Loki log collection.
Define the first Phase 2 implementation issue after design.
```

## Recommended next implementation sequence

```text
1. [DESIGN] lab-full-ops 파일저장소·백업·관측성 확장 설계
2. [TF] lab-full-ops node/subnet/security-group skeleton
3. [ANSIBLE] lab-full-ops inventory/control path
4. [ANSIBLE] nfs-01 file storage baseline
5. [APP] minimal file metadata/upload/download operational endpoint or harness
6. [INCIDENT] file storage failure and recovery drill
7. [ANSIBLE] pg_dump + restic backup baseline
8. [VALIDATION] restore-lab DB/file restore verification
9. [OBS] Prometheus/Loki minimum observability
10. [INCIDENT] metric/log-based incident report
```

## Response style for the next chat

When continuing in a new conversation, ask the assistant to:

```text
Use the repository documents as the source of truth.
Do not change the project theme.
Prefer fewer, larger roadmap-aligned issues over many small drifting issues.
Before implementing, check whether the work advances storage, backup/restore, observability, or incident evidence.
Keep Terraform and Ansible as supporting tools, not the portfolio theme.
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

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, and not a Spring Boot sample app project. It is a VM-based operations portfolio focused on WEB/WAS/DB/storage/observability/backup tier separation, failure diagnosis, and recovery validation.

Current completed state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Validated normal path, app-01 failure bypass, rolling restart continuity, DB-backed concurrent request observation, PostgreSQL failure/recovery, and cleanup.

Next recommended task:
[DESIGN] lab-full-ops 파일저장소·백업·관측성 확장 설계

Proceed from the roadmap and avoid creating unrelated issues or PRs.
```
