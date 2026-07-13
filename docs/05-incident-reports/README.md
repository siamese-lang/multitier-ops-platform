# Incident reports

This directory converts runtime validation evidence into interview-explainable operations stories.

The fixed project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

These reports are not meant to claim production operations experience. They explain what was validated in a controlled EC2 lab and how the operator used WEB/WAS/DB/Storage/Backup evidence to narrow symptoms, verify recovery, and document claim boundaries.

## Report format

Each report follows the same structure:

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

## Current reports

| Report | Main operating point |
|---|---|
| `enhanced-service-workflow-baseline-report.md` | Establish the enhanced web work-order/evidence-file service as the operating workload before incident scenarios. |
| `upload-limit-incident-report.md` | Distinguish WEB upload limit, WAS multipart handling, DB metadata, and NFS file object behavior. |
| `latency-diagnosis-incident-report.md` | Separate WAS-side slow request from DB-backed slow path. |
| `db-web-impact-incident-report.md` | Explain how DB service failure affects readiness, web pages, and DB-backed APIs while process health can stay up. |
| `restore-lab-recovery-incident-report.md` | Explain why backup artifact creation is not recovery proof and how restore-lab verified DB/file/API consistency. |

## Evidence boundary

Supported:

```text
- The reports summarize controlled lab validation results.
- The evidence comes from repository validation documents, Ansible playbooks, HTTP/API responses, DB rows, file object checks, checksums, logs, and metrics.
- The reports are intended for infrastructure / WEB-WAS operations interview explanation.
```

Not supported:

```text
- production operations experience
- production disaster recovery
- automated failover
- RPO/RTO guarantee
- PostgreSQL HA
- SLO/SLA compliance
- commercial ITSM implementation
```

## Source-of-truth documents

Read these before editing incident reports:

```text
docs/00-project/current-state-after-enhanced-runtime-validation.md
docs/04-evidence/evidence-index.md
docs/04-evidence/restore-lab-recovery-validation-2026-07-13.md
apps/ops-sample-service/README.md
apps/ops-sample-service/FAILURE_LAB.md
```
