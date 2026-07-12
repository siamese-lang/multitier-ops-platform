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

Current service direction:

```text
ops-sample-service를 운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스로 보강한다.
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
commercial ITSM product clone
```

Terraform, Ansible, Spring Boot, Nginx, PostgreSQL, NFS, restic, node_exporter, and Prometheus are supporting tools only. The portfolio theme is EC2-based multi-tier operations, WEB/WAS failure diagnosis, and recovery validation.

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
docs/00-project/ops-sample-service-completion-scope.md
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/04-evidence/evidence-index.md
```

For detailed runtime evidence, read the referenced documents in `docs/04-evidence/`.

## Current completed evidence state

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

## Current unfinished part

The infrastructure and evidence baseline are meaningful, but the operated service is not yet strong enough for final portfolio positioning.

The service must be completed enough to answer this interview question without defensive wording:

```text
What service did you operate?
```

Target answer after implementation:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며, 조치 메모와 증빙 파일을 남길 수 있습니다. PostgreSQL에는 작업 요청과 파일 메타데이터를 저장하고, NFS에는 실제 첨부파일을 저장하도록 분리했습니다.
```

Do not use this answer as a completed claim until the web workflow and evidence are implemented and validated.

## Phase 4 freeze remains valid

Phase 4 observability expansion is frozen.

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

The next work should focus on service completion, not observability expansion.

## Validated claims

The repository can currently support these claims:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Claims not yet supported

Do not claim yet:

```text
ops-sample-service is a completed web business service.
The project includes a completed work-order web workflow.
The project validates status changes and action history through web pages.
The project validates full evidence upload/download workflow through the enhanced web service.
The project validates WEB/WAS timeout, thread, connection-pool, or slow-query behavior through service-level scenarios.
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

For service implementation PRs, use static and local checks first. Open a new AWS runtime only after the service enhancement is ready for one planned validation window:

```text
prepare statically -> apply once -> configure -> validate -> collect evidence -> destroy once
```

## Recommended next tasks

Use implementation tasks by default, not more portfolio packaging:

```text
[APP] Add work order domain and schema
[APP] Add basic server-rendered web UI
[APP] Add evidence upload/download workflow
[APP] Add status transition and event history
[APP] Add operations dashboard and failure lab
[ANSIBLE] Add validation for enhanced service workflow
[VALIDATION] Refresh restore-lab evidence after service completion
```

Avoid new runtime tasks until the service implementation is ready for validation.

## Prompt to start the next chat

```text
We are continuing the GitHub project `siamese-lang/multitier-ops-platform`.

Before doing any work, read these repository documents and treat them as the source of truth:

- README.md
- docs/00-project/project-scope.md
- docs/00-project/roadmap.md
- docs/00-project/workload-strategy.md
- docs/00-project/ops-sample-service-completion-scope.md
- docs/00-project/portfolio-summary.md
- docs/00-project/interview-explanation-notes.md
- docs/00-project/next-chat-handoff.md
- docs/04-evidence/evidence-index.md

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, not a Spring Boot sample app project, not Kubernetes/EKS/GitOps work, not a Grafana dashboard-first project, and not a Prometheus-only monitoring practice. It is a VM-based operations portfolio focused on WEB/WAS/DB/Storage/Backup/Observability tier separation, failure diagnosis, and recovery validation.

Current completed evidence state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Phase 2A lab-full-ops storage validation completed.
- Phase 2B backup artifact creation completed as backup-artifact evidence.
- Phase 3 restore-lab DB/file/API recovery validation completed.
- Phase 4A logs/service/request-path observability evidence completed.
- Phase 4B node_exporter + Prometheus scrape metrics evidence completed.
- Phase 4C metric-based DB service incident diagnosis completed.
- Phase 4D Prometheus DB service alert-rule evaluation evidence completed.

Phase 4 observability expansion is frozen. Do not create new AWS runtime by default. Do not add more Prometheus/Grafana/Alertmanager features by default.

Current priority is service completion: make `ops-sample-service` explainable as a lightweight web service for operations work orders and evidence files. Start from `docs/00-project/ops-sample-service-completion-scope.md`. Next work should implement the service domain/schema, then web UI, then upload/download, status history, operations dashboard, and validation.
```
