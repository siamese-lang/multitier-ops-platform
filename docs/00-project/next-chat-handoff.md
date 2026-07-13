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

Current operated service identity:

```text
ops-sample-service = 운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
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
docs/00-project/current-state-after-enhanced-runtime-validation.md
docs/00-project/workload-strategy.md
docs/00-project/ops-sample-service-completion-scope.md
docs/00-project/enhanced-service-operations-scenarios.md
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/00-project/next-chat-handoff.md
docs/04-evidence/evidence-index.md
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
```

For detailed runtime evidence, read the referenced documents in `docs/04-evidence/`.

## Current completed runtime evidence state

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
Phase 5E. enhanced-service runtime validation: completed as first enhanced validation pass
```

Enhanced validation scope completed:

```text
S1 enhanced service workflow validation: completed
S2 upload-limit incident validation: completed
S3 latency scenario validation: completed
S4 DB web-impact incident validation: completed
Backup baseline: completed
Restore-lab DB/file restore baseline: completed
Restore-lab HTTP/API consistency validation: completed
Source lab destroy: completed
Restore lab destroy: completed
```

Key state document:

```text
docs/00-project/current-state-after-enhanced-runtime-validation.md
```

## Current service state

`ops-sample-service` is implemented as a lightweight web service for operations work orders and evidence files.

Implemented capabilities:

```text
work-order list/detail/create/status-change pages
status history and operation audit logs
evidence upload/download workflow
PostgreSQL metadata + file storage object consistency path
failure-lab: sleep, db-sleep, file-storage-check, upload-limits
```

The service can answer this interview question:

```text
What service did you operate?
```

Target answer:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며, 조치 메모와 증빙 파일을 남길 수 있습니다. PostgreSQL에는 작업 요청과 파일 메타데이터를 저장하고, NFS에는 실제 첨부파일을 저장하도록 분리했습니다.
```

Important boundary:

```text
The service is intentionally lightweight. It exists to make WEB/WAS/DB/Storage/Backup operating scenarios explainable.
It is not a commercial ITSM clone or production service.
```

## Validated claims

The repository can currently support these runtime evidence claims:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
The enhanced work-order/evidence-file web workflow was validated in AWS runtime.
Upload-limit, latency, and DB web-impact scenarios were validated against the enhanced service model.
Restore-lab recovery was refreshed against the enhanced service model.
```

## Claims not supported

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
commercial ITSM implementation
production disaster recovery
RPO/RTO guarantee
```

## Runtime policy

Do not repeatedly create and destroy AWS resources.

Current default:

```text
No new AWS runtime by default.
No more observability feature expansion.
No more Prometheus/Grafana/Alertmanager expansion.
Documentation and portfolio hardening first.
```

If a future runtime validation is justified, use one planned validation window:

```text
prepare statically -> apply once -> configure -> validate -> collect evidence -> destroy once
```

## Current next tasks

The next phase is portfolio hardening, not project shutdown.

Recommended next tasks:

```text
[DOCS] Harden evidence-index claim mapping
[DOCS] Add incident reports for enhanced operating scenarios
[DOCS] Update interview explanation notes after enhanced validation
[DOCS] Improve portfolio summary for infrastructure / WEB-WAS operations interviews
[VALIDATION] Plan one VM/systemd deployment rollback scenario only if needed
```

Suggested incident report documents:

```text
docs/05-incident-reports/upload-limit-incident-report.md
docs/05-incident-reports/latency-diagnosis-incident-report.md
docs/05-incident-reports/db-web-impact-incident-report.md
docs/05-incident-reports/restore-lab-recovery-incident-report.md
```

Each incident report should use this structure:

```text
Scenario
User-visible symptom
Impact scope
Initial hypotheses
Layer-by-layer checks
Observed evidence
Root-cause judgment
Action taken
Recovery validation
Remaining limits
Interview explanation points
```

## Optional future validation

If one more runtime window is worth the cost, prioritize:

```text
VM/systemd app deployment and rollback scenario
```

Avoid:

```text
EKS/GitOps migration
ALB/RDS redesign
Blue/Green overengineering
Grafana dashboard work
Alertmanager routing work
unrelated architecture expansion
```

## Prompt to start the next chat

```text
We are continuing the GitHub project `siamese-lang/multitier-ops-platform`.

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, not a Spring Boot sample app project, not Kubernetes/EKS/GitOps work, not a Grafana dashboard-first project, and not a Prometheus-only monitoring practice. It is a VM-based operations portfolio focused on WEB/WAS/DB/Storage/Backup/Observability tier separation, failure diagnosis, and recovery validation.

Before doing work, read:
- README.md
- docs/00-project/project-scope.md
- docs/00-project/roadmap.md
- docs/00-project/current-state-after-enhanced-runtime-validation.md
- docs/00-project/portfolio-summary.md
- docs/00-project/interview-explanation-notes.md
- docs/00-project/next-chat-handoff.md
- docs/04-evidence/evidence-index.md
- apps/ops-sample-service/README.md
- apps/ops-sample-service/FAILURE_LAB.md

Current completed evidence includes Phase 0-4, enhanced service validation S1-S4, backup baseline, restore-lab DB/file restore baseline, restore-lab HTTP/API consistency validation, and both source/restore lab destroy.

Do not open AWS runtime by default. The next task is portfolio hardening: evidence-index, incident reports, interview notes, and claim-boundary cleanup.
```
