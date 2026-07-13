# Portfolio review guide

This guide explains how to review the repository as an infrastructure / WEB-WAS operations portfolio.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The project is not finished just because one validation window completed. The current goal is to make the existing implementation and runtime evidence explainable in an IT infrastructure engineering interview.

## Review order

Use this order when reviewing the portfolio from the top:

```text
1. README.md
   - project identity, operated service, topology, completed validation scope
2. docs/00-project/current-state-after-enhanced-runtime-validation.md
   - current validation state after S1-S4 and restore-lab validation
3. docs/00-project/portfolio-summary.md
   - recruiter/interviewer-facing project summary
4. docs/04-evidence/evidence-index.md
   - claim-to-evidence map
5. docs/05-incident-reports/README.md
   - incident-report index and evidence boundary
6. docs/05-incident-reports/*.md
   - operations narratives for each scenario
7. docs/00-project/interview-incident-qna.md
   - scenario-specific interview Q&A
8. docs/00-project/interview-explanation-notes.md
   - 30-second/2-minute explanation and broader interview Q&A
9. apps/ops-sample-service/README.md
   - operated service behavior and endpoint details
10. apps/ops-sample-service/FAILURE_LAB.md
    - failure-lab endpoint behavior
11. docs/00-project/deployment-rollback-scenario-plan.md
    - optional next runtime scenario design; not evidence yet
```

## What each layer is for

| Layer | Purpose |
|---|---|
| README | First-screen explanation of the project direction and completed evidence. |
| Current-state document | Prevents future conversations or edits from reverting the project to a pre-validation state. |
| Portfolio summary | Converts technical work into recruiter/interviewer-readable claims. |
| Evidence index | Maps each claim to supporting evidence and keeps unsupported claims explicit. |
| Incident reports | Converts raw runtime validation into operations stories: symptom, impact, checks, evidence, judgment, recovery. |
| Interview notes | Provides concise spoken explanations and likely Q&A. |
| Service docs | Explain what the operated workload actually does. |
| Deployment rollback plan | Defines the next optional runtime scenario without claiming it as completed evidence. |

## Incident reports to read first

```text
docs/05-incident-reports/enhanced-service-workflow-baseline-report.md
docs/05-incident-reports/upload-limit-incident-report.md
docs/05-incident-reports/latency-diagnosis-incident-report.md
docs/05-incident-reports/db-web-impact-incident-report.md
docs/05-incident-reports/restore-lab-recovery-incident-report.md
```

The strongest interview sequence is:

```text
1. Start with the enhanced service workflow baseline.
2. Explain upload-limit diagnosis as a WEB/WAS/DB/NFS separation problem.
3. Explain latency diagnosis as WAS-side delay vs DB-backed delay.
4. Explain DB web-impact as health vs readiness and dependency failure.
5. Explain restore-lab as the difference between backup artifact creation and actual recovery proof.
```

## Supported portfolio claims

The current repository can support these claims:

```text
- EC2 instances were separated into WEB/WAS/DB/Storage/Backup/Observability tiers.
- The operated workload is a lightweight work-order and evidence-file web service.
- WEB/WAS/DB normal paths and selected failure paths were validated with runtime evidence.
- DB metadata and NFS file objects were checked together using size and SHA-256 evidence.
- Enhanced work-order/evidence-file workflows were validated as a first enhanced runtime pass.
- Upload-limit, latency, and DB web-impact scenarios were validated in a controlled lab.
- Backup artifact creation was separated from recovery proof.
- Restore-lab validation proved restored DB/file/API consistency in an isolated runtime.
```

## Claims not to make

Do not claim:

```text
- production operations experience
- production disaster recovery
- automated failover
- RPO/RTO guarantee
- PostgreSQL HA
- SLO/SLA compliance
- commercial ITSM implementation
- Kubernetes/EKS/GitOps operation
- AWS managed architecture operation
- production monitoring maturity
```

## Interview framing

Use this framing:

```text
실제 상용 운영 경험이라고 주장하지는 않습니다. 대신 EC2를 VM 환경처럼 사용해 WEB/WAS/DB/Storage/Backup 계층을 직접 나누고, 작업 요청·증빙 파일 서비스를 올린 뒤 장애와 복구 상황을 evidence로 검증했습니다. 면접에서는 기능 개발보다 운영자가 어떤 계층을 확인했고 어떤 근거로 원인을 좁혔는지를 중심으로 설명합니다.
```

## Next hardening work

The next work should improve portfolio explanation quality rather than add unrelated platform features.

Current sequence:

```text
1. README.md links to the portfolio review guide and incident documents.
2. portfolio-summary.md links to the incident report layer and interview incident Q&A.
3. deployment-rollback-scenario-plan.md defines the next optional runtime scenario without claiming evidence.
4. Next documentation work should refresh interview-explanation-notes.md to remove old pre-validation wording and reference S1-S4 incident reports as completed evidence.
5. A deployment/rollback runtime window should be opened only if it is still justified after the interview notes are updated.
```
