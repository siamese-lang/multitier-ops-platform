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
Prometheus-only monitoring practice
```

Terraform, Ansible, Spring Boot, Nginx, PostgreSQL, NFS, restic, node_exporter, and Prometheus are supporting tools only. The portfolio theme is EC2-based multi-tier operations, failure diagnosis, and recovery validation.

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

Do not run Terraform from WSL. Do not run Ansible from Git Bash.

## Core documents to read first

```text
README.md
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/04-evidence/evidence-index.md
```

For detailed runtime evidence, read the referenced documents in `docs/04-evidence/`.

## Current completed state

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
```

## Phase 4 freeze

Phase 4 is complete for this portfolio.

Do not continue with:

```text
Grafana dashboard-first work
Alertmanager notification maturity
Loki expansion
blackbox exporter expansion
additional Prometheus feature work
PostgreSQL HA/failover
Kubernetes/EKS/GitOps
new AWS runtime windows by default
```

The next work should be documentation and portfolio packaging only.

## Validated claims

The repository can now support these claims:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Not supported claims

Do not claim:

```text
production operations experience
production-grade monitoring maturity
Grafana dashboard readiness
Alertmanager notification maturity
paging or on-call workflow
PostgreSQL HA
automatic failover
SLO/SLA compliance
Kubernetes/EKS/GitOps operation
AWS managed architecture operation
```

## Runtime policy

Do not repeatedly create and destroy AWS resources.

Current default:

```text
No new AWS runtime.
No more observability feature expansion.
No more Prometheus/Grafana/Alertmanager expansion.
```

Only open a new AWS runtime if a clearly roadmap-aligned validation scenario is missing and cannot be documented from existing evidence. If such a runtime is opened, it must be one planned window:

```text
prepare statically -> apply once -> configure -> validate -> collect evidence -> destroy once
```

## Recommended next tasks

Use documentation-only tasks by default:

```text
[DOCS] Final pass for README/evidence link consistency
[DOCS] Polish Korean interview wording for target job postings
[DOCS] Add a simple architecture diagram if needed
[DOCS] Prepare GitHub repository URL submission note
```

Avoid new runtime tasks unless the user explicitly chooses a missing validation scenario.

## Prompt to start the next chat

```text
We are continuing the GitHub project `siamese-lang/multitier-ops-platform`.

Before doing any work, read these repository documents and treat them as the source of truth:

- README.md
- docs/00-project/project-scope.md
- docs/00-project/roadmap.md
- docs/00-project/workload-strategy.md
- docs/00-project/portfolio-summary.md
- docs/00-project/interview-explanation-notes.md
- docs/00-project/next-chat-handoff.md
- docs/04-evidence/evidence-index.md

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, not a Spring Boot sample app project, not Kubernetes/EKS/GitOps work, not a Grafana dashboard-first project, and not a Prometheus-only monitoring practice. It is a VM-based operations portfolio focused on WEB/WAS/DB/Storage/Backup/Observability tier separation, failure diagnosis, and recovery validation.

Current completed state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Phase 2A lab-full-ops storage validation completed.
- Phase 2B backup artifact creation completed as backup-artifact evidence.
- Phase 3 restore-lab DB/file/API recovery validation completed.
- Phase 4A logs/service/request-path observability evidence completed.
- Phase 4B node_exporter + Prometheus scrape metrics evidence completed.
- Phase 4C metric-based DB service incident diagnosis completed.
- Phase 4D Prometheus DB service alert-rule evaluation evidence completed.

Phase 4 is frozen. Do not create new AWS runtime by default. Do not add more Prometheus/Grafana/Alertmanager features by default. Next work should focus on final portfolio readability, evidence links, and interview explanation polish.
```
