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
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
docs/00-project/project-scope.md
docs/00-project/roadmap.md
docs/00-project/workload-strategy.md
docs/00-project/ops-sample-service-completion-scope.md
docs/00-project/enhanced-service-operations-scenarios.md
docs/00-project/portfolio-summary.md
docs/00-project/interview-explanation-notes.md
docs/00-project/next-chat-handoff.md
docs/03-runbooks/lab-full-ops-enhanced-service-workflow-validation.md
docs/03-runbooks/lab-full-ops-enhanced-upload-limit-incident.md
docs/03-runbooks/lab-full-ops-enhanced-latency-scenario.md
docs/03-runbooks/lab-full-ops-enhanced-db-web-impact-incident.md
docs/03-runbooks/restore-lab-enhanced-service-validation.md
docs/04-evidence/evidence-index.md
```

For detailed runtime evidence, read the referenced documents in `docs/04-evidence/`.

## Current completed runtime evidence state

```text
Phase 0. lab-runtime smoke test: completed
Phase 1. lab-full-min WEB/WAS/DB minimum environment: completed
Phase 2A. lab-full-ops storage validation: completed
Phase 2B. lab-full-ops backup artifact creation: completed as backup-artifact evidence
Phase 3. restore-lab DB/file/API recovery validation: completed for the earlier workload model
Phase 4A. observability logs/service/request-path evidence: completed
Phase 4B. node_exporter + Prometheus scrape metrics evidence: completed
Phase 4C. metric-based DB service incident diagnosis: completed
Phase 4D. Prometheus DB service alert-rule evaluation evidence: completed
```

## Current service implementation and validation-prep state

The service implementation baseline is complete in repository code:

```text
Phase 5A. work-order domain/schema: implemented
Phase 5B. server-rendered work-order web workflow: implemented
Phase 5C. evidence upload/download workflow: implemented
Phase 5D. WEB/WAS failure-lab endpoints: implemented
Phase 5E. enhanced service workflow validation playbook: statically prepared
Phase 5F. enhanced service operations scenario matrix: defined
Phase 5G. upload failure and upload-limit incident validation playbook: statically prepared
Phase 5H. WAS sleep vs DB sleep latency scenario validation playbook: statically prepared
Phase 5I. DB service incident web-impact validation playbook: statically prepared
Phase 5J. enhanced restore-lab service validation playbook: statically prepared
```

Implemented service capabilities:

```text
work-order list/detail/create/status-change pages
status history and operation audit logs
evidence upload/download workflow
PostgreSQL metadata + file storage object consistency path
failure-lab: sleep, db-sleep, file-storage-check, upload-limits
```

Validation prep and scenario planning added:

```text
infra/ansible/playbooks/lab-full-ops-enhanced-service-workflow-validation.yml
docs/03-runbooks/lab-full-ops-enhanced-service-workflow-validation.md
infra/ansible/playbooks/lab-full-ops-enhanced-upload-limit-incident.yml
docs/03-runbooks/lab-full-ops-enhanced-upload-limit-incident.md
infra/ansible/playbooks/lab-full-ops-enhanced-latency-scenario.yml
docs/03-runbooks/lab-full-ops-enhanced-latency-scenario.md
infra/ansible/playbooks/lab-full-ops-enhanced-db-web-impact-incident.yml
docs/03-runbooks/lab-full-ops-enhanced-db-web-impact-incident.md
infra/ansible/playbooks/restore-lab-enhanced-service-validation.yml
docs/03-runbooks/restore-lab-enhanced-service-validation.md
docs/00-project/enhanced-service-operations-scenarios.md
```

The service can now answer this interview question without defensive wording:

```text
What service did you operate?
```

Target answer:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며, 조치 메모와 증빙 파일을 남길 수 있습니다. PostgreSQL에는 작업 요청과 파일 메타데이터를 저장하고, NFS에는 실제 첨부파일을 저장하도록 분리했습니다.
```

Important boundary:

```text
The enhanced service implementation, scenario matrix, baseline validation playbook, incident playbooks, and enhanced restore-lab validation playbook are complete.
Enhanced AWS runtime evidence is not yet refreshed.
```

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

The next work should focus on one planned enhanced-service runtime validation window, not more app features or observability expansion.

## Validated claims

The repository can currently support these runtime evidence claims:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment for the earlier workload model.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

The repository can currently support these implementation and prep claims:

```text
ops-sample-service includes a lightweight web workflow for operations work orders and evidence files.
The service includes work-order pages, status history, audit logs, evidence upload/download, and failure-lab endpoints.
The enhanced service workflow validation playbook is prepared, but not yet runtime-validated.
The upload failure and upload-limit incident validation playbook is prepared, but not yet runtime-validated.
The latency scenario validation playbook is prepared, but not yet runtime-validated.
The DB web-impact incident validation playbook is prepared, but not yet runtime-validated.
The enhanced restore-lab service validation playbook is prepared, but not yet runtime-validated.
The enhanced service operations scenario matrix is defined, but incident-specific runtime evidence is not yet collected.
```

## Claims not yet supported by refreshed runtime evidence

Do not claim yet:

```text
The enhanced web workflow has been runtime-validated through AWS/Nginx/WAS.
The evidence upload/download workflow has been runtime-validated through Nginx/WAS/NFS/PostgreSQL.
Upload failure isolation has been validated across Nginx/WAS/NFS/PostgreSQL.
The failure-lab sleep/db-sleep scenarios have been validated with logs and metrics.
The DB service incident has been validated against the enhanced web workflow.
The restore-lab recovery has been refreshed against the enhanced service model.
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
commercial ITSM implementation
```

## Runtime policy

Do not repeatedly create and destroy AWS resources.

Current default:

```text
No new AWS runtime by default.
No more observability feature expansion.
No more Prometheus/Grafana/Alertmanager expansion.
```

For enhanced service validation, use one planned validation window:

```text
prepare statically -> apply once -> configure -> validate -> collect evidence -> destroy once
```

## Recommended next tasks

The static prep is now complete enough to run one planned runtime window:

```text
[VALIDATION] Run one planned enhanced-service AWS validation window
[VALIDATION] Document enhanced-service runtime evidence
[DOCS] Update evidence index after enhanced-service validation
[DOCS] Update portfolio/interview/submission notes after validated evidence
```

The scenario matrix is the planning source of truth:

```text
docs/00-project/enhanced-service-operations-scenarios.md
```

Avoid app feature expansion unless validation exposes a concrete defect.

## Prompt to start the next chat

```text
We are continuing the GitHub project `siamese-lang/multitier-ops-platform`.

Before doing any work, read these repository documents and treat them as the source of truth:

- README.md
- apps/ops-sample-service/README.md
- apps/ops-sample-service/FAILURE_LAB.md
- docs/00-project/project-scope.md
- docs/00-project/roadmap.md
- docs/00-project/workload-strategy.md
- docs/00-project/ops-sample-service-completion-scope.md
- docs/00-project/enhanced-service-operations-scenarios.md
- docs/00-project/portfolio-summary.md
- docs/00-project/interview-explanation-notes.md
- docs/00-project/next-chat-handoff.md
- docs/03-runbooks/lab-full-ops-enhanced-service-workflow-validation.md
- docs/03-runbooks/lab-full-ops-enhanced-upload-limit-incident.md
- docs/03-runbooks/lab-full-ops-enhanced-latency-scenario.md
- docs/03-runbooks/lab-full-ops-enhanced-db-web-impact-incident.md
- docs/03-runbooks/restore-lab-enhanced-service-validation.md
- docs/04-evidence/evidence-index.md

The fixed project theme is:

AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

This is not an OpenKoda installation project, not a Terraform showcase, not a Spring Boot sample app project, not Kubernetes/EKS/GitOps work, not a Grafana dashboard-first project, and not a Prometheus-only monitoring practice. It is a VM-based operations portfolio focused on WEB/WAS/DB/Storage/Backup/Observability tier separation, failure diagnosis, and recovery validation.

Current completed runtime evidence state:
- Phase 0 lab-runtime smoke test completed.
- Phase 1 lab-full-min WEB/WAS/DB completed.
- Phase 2A lab-full-ops storage validation completed.
- Phase 2B backup artifact creation completed as backup-artifact evidence.
- Phase 3 restore-lab DB/file/API recovery validation completed for the earlier workload model.
- Phase 4A logs/service/request-path observability evidence completed.
- Phase 4B node_exporter + Prometheus scrape metrics evidence completed.
- Phase 4C metric-based DB service incident diagnosis completed.
- Phase 4D Prometheus DB service alert-rule evaluation evidence completed.

Current service implementation and validation-prep state:
- `ops-sample-service` is now implemented as a lightweight web service for operations work orders and evidence files.
- It includes work-order list/detail/create/status-change pages, status history, audit logs, evidence upload/download, DB/file consistency paths, and WEB/WAS failure-lab endpoints.
- Enhanced workflow validation is statically prepared in `infra/ansible/playbooks/lab-full-ops-enhanced-service-workflow-validation.yml`.
- Upload failure isolation validation is statically prepared in `infra/ansible/playbooks/lab-full-ops-enhanced-upload-limit-incident.yml`.
- WAS sleep vs DB sleep latency validation is statically prepared in `infra/ansible/playbooks/lab-full-ops-enhanced-latency-scenario.yml`.
- DB web-impact validation is statically prepared in `infra/ansible/playbooks/lab-full-ops-enhanced-db-web-impact-incident.yml`.
- Enhanced restore-lab validation is statically prepared in `infra/ansible/playbooks/restore-lab-enhanced-service-validation.yml`.
- Enhanced service operations scenarios are defined in `docs/00-project/enhanced-service-operations-scenarios.md`.

Phase 4 observability expansion is frozen. Do not create new AWS runtime by default. Do not add more Prometheus/Grafana/Alertmanager features by default.

Current priority is one planned AWS validation window. The sequence should be: apply once, configure DB/NFS/app/Nginx, run enhanced workflow validation, run upload failure incident validation, run latency scenario validation, run DB web-impact incident validation, create backup artifacts, deploy restore-lab, restore DB/file artifacts, run enhanced restore-lab validation, collect evidence, document results, and destroy once.
```
