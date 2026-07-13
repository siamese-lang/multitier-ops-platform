# Final runtime validation summary - 2026-07-13

## Purpose

This document summarizes the final AWS EC2 runtime validation window for the project:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is to preserve the final claim boundary after the runtime lab was validated and destroyed. Raw reports and logs are intentionally kept outside the repository in the local evidence archive.

## Runtime status

```text
lab-full-ops AWS runtime validation: completed
connection pressure validation: completed
bad deployment rollback validation: completed
NFS mount failure/recovery validation: completed
Nginx config rollback validation: completed
AWS lab cleanup/destroy: completed
```

The runtime environment is no longer intentionally retained. Future work should default to static checks, documentation hardening, and interview explanation refinement unless a new explicit runtime validation window is planned.

## Local raw evidence archive

Raw report files are not committed to the repository.

Expected local archive categories:

```text
multitier-ops-platform-evidence/connection-pressure/
multitier-ops-platform-evidence/bad-deployment-rollback/
multitier-ops-platform-evidence/nfs-mount-failure/
multitier-ops-platform-evidence/nginx-config-rollback/
multitier-ops-platform-evidence/final-state/
```

## Scenario 1: Connection pressure validation

Supported claim:

```text
Nginx -> Spring Boot embedded Tomcat -> HikariCP -> PostgreSQL 경로에서 WAS request-thread pressure와 DB connection-pool pressure를 구분했다.
```

Validated behavior:

```text
WAS request-thread pressure caused delayed but successful DB-backed summary behavior.
HikariCP pool pressure caused DB-backed request failure while PostgreSQL remained active.
Nginx access log, app journald, HikariCP state, PostgreSQL pg_stat_activity, and HTTP timing/status were correlated.
```

Boundary:

```text
This is bounded lab evidence.
It is not production load testing, capacity sizing, SLO/SLA validation, autoscaling evidence, or external Tomcat/WAR operation.
```

## Scenario 2: Bad deployment rollback validation

Supported claim:

```text
A bad WAS artifact deployment was detected through systemd/health behavior, then the previous jar and environment file were restored and validated through health, version, readiness, and DB-backed summary checks.
```

Validated behavior:

```text
baseline service state was active
bad artifact deployment produced observable service/health failure
rollback was attempted
rollback health/version/readiness/summary checks succeeded
final service state returned to active
rollback status was validated
```

Boundary:

```text
This is VM/systemd deployment rollback evidence.
It is not production release management, blue/green, canary, zero-downtime deployment, or production incident response experience.
```

## Scenario 3: NFS mount failure and recovery validation

Supported claim:

```text
An app-side NFS mount loss affected evidence-file creation while DB-backed work-order creation remained available, and remount restored file evidence consistency.
```

Validated behavior:

```text
baseline storage readiness was true
baseline NFS mount was mounted
outage mount state became not_mounted
outage storage readiness became false
work-order creation returned 201 Created through the DB-backed path
evidence-file creation returned 503 storage-not-ready
remount restored the NFS path
recovery evidence creation became consistent
recovery consistency endpoint returned consistent
recovered file existed on NFS with matching size and SHA-256
```

Operational interpretation:

```text
The DB-backed work-order path and the file-storage-dependent evidence-file path can fail differently.
The correct first checks are mount state, file-storage readiness, Nginx request log, app journald, DB metadata, and NFS object state.
```

Boundary:

```text
This is bounded app-side NFS failure/recovery evidence.
It is not production storage HA, automatic failover, NFS performance tuning, or production chaos engineering.
```

## Scenario 4: Nginx bad config detection and rollback validation

Supported claim:

```text
An invalid Nginx config candidate was rejected by nginx -t before unsafe reload, the running WEB tier stayed active, and the original config was restored and reloaded with healthy proxied service checks.
```

Validated behavior:

```text
baseline Nginx service path was healthy
invalid config candidate was written to the site file
nginx -t rejected the bad candidate
running Nginx stayed active before unsafe reload
previous site config was restored
restored nginx -t succeeded
Nginx reload succeeded
post-rollback health/version/summary checks succeeded
rollback status was validated
```

Boundary:

```text
This is WEB-tier config validation and rollback evidence.
It is not production traffic safety guarantee, zero-downtime release guarantee, blue-green deployment, canary deployment, or enterprise change-management maturity.
```

## Final cleanup

The runtime validation window was closed after evidence collection.

Supported claim:

```text
AWS runtime resources used for the validation window were destroyed after evidence collection.
```

Boundary:

```text
Terraform destroy completion is cost-control and lab-lifecycle evidence.
It is not production decommissioning or production change-management evidence.
```

## Safe portfolio summary

Use this concise version in interviews or portfolio descriptions:

```text
EC2 VM 기반 WEB/WAS/DB/Storage/Backup 계층을 구성하고, 운영 대상 서비스의 정상 경로와 장애·복구 시나리오를 runtime evidence로 검증했습니다. Nginx 설정 오류, WAS artifact 배포 실패, Tomcat/HikariCP pressure, NFS mount 장애, 백업·복구를 각각 분리해 HTTP 상태, 로그, systemd 상태, DB row, pg_stat_activity, NFS 파일 checksum으로 원인을 확인했습니다. 검증 후 AWS lab 리소스는 삭제했습니다.
```

## Claims not supported

Do not claim:

```text
production operations experience
production load testing
capacity sizing
SLO/SLA validation
RPO/RTO guarantee
production DR
PostgreSQL HA
automatic failover
storage HA
blue-green deployment
canary deployment
zero-downtime release guarantee
external Tomcat/WAR operation
Kubernetes/EKS/GitOps operation
commercial ITSM implementation
```
