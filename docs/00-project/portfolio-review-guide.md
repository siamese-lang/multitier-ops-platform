# Portfolio review guide

This guide explains how to review the repository as an infrastructure / WEB-WAS operations portfolio.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The current repository should be reviewed as a completed v1.0 operations portfolio. The AWS runtime validation window was completed on 2026-07-13, and the lab resources were destroyed after evidence collection. Future work should not reopen AWS runtime unless a specific new claim must be validated.

## Review order

Use this order when reviewing the portfolio from the top:

```text
1. README.md
   - project identity, operated service, topology, completed validation scope
2. docs/00-project/portfolio-summary.md
   - recruiter/interviewer-facing project summary
3. docs/04-evidence/evidence-index.md
   - representative interview scenario map and claim-to-evidence map
4. docs/04-evidence/final-runtime-validation-2026-07-13.md
   - final runtime validation and cleanup summary
5. docs/04-evidence/connection-pressure-validation-2026-07-13.md
   - Tomcat request-thread pressure vs HikariCP connection-pool pressure evidence
6. docs/05-incident-reports/README.md
   - incident-report index and evidence boundary
7. docs/05-incident-reports/*.md
   - operations narratives for baseline, upload, latency, DB impact, and recovery scenarios
8. docs/00-project/interview-incident-qna.md
   - scenario-specific interview Q&A for the representative scenarios
9. docs/00-project/interview-explanation-notes.md
   - 30-second/2-minute explanation and broader interview Q&A
10. apps/ops-sample-service/README.md
    - operated service behavior and endpoint details
11. apps/ops-sample-service/FAILURE_LAB.md
    - failure-lab endpoint behavior
12. docs/00-project/submission-description-notes.md
    - application-form and portfolio URL wording
```

## What each layer is for

| Layer | Purpose |
|---|---|
| README | First-screen explanation of the project direction, operated service, completed validation, and boundary. |
| Portfolio summary | Converts technical work into recruiter/interviewer-readable claims. |
| Evidence index | Maps representative scenarios and portfolio claims to supporting evidence while keeping boundaries explicit. |
| Final runtime summary | Preserves the final validation window, completed scenarios, cleanup state, and unsupported claims. |
| Connection pressure evidence | Provides the strongest WAS/DB runtime distinction evidence for Tomcat thread pressure vs HikariCP pool pressure. |
| Incident reports | Convert raw runtime validation into operations stories: symptom, impact, checks, evidence, judgment, recovery. |
| Interview Q&A | Provides concise spoken explanations for the representative scenarios. It is an explanation layer, not primary evidence. |
| Service docs | Explain what the operated workload actually does. |
| Submission notes | Provide safe wording for application forms and portfolio URL descriptions. |

## Representative interview scenario sequence

Use this five-scenario sequence when explaining the project in an interview:

```text
1. Nginx bad config detection and rollback
   - WEB-tier config validation before reload
   - known-good config restore and proxied service verification

2. Bad WAS artifact deployment and rollback
   - VM/systemd WAS failure detection
   - previous jar/env restore and health/version/readiness/summary verification

3. Tomcat request-thread pressure vs HikariCP pool pressure
   - delayed but successful DB-backed API under request-thread pressure
   - failed DB-backed API under WAS-side DB connection-pool exhaustion while PostgreSQL remained active

4. App-side NFS mount failure and recovery
   - DB-backed work-order path remained available
   - file-storage-dependent evidence-file path failed and recovered after remount
   - DB metadata, NFS object, size, and SHA-256 consistency were checked

5. Backup artifact vs restore-lab recovery proof
   - pg_dump/restic artifact creation was separated from recovery proof
   - restore-lab validated DB/file/API consistency after restore
```

The strongest framing is:

```text
장애를 많이 재현했다는 점보다, 장애별로 먼저 확인해야 할 계층과 근거를 나누어 판단했다는 점을 강조합니다.
```

## Supported portfolio claims

The current repository can support these claims:

```text
- EC2 instances were separated into WEB/WAS/DB/Storage/Backup/Observability tiers.
- The operated workload is a lightweight work-order and evidence-file web service.
- WEB/WAS/DB normal paths and selected failure paths were validated with runtime evidence.
- DB metadata and NFS file objects were checked together using size and SHA-256 evidence.
- Backup artifact creation was separated from restore-lab recovery proof.
- Restore-lab validation proved restored DB/file/API consistency in an isolated runtime.
- Tomcat request-thread pressure and HikariCP DB connection-pool pressure were distinguished with bounded lab evidence.
- A bad WAS artifact deployment was detected and rolled back through jar/env restore and post-rollback checks.
- App-side NFS mount loss was diagnosed separately from DB-backed business-path availability.
- An invalid Nginx config candidate was rejected with nginx -t before unsafe reload, then restored and reloaded successfully.
- Logs, service state, HTTP status/timing, pg_stat_activity, DB rows, file objects, and checksums were used as evidence.
- AWS lab resources were destroyed after evidence collection.
```

## Claims not to make

Do not claim:

```text
- production operations experience
- production incident response
- production disaster recovery
- automated failover
- RPO/RTO guarantee
- PostgreSQL HA
- storage HA
- SLO/SLA compliance
- commercial ITSM implementation
- Kubernetes/EKS/GitOps operation
- AWS managed architecture operation
- production monitoring maturity
- production load testing
- capacity sizing
- external Tomcat/WAR operation
- blue-green/canary deployment
- zero-downtime release guarantee
```

## Interview framing

Use this framing:

```text
실제 상용 운영 경험이라고 주장하지는 않습니다. 대신 EC2를 VM 환경처럼 사용해 WEB/WAS/DB/Storage/Backup/Observability 계층을 직접 나누고, 작업 요청·증빙 파일 서비스를 올린 뒤 장애와 복구 상황을 evidence로 검증했습니다. 면접에서는 기능 개발보다 운영자가 어떤 계층을 확인했고 어떤 근거로 원인을 좁혔는지를 중심으로 설명합니다.
```

## Current hardening direction

The project is close to v1.0 portfolio completion. New service features, new open-source tools, or new AWS runtime windows should not be added by default.

Current sequence:

```text
1. Keep README, portfolio-summary, evidence-index, and interview Q&A aligned around the representative five scenarios.
2. Use evidence-index as the claim-to-evidence source of truth.
3. Use interview-incident-qna.md as the spoken explanation layer.
4. Use submission-description-notes.md for application-form wording.
5. Open a new AWS runtime only if a materially new claim needs evidence.
```
