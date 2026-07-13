# Deployment rollback scenario plan

## Purpose

This document defines a possible next high-value validation scenario for the project.

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The project is not complete only because enhanced runtime validation succeeded. However, new AWS runtime windows should not be opened casually. This scenario should be executed only if it materially improves the portfolio for infrastructure / WEB-WAS operations interviews.

## Why this scenario is valuable

The current project is already strong in these areas:

```text
WEB/WAS/DB normal path
upload/download validation
upload-limit diagnosis
latency diagnosis
DB web-impact diagnosis
backup artifact creation
restore-lab DB/file/API recovery validation
logs, service state, request path, metrics, DB row, file checksum evidence
```

A remaining high-value operations topic is deployment and rollback.

Infrastructure / WEB-WAS operations interviews often ask:

```text
- 배포 후 무엇을 확인하나요?
- 배포 중 장애가 나면 어떻게 원인을 좁히나요?
- 롤백 기준은 무엇인가요?
- systemd 서비스 재시작 중 사용자 영향은 어떻게 확인하나요?
- health와 readiness를 어떻게 구분하나요?
```

This scenario answers those questions without changing the project into CI/CD, Kubernetes, blue/green, or managed architecture work.

## Scenario boundary

This scenario is allowed:

```text
VM/systemd based app jar deployment and rollback validation
```

This scenario must not become:

```text
- Kubernetes/EKS/GitOps work
- ALB/RDS redesign
- blue/green overengineering
- GitHub Actions deployment automation showcase
- zero-downtime production deployment claim
- production release management claim
```

## Target scenario

```text
app jar deployment
-> systemd restart
-> health/readiness/workflow validation
-> induced bad deployment or failed restart
-> rollback to previous jar
-> HTTP/API/DB/file consistency validation
```

## Proposed topology

Use the existing lab-full-ops style runtime:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
```

Optional extension if already available:

```text
nginx-01 -> app-01:8080
nginx-01 -> app-02:8080
```

Do not add new infrastructure only for this scenario unless a concrete validation gap requires it.

## Validation phases

### Phase A. Pre-deployment baseline

Goal:

```text
Confirm the current app version is serving the enhanced work-order/evidence-file workflow normally.
```

Checks:

```text
systemctl status ops-sample-service
curl /healthz
curl /readyz
GET /work-orders
POST or form-submit work-order create path
status transition path
evidence upload/download path
DB metadata row check
NFS file object check
optional consistency API check
```

Evidence:

```text
HTTP status
request ID
Nginx access log sample
app journald log sample
DB row count or sample row
NFS file path, size, SHA-256
```

### Phase B. Normal deployment

Goal:

```text
Deploy a new jar version and verify that the service returns to normal operating state after systemd restart.
```

Checks:

```text
copy new jar to release path
update current symlink or service jar path
systemctl daemon-reload if needed
systemctl restart ops-sample-service
systemctl status ops-sample-service
journalctl -u ops-sample-service
/healthz
/readyz
/work-orders
work-order create/status-change/upload/download
```

Evidence:

```text
previous jar path
new jar path
systemd restart result
health/readiness result
Nginx upstream result
DB/file consistency result
```

### Phase C. Bad deployment or failed restart

Goal:

```text
Show how a WEB/WAS operator detects that a deployment failed and avoids confusing it with DB/NFS failure.
```

Safe failure examples:

```text
- point systemd service to a missing jar path
- point service to an intentionally invalid jar copy
- set a bad environment variable that prevents startup
```

Avoid destructive failures:

```text
- do not corrupt DB data
- do not delete NFS evidence files
- do not break security groups or Terraform state
- do not induce broad AWS infrastructure failure
```

Checks:

```text
systemctl status ops-sample-service
journalctl -u ops-sample-service
curl /healthz
curl /readyz
Nginx access/error log
Nginx upstream failure status
DB service status remains normal
NFS mount remains normal
```

Expected judgment:

```text
The failure is in the WAS deployment/service startup layer, not DB, NFS, or Nginx configuration.
```

### Phase D. Rollback

Goal:

```text
Restore the previous jar and prove that the work-order/evidence-file workflow works again.
```

Checks:

```text
restore previous jar symlink or service path
systemctl restart ops-sample-service
systemctl status ops-sample-service
/healthz
/readyz
/work-orders
work-order create/status-change/upload/download
DB metadata and NFS file object consistency
Nginx and app log sample after rollback
```

Recovery proof:

```text
The rollback is valid only if HTTP/API workflow, DB metadata, and NFS file object checks succeed after restoring the previous service artifact.
```

## Claim boundary

Supported if executed:

```text
A controlled VM/systemd deployment failure and rollback scenario was validated in the lab, and recovery was verified through health/readiness, HTTP workflow, logs, DB metadata, and NFS file object checks.
```

Not supported:

```text
- production deployment experience
- zero-downtime deployment guarantee
- blue/green deployment
- canary deployment
- enterprise release management
- automated CI/CD maturity
```

## Evidence document to create after execution

If the runtime scenario is executed, create:

```text
docs/04-evidence/deployment-rollback-validation-YYYY-MM-DD.md
docs/05-incident-reports/deployment-rollback-incident-report.md
```

## Interview explanation target

Use this answer after validation:

```text
배포·롤백 시나리오는 Kubernetes나 대형 CI/CD가 아니라 VM 기반 WEB/WAS 운영 관점으로 검증했습니다. 새 jar 배포 후 systemd restart, health/readiness, Nginx upstream 응답, 업무 화면, DB metadata, NFS file object를 확인했습니다. 이후 의도적으로 잘못된 service artifact를 지정해 WAS 기동 실패를 만들고, DB/NFS가 아니라 WAS 배포 계층 문제임을 systemctl, journalctl, Nginx log로 분리했습니다. 마지막으로 이전 jar로 rollback한 뒤 업무 흐름과 DB/file consistency가 정상인지 확인했습니다.
```

## Execution decision

Do not execute this scenario immediately.

Execute only when:

```text
1. Existing documentation hardening is complete.
2. There is enough AWS budget for one controlled runtime window.
3. The exact evidence checklist is finalized.
4. The environment can be destroyed immediately after evidence collection.
```

Until then, this remains a plan, not runtime evidence.
