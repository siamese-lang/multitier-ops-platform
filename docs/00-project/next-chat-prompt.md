# Next chat prompt

Copy the prompt below into a new ChatGPT conversation when continuing the project.

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
- docs/00-project/final-handoff-before-runtime.md
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

Short version:

VM 기반 WEB/WAS/DB 운영환경을 직접 구성하고, 장애·성능·복구 문제를 로그와 지표로 분석하는 운영 포트폴리오

This is not an OpenKoda installation project, not a Terraform showcase, not a Spring Boot sample app project, not Kubernetes/EKS/GitOps work, not a Grafana dashboard-first project, and not a Prometheus-only monitoring practice. It is a VM-based operations portfolio focused on WEB/WAS/DB/Storage/Backup/Observability tier separation, failure diagnosis, and recovery validation.

The operated service identity is:

ops-sample-service = 운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스

The service includes work-order list/detail/create/status-change pages, status history, operation audit logs, evidence upload/download, PostgreSQL metadata plus file storage consistency, and WEB/WAS failure-lab endpoints: sleep, db-sleep, file-storage-check, and upload-limits.

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

- Phase 5A work-order domain/schema implemented.
- Phase 5B server-rendered work-order web workflow implemented.
- Phase 5C evidence upload/download workflow implemented.
- Phase 5D WEB/WAS failure-lab endpoints implemented.
- Phase 5E enhanced service workflow validation playbook statically prepared.
- Phase 5F enhanced service operations scenario matrix defined.
- Phase 5G upload failure and upload-limit incident validation playbook statically prepared.
- Phase 5H WAS sleep vs DB sleep latency scenario validation playbook statically prepared.
- Phase 5I DB service incident web-impact validation playbook statically prepared.
- Phase 5J enhanced restore-lab service validation playbook statically prepared.

Prepared validation files:

- infra/ansible/playbooks/lab-full-ops-enhanced-service-workflow-validation.yml
- infra/ansible/playbooks/lab-full-ops-enhanced-upload-limit-incident.yml
- infra/ansible/playbooks/lab-full-ops-enhanced-latency-scenario.yml
- infra/ansible/playbooks/lab-full-ops-enhanced-db-web-impact-incident.yml
- infra/ansible/playbooks/restore-lab-enhanced-service-validation.yml

Prepared scenarios:

- S1 Normal workflow baseline
- S2 Evidence upload failure isolation
- S3 WAS long request vs DB-backed delay
- S4 DB service incident with web impact
- S5 Enhanced backup and restore refresh

Important boundary:

The enhanced service implementation and validation playbooks are prepared, but enhanced AWS runtime evidence has not yet been collected.

Do not claim yet:

- enhanced web workflow runtime validation through AWS/Nginx/WAS
- evidence upload/download runtime validation through Nginx/WAS/NFS/PostgreSQL
- upload failure isolation across Nginx/WAS/NFS/PostgreSQL
- WAS sleep vs DB-backed delay validation with logs and PostgreSQL activity evidence
- DB service incident validation against the enhanced web workflow
- enhanced restore-lab recovery validation against the new service model

Do not continue with:

- app feature expansion unless runtime validation exposes a concrete defect
- Grafana dashboard-first work
- Alertmanager notification maturity
- Loki expansion
- blackbox exporter expansion
- additional Prometheus feature work
- PostgreSQL HA/failover
- Kubernetes/EKS/GitOps
- new AWS runtime windows by default

Execution split:

- Git Bash = Terraform only
- WSL = Ansible, Git work, evidence organization
- AWS = apply once -> validation -> evidence collect -> destroy once

Do not run Terraform from WSL. Do not run Ansible from Git Bash.

Current priority:

[VALIDATION] Run one planned enhanced-service AWS validation window.

Recommended runtime order:

1. Pull latest main locally.
2. Build or obtain the latest ops-sample-service artifact.
3. Terraform apply once from Git Bash.
4. Populate Ansible inventories from Terraform outputs.
5. Configure DB, NFS, app, Nginx, backup, and monitoring as needed.
6. Run enhanced service workflow validation.
7. Run upload failure / upload-limit incident validation.
8. Run WAS sleep vs DB sleep latency scenario validation.
9. Run DB web-impact incident validation.
10. Create enhanced backup artifacts.
11. Deploy restore-lab.
12. Restore DB/file artifacts into restore-lab.
13. Run enhanced restore-lab service validation using sample values from the runtime evidence window.
14. Preserve raw evidence locally.
15. Add repository evidence documents summarizing actual results.
16. Update evidence index, roadmap, portfolio summary, interview notes, and submission notes only after evidence exists.
17. Terraform destroy once.

Be careful: do not say the runtime validation has succeeded until the AWS validation window has actually run and the evidence documents have been added. If validation exposes a defect, fix only that concrete defect and rerun the minimum necessary validation.
```
