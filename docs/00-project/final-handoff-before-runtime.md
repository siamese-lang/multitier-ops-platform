# Final handoff before enhanced-service runtime

Use this document when continuing the project after the enhanced-service implementation and validation-prep work.

## Fixed project identity

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short version:

```text
VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오
```

The operated service is:

```text
ops-sample-service = 운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

Do not reinterpret this as:

```text
OpenKoda installation
Terraform practice
AWS managed architecture showcase
Spring Boot sample app project
Kubernetes/EKS/GitOps project
Grafana dashboard-first project
Prometheus-only monitoring practice
commercial ITSM product clone
production operations experience
```

Terraform, Ansible, Spring Boot, Nginx, PostgreSQL, NFS, restic, node_exporter, and Prometheus are supporting tools. The portfolio theme is EC2 VM-based multi-tier operations, WEB/WAS failure diagnosis, and recovery validation.

## Repository state

Repository:

```text
siamese-lang/multitier-ops-platform
```

Latest completed preparation PRs:

```text
#140 [DESIGN] Define ops-sample-service completion scope
#141 [APP] Add work order domain and schema
#142 [APP] Add basic web workflow pages
#143 [APP] Add user evidence upload and download workflow
#144 [APP] Add WEB/WAS failure lab endpoints
#145 [DOCS] Reframe service completion status
#146 [ANSIBLE] Add enhanced service workflow validation
#147 [DESIGN] Define enhanced service operations scenarios
#148 [ANSIBLE] Add upload failure scenario validation
#149 [ANSIBLE] Add latency scenario validation
#150 [ANSIBLE] Add DB web-impact incident validation
#151 [ANSIBLE] Prepare enhanced restore-lab validation
```

The repository is prepared for one planned enhanced-service AWS validation window. Do not add more app features or observability features unless runtime validation exposes a concrete defect.

## Completed runtime evidence before enhanced-service refresh

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

Supported runtime claims from existing evidence:

```text
EC2-based WEB/WAS/DB/Storage/Backup/Observability tiers were separated and configured.
WEB/WAS/DB normal and failure paths were validated with evidence.
DB metadata and NFS file object consistency were validated with size and SHA-256 evidence.
Backup artifacts were created and then restored in a separate restore-lab environment for the earlier workload model.
Logs, service state, request-path responses, and metrics were used to narrow DB service incidents.
Prometheus metrics distinguished DB host reachability from PostgreSQL service failure.
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Completed service implementation baseline

`ops-sample-service` now includes:

```text
work-order list/detail/create/status-change pages
status history and operation audit logs
evidence upload/download workflow
PostgreSQL metadata + file storage object consistency path
failure-lab: sleep, db-sleep, file-storage-check, upload-limits
```

This supports the following implementation claim only:

```text
ops-sample-service includes a lightweight web workflow for operations work orders and evidence files.
```

It does not yet support the claim that the enhanced workflow has been runtime-validated through AWS/Nginx/WAS/NFS/PostgreSQL.

## Static validation prep completed

The following validation-prep files are present:

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

Prepared scenarios:

```text
S1. Normal workflow baseline
S2. Evidence upload failure isolation
S3. WAS long request vs DB-backed delay
S4. DB service incident with web impact
S5. Enhanced backup and restore refresh
```

## Claims not yet supported

Do not claim these until one planned runtime window succeeds and evidence documents are added:

```text
enhanced web workflow runtime validation through AWS/Nginx/WAS
evidence upload/download runtime validation through Nginx/WAS/NFS/PostgreSQL
upload failure isolation across Nginx/WAS/NFS/PostgreSQL
WAS sleep vs DB-backed delay validation with logs and PostgreSQL activity evidence
DB service incident validation against the enhanced web workflow
enhanced restore-lab recovery validation against the new service model
```

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

```text
No new AWS runtime by default.
No more observability feature expansion.
No more Prometheus/Grafana/Alertmanager expansion.
Use exactly one planned enhanced-service validation window.
```

Execution split:

```text
Git Bash = Terraform only
WSL      = Ansible, Git work, evidence organization
AWS      = apply once -> validation -> evidence collect -> destroy once
```

Do not run Terraform from WSL. Do not run Ansible from Git Bash.

## Next runtime sequence

Use this order unless a concrete defect appears:

```text
1. Pull latest main locally.
2. Build or obtain the latest ops-sample-service artifact.
3. Terraform apply once from Git Bash.
4. Populate Ansible inventories from Terraform outputs.
5. Configure DB, NFS, app, Nginx, backup, monitoring as needed.
6. Run enhanced service workflow validation.
7. Run upload failure / upload-limit incident validation.
8. Run WAS sleep vs DB sleep latency scenario validation.
9. Run DB web-impact incident validation.
10. Create enhanced backup artifacts.
11. Deploy restore-lab.
12. Restore DB/file artifacts into restore-lab.
13. Run enhanced restore-lab service validation using sample values from the runtime evidence window.
14. Preserve raw evidence locally.
15. Add repository evidence documents summarizing results.
16. Update evidence index, roadmap, portfolio summary, interview notes, and submission notes only after evidence exists.
17. Terraform destroy once.
```

## Expected evidence documents after runtime

Do not create these as completed evidence until the runtime window actually succeeds:

```text
docs/04-evidence/enhanced-service-workflow-validation-<date>.md
docs/04-evidence/enhanced-upload-failure-validation-<date>.md
docs/04-evidence/enhanced-latency-scenario-validation-<date>.md
docs/04-evidence/enhanced-db-web-impact-validation-<date>.md
docs/04-evidence/enhanced-restore-lab-validation-<date>.md
```

## Safe next task

```text
[VALIDATION] Run one planned enhanced-service AWS validation window
```

If runtime validation exposes a defect, fix only the concrete defect and repeat the minimum necessary validation. Do not expand the project scope.
